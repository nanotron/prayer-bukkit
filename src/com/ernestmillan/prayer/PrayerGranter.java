package com.ernestmillan.prayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

import org.bukkit.ChatColor;
import org.bukkit.CropState;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.CocoaPlant;
import org.bukkit.material.CocoaPlant.CocoaPlantSize;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Processes prayer request grant based on odds and XP levels.
 * @author Ernest Millan
 */
public class PrayerGranter {
	
	private Prayer plugin;
	Player player, recipient, grantee;
	private String prayer_request, available_options_txt, text_not_allowed;
	private World world;
	private CommandSender sender;
	private Boolean prayer_allowed, unlimited_creative_prayer, unlimited_op_prayer, unlimited_prayer, creative_mode, self_pray;
	Boolean is_flying, is_firehand, is_curse;
	private int prayer_limit;
	Timer cooldownGrantedTimer, flightTimer, firehandTimer;
	String cooldown_remaining_granted, cooldown_remaining_attempted;
	PrayerExecutor executorInstance;
	public static Map<Player, Boolean> cooldown_between_attempted_active = new HashMap<Player, Boolean>();
	public static Map<Player, Boolean> cooldown_between_granted_active = new HashMap<Player, Boolean>();
	
	public PrayerGranter(PrayerExecutor executorInstance, Prayer plugin, CommandSender sender, Boolean is_curse, String[] args, String available_options_txt, Player recipient) {
		this.plugin = plugin;
		this.sender = sender;
		this.is_curse = is_curse;
		this.prayer_request = args[0];
		this.player = (Player)sender;
		this.recipient = recipient;
		this.world = player.getWorld();
		this.available_options_txt = available_options_txt;
		this.is_flying = false;
		this.is_firehand = false;
		this.prayer_allowed = true;
		this.unlimited_creative_prayer = PrayerExecutor.isPlayerCreativeUnlimited(player);
		this.unlimited_op_prayer = PrayerExecutor.isPlayerOpUnlimited(player);
		this.unlimited_prayer = (unlimited_creative_prayer || unlimited_op_prayer) ? true : false;
		this.prayer_limit = PrayerExecutor.getPrayerLimit(player);
		this.executorInstance = executorInstance;
	}
	
	public void processPrayer() {
		if(recipient != null && recipient instanceof Player) {
			grantee = recipient;
			self_pray = false;
		} else {
			grantee = player;
			self_pray = true;
		}

		creative_mode = PrayerExecutor.isPlayerInCreative(grantee);
		
		Boolean is_valid_option = PrayerExecutor.isValidOption(prayer_request, is_curse);
		verifyCooldowns();
		
		if(prayer_allowed) {
			if (prayer_request.equalsIgnoreCase("help") || !is_valid_option) {
				sender.sendMessage(available_options_txt);
			} else {						
				int players_xp = player.getLevel();		
				int prayer_count = executorInstance.getCurrentCount(player);
				PrayerExecutor.pray_counter.put(player, prayer_count+1);	

				ChatColor prayer_color = ChatColor.GRAY;
				if(prayer_count >= prayer_limit-2) {
					prayer_color = ChatColor.RED;
				} 

				int prayers_remaining = prayer_limit - prayer_count;
				int prayer_range = getRange(players_xp);
				String prayer_count_plural = (prayers_remaining == 1) ? "" : "s";
				String prayer_count_display = (unlimited_prayer) ? "" : prayer_color+" ("+(prayers_remaining)+" prayer"+prayer_count_plural+" left).";
				Boolean no_punishment = (unlimited_prayer) ? true : false;
				Boolean prayer_granted = false;

				// Patch for instances where player has sudden drop in XP
				if(prayers_remaining < -1) {
					PrayerExecutor.pray_counter.put(player, players_xp);
				}

				if((prayers_remaining == -1) && !no_punishment) {
					// Punish player for excessive prayers.
					issuePunishment(null, PrayerConfig.excessive_prayer_damage_level);
				} else if (prayer_request.equalsIgnoreCase("op")) {
					// If operator
					if(!unlimited_op_prayer && !player.isOp()) {
						issuePunishment("generic", PrayerConfig.request_op_damage_level);
					} else {
						sender.sendMessage(ChatColor.RED+"Thou art already an operator.");
					}
				} else {
					// Regular granted prayer
					Random generator = new Random();
					int roll = generator.nextInt(prayer_range) + 1;	

					if(roll == 1) {
						// Flames effect when prayer is granted.
						Effect prayerEffect = Effect.MOBSPAWNER_FLAMES;
						player.playEffect(player.getLocation(), prayerEffect, null);

						// Process Prayer Request
						prayer_granted = true;
						grantPrayer();
					} else {
						// Deny Prayer Request - Warn or provide XP.
						denyPrayer();
					}
				}

				if(prayers_remaining != -1 && !prayer_granted) {
					String extra_alter_txt = (PrayerAltar.player_near_altar.get(player)) ? ","+ChatColor.LIGHT_PURPLE+" in front of an Altar"+ChatColor.RESET+"," : "";
					sender.sendMessage("Thou hast prayed"+extra_alter_txt+" for "+ChatColor.GOLD+prayer_request+"."+prayer_count_display);
				}
				
				if(PrayerConfig.enable_verbose_logging || PrayerConfig.enable_dev_logging) {
					logging(prayer_limit, prayer_range, prayers_remaining, prayer_granted, no_punishment);
				}
			}
		} else {
			player.sendMessage(text_not_allowed);
		}
	}
	
	private void logCooldown(String key, int cooldown_time) {
		// Record Granted Cooldown Timestamp
		ConfigAccessor users_config = new ConfigAccessor(plugin, "users/"+player.getName()+".yml");
		users_config.getConfig().set(key, System.currentTimeMillis() + cooldown_time);
		users_config.saveConfig();
	}
	
	private void setGrantedCooldown() {
		if (!unlimited_prayer) {
			if(PrayerConfig.enable_cooldown_between_granted_prayers && (PrayerConfig.cooldown_duration_of_granted_prayer > 0)) {
				PrayerGranter.cooldown_between_granted_active.put(player, true);
				
				// Record Granted Cooldown Timestamp
				logCooldown("cooldown_granted_expire", PrayerConfig.cooldown_duration_of_granted_prayer);
			}
		}
	}
	
	private void setAttemptedCooldown() {
		if(PrayerConfig.enable_cooldown_between_attempted_prayers && (PrayerConfig.cooldown_duration_of_attempted_prayer > 0) && !unlimited_creative_prayer && prayer_allowed) {
			PrayerGranter.cooldown_between_attempted_active.put(player, true);
			
			// Record Attempted Cooldown Timestamp
			logCooldown("cooldown_attempted_expire", PrayerConfig.cooldown_duration_of_attempted_prayer);
		}
	}
	
	private void verifyCooldowns() {
		if (!unlimited_prayer) {
			verifyLoggedCooldowns(plugin, player);
			
			if(PrayerGranter.cooldown_between_granted_active.get(player) == null || PrayerGranter.cooldown_between_granted_active.get(player) == false) {
				PrayerGranter.cooldown_between_granted_active.put(player, false);
			}
			if(PrayerGranter.cooldown_between_attempted_active.get(player) == null || PrayerGranter.cooldown_between_attempted_active.get(player) == false) {
				PrayerGranter.cooldown_between_attempted_active.put(player, false);
			}

			String patience_txt = ChatColor.RED+"Patience my child. Thou must wait ";
			if(PrayerGranter.cooldown_between_granted_active.get(player)) {
				prayer_allowed = false;
				text_not_allowed = patience_txt+PrayerUtil.genDurationText(PrayerConfig.cooldown_duration_of_granted_prayer)+" between prayer grants. "+cooldown_remaining_granted+" remaining.";
			}
			if(PrayerGranter.cooldown_between_attempted_active.get(player) && !PrayerGranter.cooldown_between_granted_active.get(player)) {
				prayer_allowed = false;
				text_not_allowed = patience_txt+PrayerUtil.genDurationText(PrayerConfig.cooldown_duration_of_attempted_prayer)+" between prayer attempts. "+cooldown_remaining_attempted+" remaining.";
			}

			// Start cool-down for attempted prayer.
			setAttemptedCooldown();
		}
	}
	
	private void verifyLoggedCooldowns(JavaPlugin plugin, Player player) {
		ConfigAccessor users_config = new ConfigAccessor(plugin, "users/"+player.getName()+".yml");
		
		if(users_config.getConfig().get("cooldown_granted_expire") != null) {
			long cooldown_granted = (Long) users_config.getConfig().get("cooldown_granted_expire");
			
			if(cooldown_granted > System.currentTimeMillis()) {
				cooldown_remaining_granted = PrayerUtil.getDurationTextComplete((int) (cooldown_granted - System.currentTimeMillis()));
				PrayerGranter.cooldown_between_granted_active.put(player, true);				
			} else {
				PrayerGranter.cooldown_between_granted_active.put(player, false);
			}
		}
		
		if(users_config.getConfig().get("cooldown_attempted_expire") != null) {
			long cooldown_attempted = (Long) users_config.getConfig().get("cooldown_attempted_expire");

			if(cooldown_attempted > System.currentTimeMillis()) {
				cooldown_remaining_attempted = PrayerUtil.getDurationTextComplete((int) (cooldown_attempted - System.currentTimeMillis()));
				PrayerGranter.cooldown_between_attempted_active.put(player, true);
			} else {
				PrayerGranter.cooldown_between_attempted_active.put(player, false);
			}
		}
	}
	
	private int getRange(int characters_xp) {
		int range = 0;
		int max_odds = 35;
		int min_odds = 3;
		
		range = ((max_odds - characters_xp) >= min_odds) ? max_odds - characters_xp : min_odds;
		
		if(characters_xp >= 10000) {
			range = min_odds-1;
		}
		if(isNearVillager() && range >= min_odds) {
			range = (int) (range-Math.ceil(range/8));
		}
		if(PrayerAltar.player_near_altar.get(player) && PrayerConfig.allow_altar_influence) {
			int alter_activator = PrayerAltar.altar_activator_block_type.get(player);
			
			if(alter_activator == 1) {
				range = (int) Math.ceil(range / 1.5);
				if(range < min_odds) {
					range = min_odds;
				}
			} else if(alter_activator == 2) {
				range = (int) Math.ceil(range / 2);
				if(range < min_odds) {
					range = min_odds;
				}
			} else if(alter_activator == 3) {
				range = 1;
			}
		}
		if(isHeadHeld() && PrayerConfig.allow_skull_influence) {
			range = (int) (range-Math.ceil(range/5));
		}
		if(unlimited_prayer) {
			range = 1; 
		}

		return range;
	}
	
	private void grantPrayer() {
		Boolean prayer_granted = true;
		String prayed_for_txt = "";
		
		if(!self_pray) {
			prayed_for_txt = ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast blessed thee. ";
		}
		
		// Curse
		if(is_curse) {
			if (prayer_request.equalsIgnoreCase("fire")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thee with "+ChatColor.RED+"fire"+ChatColor.RESET+".");
				grantee.setFireTicks(400);
			} else if (prayer_request.equalsIgnoreCase("flood")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thee with a "+ChatColor.RED+"flood"+ChatColor.RESET+".");
				createFlood();
			} else if (prayer_request.equalsIgnoreCase("foes")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thee with "+ChatColor.RED+"foes"+ChatColor.RESET+".");
				sendEntityToPlayer(grantee, EntityType.CREEPER, 1, true);
				sendEntityToPlayer(grantee, EntityType.SKELETON, 1, true);
				sendEntityToPlayer(grantee, EntityType.ZOMBIE, 1, true);
			} else if (prayer_request.equalsIgnoreCase("health")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thy "+ChatColor.RED+"health"+ChatColor.RESET+".");
				grantee.setHealth(2);	
			} else if (prayer_request.equalsIgnoreCase("hunger")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thy "+ChatColor.RED+"hunger"+ChatColor.RESET+".");
				grantee.setFoodLevel(2);
			} else if (prayer_request.equalsIgnoreCase("smite")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed thee with "+ChatColor.RED+"lightning"+ChatColor.RESET+".");
				world.strikeLightning(grantee.getLocation());	
			} else if (prayer_request.equalsIgnoreCase("waters")) {
				grantee.sendMessage(ChatColor.YELLOW+player.getName()+ChatColor.RESET+" hast cursed the "+ChatColor.RED+"waters"+ChatColor.RESET+" near thee.");	
				curseTheWaters();
			} else {
				sender.sendMessage(available_options_txt);
				prayer_granted = false;
			}
		// Pray	
		} else {
			if (prayer_request.equalsIgnoreCase("air")) {
				String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_air_ms);			
				grantee.sendMessage(prayed_for_txt+"The breath of "+PrayerConfig.deity_name+" is yours for "+ChatColor.YELLOW+duration_txt+ChatColor.RESET+".");

				PotionEffect breathePotion = new PotionEffect(PotionEffectType.WATER_BREATHING, PrayerConfig.duration_air, 2);
				grantee.addPotionEffect(breathePotion, true);
			} else if(prayer_request.equalsIgnoreCase("ally")) {
				grantee.sendMessage(prayed_for_txt+"Thou hast received a guardian "+ChatColor.YELLOW+"Iron Golem.");
				sendEntityToPlayer(grantee, EntityType.IRON_GOLEM, 1, false);
			} else if (prayer_request.equalsIgnoreCase("clearsky")) {
				grantee.sendMessage(prayed_for_txt+"Let there be "+ChatColor.YELLOW+"clear sky!");
				world.setStorm(false);
			} else if (prayer_request.equalsIgnoreCase("crops")) {
				grantee.sendMessage(prayed_for_txt+"Thy "+ChatColor.YELLOW+"crops"+ChatColor.RESET+" have been blessed by "+PrayerConfig.deity_name+".");
				blessCrops();
			} else if (prayer_request.equalsIgnoreCase("day")) {
				grantee.sendMessage(prayed_for_txt+"Let there be "+ChatColor.YELLOW+"light!");
				world.setTime(0);	
			} else if (prayer_request.equalsIgnoreCase("firehand")) {
				if(is_firehand) {
					sender.sendMessage(ChatColor.RED+"Firehand is already active.");
				} else {
					String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_firehand_ms);
					grantee.sendMessage(prayed_for_txt+ChatColor.YELLOW+"Firehand"+ChatColor.RESET+" is yours for "+ChatColor.YELLOW+duration_txt+ChatColor.RESET+". You've got the touch, you've got the power!");
					is_firehand = true; 
					
					FirehandTask firehandTimer = new FirehandTask();
					firehandTimer.runTaskLater(plugin, PrayerConfig.convertMillisecondsToTicks(PrayerConfig.duration_firehand_ms));
				}
			} else if (prayer_request.equalsIgnoreCase("flight")) {
				if(is_flying) {
					sender.sendMessage(ChatColor.RED+"Flight is already active.");
				} else if(creative_mode) {
					sender.sendMessage("One may already fly in creative mode.");
				} else {
					String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_flight);
					grantee.sendMessage(prayed_for_txt+"The power of flight is yours for "+ChatColor.YELLOW+duration_txt+ChatColor.RESET+". Fly "+grantee.getDisplayName()+" fly!");
					grantee.setAllowFlight(true);
					is_flying = true; 
					
					FlightRollTaskBukkit flightTask = new FlightRollTaskBukkit();
					flightTask.runTaskLater(plugin, PrayerConfig.convertMillisecondsToTicks(PrayerConfig.duration_flight));
				}				
			} else if (prayer_request.equalsIgnoreCase("food")) {
				Random generator = new Random();
				int roll = generator.nextInt(3) + 1; // 1 of 3

				if(roll == 1) { // Sometimes give the player two animals.
					grantee.sendMessage(prayed_for_txt+"Be fruitful and multiply!");
					Random generator_animal = new Random();
					int roll_animal = generator_animal.nextInt(3) + 1; // 1 of 3

					if(roll_animal == 1) {
						sendEntityToPlayer(grantee, EntityType.COW, 2, true);
					} else if (roll_animal == 2) {
						sendEntityToPlayer(grantee, EntityType.PIG, 2, true);
					} else {
						sendEntityToPlayer(grantee, EntityType.CHICKEN, 2, true);
					}
				} else {
					// else just affect hunger level.
					grantee.sendMessage(prayed_for_txt+ChatColor.YELLOW+"Hunger"+ChatColor.RESET+" no more, my child.");
					grantee.setFoodLevel(20);
				}
			} else if (prayer_request.equalsIgnoreCase("giant")) {
				if(creative_mode) {
					grantee.sendMessage(prayed_for_txt+ChatColor.AQUA+"Fee-fi-fo-fum!!!");	
					sendEntityToPlayer(grantee, EntityType.GIANT, 1, false);
				} else {
					sender.sendMessage(ChatColor.RED+"Thou may only pray for a \"mighty\" Giant in Creative mode.");	
				}
			} else if (prayer_request.equalsIgnoreCase("health")) {
				grantee.sendMessage(prayed_for_txt+"Restoring thy "+ChatColor.YELLOW+"health"+ChatColor.RESET+". Live long and prosper.");
				grantee.setHealth(grantee.getMaxHealth());	
			} else if (prayer_request.equalsIgnoreCase("home")) {
				grantee.sendMessage(prayed_for_txt+"Home is where thou hangs thy pickaxe.");
				Location home_location = (grantee.getBedSpawnLocation() != null) ? grantee.getBedSpawnLocation() : world.getSpawnLocation();
				grantee.teleport(home_location);
			} else if (prayer_request.equalsIgnoreCase("invisibility")) {
				String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_invisibility_ms);
				grantee.sendMessage(prayed_for_txt+"Invisibility granted for "+ChatColor.YELLOW+duration_txt+".");
				PotionEffect invisibilityPotion = new PotionEffect(PotionEffectType.INVISIBILITY, PrayerConfig.duration_invisibility, 2);
				grantee.addPotionEffect(invisibilityPotion, true);		
			} else if (prayer_request.equalsIgnoreCase("jump")) {
				String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_jump_ms);
				grantee.sendMessage(prayed_for_txt+"Enhanced jump granted for "+ChatColor.YELLOW+duration_txt+ChatColor.RESET+". Jump "+grantee.getDisplayName()+" jump!");
				PotionEffect jumpPotion = new PotionEffect(PotionEffectType.JUMP, PrayerConfig.duration_jump, 2);
				grantee.addPotionEffect(jumpPotion, true);			
			} else if (prayer_request.equalsIgnoreCase("lightning")) {
				grantee.sendMessage(prayed_for_txt+"Let there be "+ChatColor.YELLOW+"lightning!");
				Block targetBlock = grantee.getTargetBlock(null, 60);
				world.strikeLightning(targetBlock.getLocation());	
			} else if (prayer_request.equalsIgnoreCase("minions")) {
				grantee.sendMessage(prayed_for_txt+"Behold thy mighty "+ChatColor.YELLOW+"minions!");		
				Random generator_pet = new Random();
				int roll_pet = generator_pet.nextInt(2) + 1; // 1 of 2

				EntityType petType = (roll_pet == 1) ? EntityType.WOLF : EntityType.OCELOT;
				sendEntityToPlayer(grantee, petType, PrayerConfig.option_minions_amount, true);
			} else if (prayer_request.equalsIgnoreCase("nightvision")) {
				String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_nightvision_ms);
				grantee.sendMessage(prayed_for_txt+"Night Vision granted for "+ChatColor.YELLOW+duration_txt+".");
				PotionEffect nightvisionPotion = new PotionEffect(PotionEffectType.NIGHT_VISION, PrayerConfig.duration_nightvision, 2);
				grantee.addPotionEffect(nightvisionPotion, true);	
			} else if (prayer_request.equalsIgnoreCase("shelter")) {
				grantee.sendMessage(prayed_for_txt+"Take refuge in the "+ChatColor.YELLOW+"shelter"+ChatColor.RESET+" of "+PrayerConfig.deity_name+".");	
				PrayerShelter myShelter = new PrayerShelter(grantee);
				myShelter.build();
			} else if (prayer_request.equalsIgnoreCase("speed")) {
				String duration_txt = PrayerUtil.genDurationText(PrayerConfig.duration_speed_ms);
				grantee.sendMessage(prayed_for_txt+ChatColor.YELLOW+"Speed"+ChatColor.RESET+" of "+PrayerConfig.deity_name+" granted for "+ChatColor.YELLOW+duration_txt+ChatColor.RESET+". Run "+grantee.getDisplayName()+" run!");
				PotionEffect speedPotion = new PotionEffect(PotionEffectType.SPEED, PrayerConfig.duration_speed, 2);
				grantee.addPotionEffect(speedPotion, true);
			} else if(prayer_request.equalsIgnoreCase("storm")) {
				Random generator_plague = new Random();
				int fish_roll = generator_plague.nextInt(12) + 1; // 1 of 12
				int fish_amount = (PrayerConfig.option_storm_fish_amount == -1) ? fish_roll : PrayerConfig.option_storm_fish_amount;

				sendMaterialToPlayer(grantee, Material.RAW_FISH, fish_amount, true);
				grantee.sendMessage(prayed_for_txt+"Let there be "+ChatColor.YELLOW+"storm!");

				world.setStorm(true);
				world.setWeatherDuration(PrayerConfig.duration_storm);
			} else if(prayer_request.equalsIgnoreCase("strength")) {
				sender.sendMessage(prayed_for_txt+"May the strength of "+PrayerConfig.deity_name+" be with you for "+ChatColor.YELLOW+PrayerUtil.genDurationText(PrayerConfig.duration_strength_ms)+".");
				PotionEffect potion = new PotionEffect(PotionEffectType.INCREASE_DAMAGE, PrayerConfig.duration_strength, 2);
				PotionEffect potion_b = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, PrayerConfig.duration_strength, 2);
				grantee.addPotionEffect(potion);
				grantee.addPotionEffect(potion_b);
			} else if(prayer_request.equalsIgnoreCase("theforce")) {
				sender.sendMessage(prayed_for_txt+"Use the Force, "+grantee.getDisplayName()+"!");
				useTheForce();
			} else {
				sender.sendMessage(available_options_txt);
				prayer_granted = false;
			}
		}
		
		if(prayer_granted) {
			PrayerExecutor.pray_counter.put(player, 0);
			
			if(!self_pray) {
				String granter_txt = is_curse ? "curse" : "blessing";
				sender.sendMessage("Thy "+granter_txt+" hath been issued to "+ChatColor.YELLOW+grantee.getName()+ChatColor.RESET+".");
				
				if(!is_curse) {
					rewardPiety(player);
				}
			}
			
			setGrantedCooldown();
		}
	}
	
	private Boolean isHeadHeld() {
		Boolean heldState = (player.getItemInHand().getType().toString().equalsIgnoreCase("SKULL_ITEM")) ? true : false;
		return heldState;
	}
	
	private Boolean isNearVillager() {
		Boolean nearVillager = false;
		List<Entity> nearby_entities = player.getNearbyEntities(30, 20, 30);
		String[] nearbyEntities = nearby_entities.toString().split(",");
		
		if(PrayerConfig.allow_villager_influence) {
			for(int x=0; x<nearbyEntities.length; x++) {
				String entity = nearbyEntities[x].replaceAll("\\s", "").replaceAll("\\W", "");
				if(entity.equalsIgnoreCase("CraftVillager")) {
					nearVillager = true;
					break;
				}
			}
		}
		return nearVillager;
	}
	
	private void denyPrayer() {
		Random generator = new Random();
		int roll = generator.nextInt(3) + 1; // 1 through 3
		String extra_txt = (!PrayerAltar.player_near_altar.get(player)) ? ", or pray near an Altar," : "";
		sender.sendMessage(ChatColor.GRAY+"Try again. "+ChatColor.GOLD+"Increase XP"+extra_txt+" to improve thy chances of grant.");
		
		if(roll == 1) {
			rewardPiety(player);
		}
	}
	
	private void rewardPiety(Player player) {
		player.giveExp(2);
		sendEntityToPlayer(player, EntityType.EXPERIENCE_ORB, 2, false);
	}
	
	private void issuePunishment(String option_type, int damage_level) {
		String angered_start = "Thou hast "+ChatColor.RED+"angered"+ChatColor.RESET+" "+PrayerConfig.deity_name;
		if(option_type == "generic") {
			sender.sendMessage(angered_start+" with your prayer request!");
		} else {
			String extra_txt = (!PrayerAltar.player_near_altar.get(player)) ? ", or pray near an Altar," : "";
			sender.sendMessage(angered_start+" with excessive prayers! "+ChatColor.GOLD+"Increase XP"+extra_txt+" to increase thy quota.");		
			PrayerExecutor.pray_counter.put(player, 0);
		}
		
		Random generator_punish = new Random();
		int roll_punish = generator_punish.nextInt(8) + 1; // 1 of 8
		
		if(roll_punish == 1) {
			sendEntityToPlayer(player, EntityType.CREEPER, 2, true);	
		} else {
			world.strikeLightning(player.getLocation());
			player.damage(damage_level);
		}
	}
	
	private void spawnPet(Player player, EntityType entity_type, Location targetLoc) {
		if(entity_type.name().equals("OCELOT")) {
			Random generator = new Random();
			int roll = generator.nextInt(3) + 1; // 1 of 3
			
			Ocelot pet = (Ocelot) world.spawnEntity(targetLoc, EntityType.OCELOT);
			if(roll == 1) {
				pet.setCatType(Type.BLACK_CAT);
			} else if(roll == 2) {
				pet.setCatType(Type.RED_CAT);
			} else {
				pet.setCatType(Type.SIAMESE_CAT);
			}
			pet.setOwner(player);
		} else {
			Tameable pet = (Tameable) world.spawnEntity(targetLoc, entity_type);
			pet.setOwner(player);
		}
	}
	
	private void sendEntityToPlayer(Player player, EntityType entity_type, int quantity, Boolean sloppy_send) {		
		Location target_block_loc = getGrantLocation(player);
		
		for(int x=0; x<quantity; x++) {
			if(sloppy_send) {
				target_block_loc = getSloppyCoords(player, target_block_loc, quantity, x);
			}
			
			if(entity_type.name().equals("OCELOT") || entity_type.name().equals("WOLF")) { 
				spawnPet(player,entity_type,target_block_loc);
			} else {
				world.spawnEntity(target_block_loc, entity_type);
			}
		}
	}
	
	private void sendMaterialToPlayer(Player player, Material type, int quantity, Boolean sloppy_send) {
		Location target_block_loc = getGrantLocation(player);
		
		if(sloppy_send) {
			ItemStack item = new ItemStack(type, 1);

			for(int x=0; x<quantity; x++) {	
				player.getWorld().dropItemNaturally(getSloppyCoords(player, target_block_loc, quantity, x), item);
			}
		} else {
			ItemStack item = new ItemStack(type, quantity);
			world.dropItemNaturally(target_block_loc, item);
		}
	}
	
	private Location getGrantLocation(Player player) {
		Block target_block = player.getTargetBlock(null, 2);
		Location target_block_loc = new Location(world, target_block.getX(), player.getLocation().getY()+2, target_block.getZ());
		return target_block_loc;
	}
	
	private Location getSloppyCoords(Player player, Location loc, int quantity, int index) {
		int adjustor = 2;
		Double adjusted_x = (double) (loc.getX() - adjustor);
		Double adjusted_z = (double) (loc.getZ() - adjustor);
		return new Location(player.getWorld(), adjusted_x, loc.getY(), adjusted_z);
	}
	
	private void blessCrops() {
		Location players_loc = grantee.getLocation();
		int range = 10;
		
		for(int x=range*-1; x<range; x++) {
			for(int y=range*-1; y<range; y++) {
				for(int z=range*-1; z<range; z++) {
					Location test_loc = new Location(world,players_loc.getX()+x,players_loc.getY()+y,players_loc.getZ()+z);
					Block test_block_type = world.getBlockAt(test_loc);

					// Find any crops and ripen them.
					String block_type_txt = test_block_type.getType().toString();
					
					if(block_type_txt.equals("CROPS") || block_type_txt.equals("POTATO") || block_type_txt.equals("CARROT") || block_type_txt.equals("MELON_STEM") || block_type_txt.equals("PUMPKIN_STEM") || block_type_txt.equals("NETHER_WARTS")) {
						test_block_type.setData(CropState.RIPE.getData());
					}
					if(block_type_txt.equals("COCOA")) {
						BlockState this_plant_state = test_block_type.getState();
						CocoaPlant cocoa_plant = (CocoaPlant) this_plant_state.getData();
						cocoa_plant.setSize(CocoaPlantSize.LARGE);
						this_plant_state.update();
					}
				}
			}		
		}
	}
	
	private void createFlood() {
		Location players_loc = grantee.getLocation();
		int range = 3;
		
		for(int x=range*-1; x<range; x++) {
			for(int y=0; y<1; y++) {
				for(int z=range*-1; z<range; z++) {
					Location test_loc = new Location(world,players_loc.getX()+x,players_loc.getY()+y,players_loc.getZ()+z);
					Block test_block_type = world.getBlockAt(test_loc);
					
					String block_type_txt = test_block_type.getType().toString();
					if(block_type_txt.equals("AIR")) {
						test_block_type.setType(Material.WATER);
					}
				}
			}		
		}
	}
	
	private void curseTheWaters() {
		Location players_loc = grantee.getLocation();
		int range = 5;
		
		for(int x=range*-1; x<range; x++) {
			for(int y=-2; y<2; y++) {
				for(int z=range*-1; z<range; z++) {
					Location test_loc = new Location(world,players_loc.getX()+x,players_loc.getY()+y,players_loc.getZ()+z);
					Block test_block_type = world.getBlockAt(test_loc);
					
					String block_type_txt = test_block_type.getType().toString();
					if(block_type_txt.equals("STATIONARY_WATER") || block_type_txt.equals("WATER")) {
						Material curse_water_material = Material.matchMaterial(PrayerConfig.curse_waters_to_material);
						test_block_type.setType(curse_water_material);
					}
				}
			}		
		}
	}
	
	private void useTheForce() {
		String direction = PrayerUtil.getPlayerDirection(grantee);	
		Block targetBlock = grantee.getTargetBlock(null, 30);
		List<Entity> nearby_entities = grantee.getNearbyEntities(10, 10, 10);
		double send_x, send_y, send_z;
		double offset = 0.07;
		int range, x_range, y_range, z_range;
		range = x_range = y_range = z_range = 2;
		
		Boolean messy = false;
		if(messy) {
			Random generator_x = new Random();
			x_range = generator_x.nextInt(range)+1; // 1 of range
			Random generator_y = new Random();
			y_range = generator_y.nextInt(range-1)+1; // 1 of range
			z_range = range; 
		}
		
		for(int x=x_range*-1; x<x_range; x++) {
			for(int y=y_range*-1; y<y_range; y++) {
				for(int z=z_range*-1; z<z_range; z++) {
					Location test_loc = new Location(world,targetBlock.getX()+x,targetBlock.getY()+y,targetBlock.getZ()+z);
					Block test_block_type = world.getBlockAt(test_loc);
					test_block_type.breakNaturally();
					
					// Launch nearby mobs
					for(Entity nearby_entity : nearby_entities) {
						if(nearby_entity.getTicksLived() != 0) {	
							Location nearby_entity_loc = nearby_entity.getLocation();
							send_x = nearby_entity_loc.getX()+offset;
							send_y = nearby_entity_loc.getY()+offset;
							send_z = nearby_entity_loc.getZ()+offset;
							
							if(direction.equals("N") || direction.equals("NE") || direction.equals("NW")) {
								send_x = nearby_entity_loc.getX()-offset;
							} 
							if(direction.equals("S") || direction.equals("SW") || direction.equals("SE") || direction.equals("E")) { 
								send_z = nearby_entity_loc.getZ()-offset;
							} 
							
							Location send_loc = new Location(world, send_x, send_y, send_z);
							if(!send_loc.getBlock().getType().isSolid()) {
								nearby_entity.teleport(send_loc);
							}
						}
					}
				}
			}		
		}
	}

	
	private void logging(int prayer_limit, int prayer_range, int prayers_remaining, Boolean prayer_granted, Boolean no_punishment) {
		// Logging
		if(PrayerConfig.enable_verbose_logging) {
			String extra_alter_txt_log = (PrayerAltar.player_near_altar.get(player)) ? ", near an Altar," : "";
			PrayerUtil.log("["+player.getName()+"] prayed"+extra_alter_txt_log+" for \""+prayer_request+"\" with a prayer-limit of "+prayer_limit+" and a prayer-range of "+prayer_range+".");
		}
		if(PrayerConfig.enable_dev_logging) {
			PrayerUtil.log("["+player.getName()+"] limit: "+prayer_limit+", counter: "+executorInstance.getCurrentCount(player)+", remaining: "+prayers_remaining+", odds: 1/"+prayer_range+", no_punishment: "+no_punishment+", near_altar: "+PrayerAltar.player_near_altar.get(player)+", granted: "+prayer_granted);
		}
	}
	
	// The TimerTasks	
	class FirehandTask extends BukkitRunnable {
		public void run() {
			grantee.sendMessage(ChatColor.GRAY+"Thy power of firehand has ceased.");
			is_firehand = false;
		}
	}

	class FlightRollTaskBukkit extends BukkitRunnable {
		public void run() {
			if(!PrayerExecutor.isPlayerInCreative(grantee)) {
				grantee.sendMessage(ChatColor.GRAY+"Thy power of flight has ceased.");
				grantee.setFlying(false);
				grantee.setAllowFlight(false);
			}
		}
	}
	
}

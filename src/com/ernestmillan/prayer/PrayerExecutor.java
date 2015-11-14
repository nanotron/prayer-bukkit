package com.ernestmillan.prayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes the pray command. Validates pray attempts and decides whether to move onto PrayerGranter.
 */
public class PrayerExecutor implements CommandExecutor {
	
	Player player, recipient;
	public static Boolean unlimited_creative_prayer, unlimited_op_prayer;
	private Boolean prayer_allowed, prayer_permitted, is_curse;
	private Prayer plugin;
	private String text_not_allowed = "Thou may not issue this command.";
	public static Map<Player, Integer> pray_counter = new HashMap<Player, Integer>();
	public static Map<Player, PrayerGranter> prayer_attempt = new HashMap<Player, PrayerGranter>(); 
	
	public PrayerExecutor(Prayer plugin, boolean is_curse) {
		this.plugin = plugin;
		this.is_curse = is_curse;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (sender instanceof Player) {
			player = (Player)sender;
		}		
		
		if (player == null) {
			sender.sendMessage("Thou must be in the game, amoungst mere mortals, to issue a prayer.");
		} else {
			if(player.hasPermission("prayer.pray") || player.hasPermission("prayer.curse")) {				
				if(PrayerExecutor.pray_counter.get(player) == null) {
					PrayerExecutor.pray_counter.put(player, 0);
				}
				
				// Pray Options
				String command_options_txt = StringUtils.join(Prayer.available_pray_options, ", ");
				if(!isPlayerInCreative(player)) {
					command_options_txt = command_options_txt.replace("giant, ", "");
				}
				// Curse Options
				String command_curse_options_txt = StringUtils.join(Prayer.available_curse_options, ", ");
				
				String available_options_txt;
				if(is_curse) {
					available_options_txt = ChatColor.YELLOW+"/curse"+ChatColor.RESET+" options: "+ChatColor.GOLD+command_curse_options_txt;
				} else {
					available_options_txt = ChatColor.YELLOW+"/"+commandLabel+ChatColor.RESET+" options: "+ChatColor.GOLD+command_options_txt;
				}
				
				PrayerAltar myAltar = new PrayerAltar(player, false);
				PrayerAltar.player_near_altar.put(player, myAltar.isNearAltar(false));
				
				isPrayerAllowed(player);
				isPrayerPermitted(player, args);
				
				if (args.length >= 1 && args[0].equalsIgnoreCase("info")) {
					int prayer_count = getCurrentCount(player);
					int prayers_remaining = getPrayerLimit(player) - prayer_count;
					String plural_counter = (prayer_count == 1) ? "" : "s";
					String plural_remaining = (prayers_remaining == 1) ? "" : "s";
					String altar_in_range_txt = PrayerAltar.player_near_altar.get(player) ? " and art near an Altar" : "";
					
					sender.sendMessage(ChatColor.GRAY+"Thou hast prayed "+ChatColor.WHITE+prayer_count+ChatColor.GRAY+" time"+plural_counter+" with "+ChatColor.WHITE+prayers_remaining+ChatColor.GRAY+" request"+plural_remaining+" remaining"+altar_in_range_txt+".");
				} else if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
					sender.sendMessage(ChatColor.GRAY+"To pray: "+ChatColor.WHITE+"/pray [option]");
					sender.sendMessage(ChatColor.GRAY+"To bless: "+ChatColor.WHITE+"/bless [option] [player]");
					sender.sendMessage(ChatColor.GRAY+"To curse: "+ChatColor.WHITE+"/curse [option] [player]");
					sender.sendMessage(ChatColor.GRAY+"Prayer quota and odds improve with XP level. "+ChatColor.WHITE+"Altars"+ChatColor.GRAY+" improve odds. More info: http://dev.bukkit.org/server-mods/prayer/");	
					sender.sendMessage(available_options_txt);
				} else {
					Boolean is_valid_option = false;
					if(args.length >= 1) {
						is_valid_option = isValidOption(args[0], is_curse);
					}
					
					//Date currentTime = new Date();
					//PrayerConfig.userConfig.getConfig().set("users", "blah");
					//PrayerConfig.userConfig.saveConfig();
					
					if(prayer_allowed && prayer_permitted) {
						if (args.length < 1 || !is_valid_option) {
							sender.sendMessage("What doest thou wish to pray for?");	
							sender.sendMessage(available_options_txt);
						} else if (args.length >= 1) {
							if (args.length == 2 && args[1] != null) {
								recipient = sender.getServer().getPlayer(args[1]);
								
								if(!PrayerConfig.enable_prayer_for_others) {
									sender.sendMessage(ChatColor.RED+"Thou may not pray for others.");
								} else if(recipient == null || !recipient.isOnline()) {
									sender.sendMessage(ChatColor.RED+"Target player must be in the game.");
								} else {
									prayer_attempt.put(player, new PrayerGranter(this, plugin, sender, is_curse, args, available_options_txt, recipient));
									concludePrayerExecute();
								}
							} else {
								if(is_curse) {
									sender.sendMessage(ChatColor.RED+"Thou must specify a recipient.");
								} else {
									prayer_attempt.put(player, new PrayerGranter(this, plugin, sender, is_curse, args, available_options_txt, null));
									concludePrayerExecute();
								}
							}
						}
					} else {
						//sender.sendMessage(ChatColor.RED+text_not_allowed);
						sender.sendMessage(text_not_allowed);

						if(PrayerConfig.enable_verbose_logging) {
							PrayerUtil.log(player.getName() + " was denied a Prayer request. Reason: \""+text_not_allowed+"\"");
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	
	private void concludePrayerExecute() {
		registerEvents(this, prayer_attempt.get(player));
		prayer_attempt.get(player).processPrayer();
	}
	
	private void registerEvents(PrayerExecutor executorInstance, PrayerGranter granterInstance) {
		plugin.getServer().getPluginManager().registerEvents(new PrayerGranterListeners(executorInstance, granterInstance), plugin);
	}
	
	public static boolean isPlayerInCreative(Player player) {
		return (player.getGameMode().toString() == "CREATIVE") ? true : false;
	}
	
	public int getCurrentCount(Player player) {
		return PrayerExecutor.pray_counter.get(player);
	}
	
	public static int getPrayerLimit(Player player) {
		int quota = PrayerConfig.fixed_prayer_session_quota;		
		return (PrayerConfig.enable_fixed_prayer_session_quota && quota > 0) ? quota : player.getLevel();
	}
	
	private void isPrayerAllowed(Player player) {
		prayer_allowed = true;
		
		// White-list
		if(PrayerConfig.enable_white_list) {
			Boolean is_user_in_white_list = isUserInList("white",player);
			if(is_user_in_white_list) {
				prayer_allowed = true;
			} else {
				prayer_allowed = false;
				text_not_allowed = "Thou may not issue a prayer, heathen.";
			}
		}
		
		if(!player.isOp() && !PrayerConfig.allow_non_operator_prayer) {
			prayer_allowed = false;
			text_not_allowed = "Only a chosen one, of operator status, may pray.";
		}
		
		// XP
		if(prayer_allowed && player.getLevel() == 0) {
			prayer_allowed = false;
			text_not_allowed = "Thou must obtain some experience points to pray.";
		}
				
		// Holy item
		if(prayer_allowed && PrayerConfig.require_item_in_hand) {			
			Boolean is_held = Material.matchMaterial(player.getItemInHand().getType().name()) == Material.matchMaterial(PrayerConfig.item_in_hand_name);
			
			if(!is_held) {
				prayer_allowed = false;
				String item_required_name = PrayerConfig.item_in_hand_name;
				text_not_allowed = "Thou must be holding a holy "+item_required_name+" item to pray.";
			}
		}
		
		// Creative
		if(isPlayerInCreative(player) && !PrayerConfig.allow_prayer_in_creative) {
			prayer_allowed = false;
			text_not_allowed = "Prayer is currently disabled in Creative mode.";
		}
		
		// Worlds		
		String playerEnvName = player.getWorld().getEnvironment().name();
		String may_not_txt = "Thou may not pray ";
		if(playerEnvName.equals("NETHER") && !PrayerConfig.allow_prayer_in_nether) {
			prayer_allowed = false;
			text_not_allowed = may_not_txt+"in the Nether.";
		}
		if(playerEnvName.equals("NORMAL") && !PrayerConfig.allow_prayer_in_normal_world) {
			prayer_allowed = false;
			text_not_allowed = may_not_txt+"here.";
		}
		if(playerEnvName.equals("THE_END") && !PrayerConfig.allow_prayer_in_end) {
			prayer_allowed = false;
			text_not_allowed = may_not_txt+"in the End.";
		}
		
		// Altar 	
		if(!PrayerAltar.player_near_altar.get(player) && PrayerConfig.require_altar_for_prayer) {
			prayer_allowed = false;
			text_not_allowed = "Thou must be near an Altar to pray.";
		} 
		
		// Unlimited Overrides
		if(isPlayerOpUnlimited(player)) {
			prayer_allowed = true;
		}
		if(isPlayerCreativeUnlimited(player)) {
			prayer_allowed = true;
		}
		
		// Curse
		if(is_curse && prayer_allowed && !PrayerConfig.enable_curse_command) {
			prayer_allowed = false;
			text_not_allowed = "Thou may not curse thy fellow player.";
		}
		
		// Blacklist Override
		if(!PrayerConfig.enable_white_list) {
			Boolean is_user_in_black_list = isUserInList("black",player);	
			
			if(is_user_in_black_list) {
				prayer_allowed = false;
				text_not_allowed = "Thou may not issue a prayer, heathen.";
			}
		}
	}
	
	public static Boolean isPlayerCreativeUnlimited(Player player) {
		Boolean testState = (isPlayerInCreative(player) && PrayerConfig.allow_prayer_in_creative && PrayerConfig.allow_unlimited_prayer_in_creative) ? true : false;
		return testState;
	}
	
	public static Boolean isPlayerOpUnlimited(Player player) {
		Boolean testState = (player.isOp() && PrayerConfig.allow_unlimited_operator_prayer) ? true : false;
		return testState;
	}
	
	private void isPrayerPermitted(Player player, String[] args) {
		prayer_permitted = true;
		
		if(!is_curse && player.isPermissionSet("prayer.pray") && !player.hasPermission("prayer.pray")) {
			prayer_permitted = false;
		} 
		if(is_curse && player.isPermissionSet("prayer.curse") && !player.hasPermission("prayer.curse")) {
			prayer_permitted = false;
		}
		
		if(!is_curse && args.length >= 1 && prayer_permitted) {
			for(String command: Prayer.available_pray_options) {
				if(player.isPermissionSet("prayer.pray."+command) && !player.hasPermission("prayer.pray."+command) && args[0].equalsIgnoreCase(command)) {
					prayer_permitted = false;
					break;
				}
			}
		}
		if(is_curse && args.length >= 1 && prayer_permitted) {
			for(String command: Prayer.available_curse_options) {
				if(player.isPermissionSet("prayer.curse."+command) && !player.hasPermission("prayer.curse."+command) && args[0].equalsIgnoreCase(command)) {
					prayer_permitted = false;
					break;
				}
			}
		}
		
		if(!prayer_permitted) {
			text_not_allowed = "Thou dost not have permission to issue this option.";
		}
	}
	
	private Boolean isUserInList(String type, Player player) {
		try {
			String config_file_path = plugin.getDataFolder()+"/"+type+"-list.txt";
			File config_file = new File(config_file_path);
		    
		    if(config_file.exists()) {
		    	// Read File
		    	@SuppressWarnings("resource")
				BufferedReader config_content = new BufferedReader(new FileReader(config_file));
		    	String name;
		    	while ((name = config_content.readLine()) != null) {
		    		if(player.getName().equalsIgnoreCase(name)) return true;
		    	}
		    	config_content.close();
		    } else {
		    	PrayerUtil.log("Unable to read "+config_file_path+". Please fix, or delete it to regenerate.");
		    }
		} catch (IOException e) {
			PrayerUtil.log("Error encountered: "+e);
			return false;
		}
		return false;
	}
	
	public static Boolean isValidOption(String prayer_request, Boolean is_curse) {
		Boolean is_valid_option = false;
		
		if(is_curse) {
			for(int i=0;i<Prayer.available_curse_options.size();i++) {
				if (prayer_request.equalsIgnoreCase(Prayer.available_curse_options.get(i))) {
					is_valid_option = true;
					break;
				} 
			}
		} else {
			for(int i=0;i<Prayer.available_pray_options.size();i++) {
				if (prayer_request.equalsIgnoreCase(Prayer.available_pray_options.get(i))) {
					is_valid_option = true;
					break;
				} 
			}
		}
		return is_valid_option;
	}

}

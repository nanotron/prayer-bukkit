package com.ernestmillan.prayer;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 * Determines if an Altar is valid and if player is near one.
 */
public class PrayerAltar {
	
	private World world;
	private Boolean from_placed_block;
	private String alter_type_txt;
	private Player player;
	public static Map<Player, Boolean> player_near_altar = new HashMap<Player, Boolean>(); 
	public static Map<Player, Integer> altar_activator_block_type = new HashMap<Player, Integer>();
	public static Map<Player, Block> altar_activator_block = new HashMap<Player, Block>();
	
	public PrayerAltar(Player player, Boolean fromPlacedBlock) {
		this.from_placed_block = fromPlacedBlock;
		this.player = player;
		this.world = player.getWorld();
	}
	
	private void holyFire() {
		world.playEffect(world.getBlockAt(altar_activator_block.get(player).getLocation()).getRelative(0, 1, 0).getLocation(), Effect.MOBSPAWNER_FLAMES, null);
	}
	
	private void lightningActivatorEffect() {
		if(PrayerConfig.enable_altar_creation_lightning_effect) {
			world.strikeLightningEffect(altar_activator_block.get(player).getLocation());
		}
	}
	
	public void informUserOfValidAltar(Sign theSign) {
		if(from_placed_block && from_placed_block != null && !PrayerAltar.player_near_altar.get(player)) {
			player.sendMessage(ChatColor.LIGHT_PURPLE+"Thou hath pleased "+PrayerConfig.deity_name+" with "+alter_type_txt+" Altar.");
			holyFire();
			lightningActivatorEffect();
		}
	}
	
	public Boolean isNearAltar(Boolean confirmedAltarSign) {
		Location players_loc = player.getLocation();
		int range = from_placed_block ? 5 : 4;
		
		// Get blocks around a player
		for(int x=range*-1; x<range; x++) {
			for(int y=range*-1; y<range; y++) {
				for(int z=range*-1; z<range; z++) {
					Location test_loc = new Location(world,players_loc.getX()+x,players_loc.getY()+y,players_loc.getZ()+z);
					Sign the_sign;

					if(world.getBlockAt(test_loc).getType().toString().equals("WALL_SIGN")) {
						the_sign = (Sign)world.getBlockAt(test_loc).getState();

						if(doesAltarLabelExist(the_sign) || confirmedAltarSign) {
							Boolean is_complete_altar = isSignOnAltarBlocks(player, world.getBlockAt(test_loc));
							
							if(is_complete_altar) {	
								holyFire();
								informUserOfValidAltar(the_sign);
								return true;
							} 
						} 
					}
				}
			}		
		}
		return false;
	}
	
	private boolean doesAltarLabelExist(Sign the_sign) {
		Boolean label_state = (the_sign.getLine(0).equalsIgnoreCase(PrayerConfig.altar_label)) ? true : false;
		return label_state;
	}
	
	private boolean isSignOnAltarBlocks(Player player, Block targetBlock) {
		Boolean is_altar = false;
		Location sign_block_loc = targetBlock.getLocation();
		
		// Find the block behind the sign.
		Location behind_test_1_loc = new Location(world, sign_block_loc.getX()-1, sign_block_loc.getY(), sign_block_loc.getZ());
		Location behind_test_2_loc = new Location(world, sign_block_loc.getX()+1, sign_block_loc.getY(), sign_block_loc.getZ());
		Location behind_test_3_loc = new Location(world, sign_block_loc.getX(), sign_block_loc.getY(), sign_block_loc.getZ()-1);
		Location behind_test_4_loc = new Location(world, sign_block_loc.getX(), sign_block_loc.getY(), sign_block_loc.getZ()+1);
		
		Location behind_sign_block_loc = null;
		if(testBlockType(behind_test_1_loc,"OBSIDIAN")) {
			behind_sign_block_loc = behind_test_1_loc;
		} else if(testBlockType(behind_test_2_loc,"OBSIDIAN")) {
			behind_sign_block_loc = behind_test_2_loc;
		} else if(testBlockType(behind_test_3_loc,"OBSIDIAN")) {
			behind_sign_block_loc = behind_test_3_loc;
		} else if(testBlockType(behind_test_4_loc,"OBSIDIAN")) {
			behind_sign_block_loc = behind_test_4_loc;	
		}	
		
		if(behind_sign_block_loc != null) {	
			// Find side blocks and their redstone torches.
			Location side_block_1_loc = new Location(world, behind_sign_block_loc.getX()-1, behind_sign_block_loc.getY(), behind_sign_block_loc.getZ());
			Location side_block_2_loc = new Location(world, behind_sign_block_loc.getX()+1, behind_sign_block_loc.getY(), behind_sign_block_loc.getZ());
			Location side_block_3_loc = new Location(world, behind_sign_block_loc.getX(), behind_sign_block_loc.getY(), behind_sign_block_loc.getZ()-1);
			Location side_block_4_loc = new Location(world, behind_sign_block_loc.getX(), behind_sign_block_loc.getY(), behind_sign_block_loc.getZ()+1);
			
			Boolean side_block_w_redstone_pair_exists = false, side_block_w_redstone_1and2 = false, side_block_w_redstone_3and5 = false;
			
			if(testBlockType(side_block_1_loc,"OBSIDIAN") && testBlockType(side_block_2_loc,"OBSIDIAN") && getRedstoneAbove(side_block_1_loc) && getRedstoneAbove(side_block_2_loc)) {
				side_block_w_redstone_pair_exists = side_block_w_redstone_1and2 = true;
			} else if(testBlockType(side_block_3_loc,"OBSIDIAN") && testBlockType(side_block_4_loc,"OBSIDIAN") && getRedstoneAbove(side_block_3_loc) && getRedstoneAbove(side_block_4_loc)) {
				side_block_w_redstone_pair_exists = side_block_w_redstone_3and5 = true;
			}
			
			if(side_block_w_redstone_pair_exists) {
				// Find far-side blocks (enchantment tables).
				Location enchantment_block_1_loc = new Location(world, behind_sign_block_loc.getX()-2, behind_sign_block_loc.getY(), behind_sign_block_loc.getZ());
				Location enchantment_block_2_loc = new Location(world, behind_sign_block_loc.getX()+2, behind_sign_block_loc.getY(), behind_sign_block_loc.getZ());
				Location enchantment_block_3_loc = new Location(world, behind_sign_block_loc.getX(), behind_sign_block_loc.getY(), behind_sign_block_loc.getZ()-2);
				Location enchantment_block_4_loc = new Location(world, behind_sign_block_loc.getX(), behind_sign_block_loc.getY(), behind_sign_block_loc.getZ()+2);
				
				Boolean altar_without_activator_exists = false;
				if(side_block_w_redstone_1and2 && testBlockType(enchantment_block_1_loc,"ENCHANTMENT_TABLE") && testBlockType(enchantment_block_2_loc,"ENCHANTMENT_TABLE")) {
					altar_without_activator_exists = true;
				} else if(side_block_w_redstone_3and5 && testBlockType(enchantment_block_3_loc,"ENCHANTMENT_TABLE") && testBlockType(enchantment_block_4_loc,"ENCHANTMENT_TABLE")) {
					altar_without_activator_exists = true;
				}
				
				if(altar_without_activator_exists) {
					// Check for activator.
					Block activator_block = player.getWorld().getBlockAt(behind_sign_block_loc).getRelative(0, 1, 0); 
					String activator_block_type_txt = activator_block.getType().toString();
					altar_activator_block.put(player, activator_block);
					altar_activator_block_type.put(player, 0);
					
					if(activator_block_type_txt.equals("JACK_O_LANTERN") && PrayerConfig.allow_altar_type_basic) {
						altar_activator_block_type.put(player, 1);
						alter_type_txt = "an";
					} else if (activator_block_type_txt.equals("GLOWSTONE") && PrayerConfig.allow_altar_type_nether) {
						altar_activator_block_type.put(player, 2);
						alter_type_txt = "a Nether";
					} else if (activator_block_type_txt.equals("DRAGON_EGG") && PrayerConfig.allow_altar_type_end) {
						altar_activator_block_type.put(player, 3);
						alter_type_txt = "an End";
					} 
					
					if(altar_activator_block_type.get(player) != 0) {
						is_altar = true;
					} 
				}
			}
		}
		return is_altar;
	}
	
	private Boolean testBlockType(Location blockLoc, String typeName) {
		return player.getWorld().getBlockAt(blockLoc).getType().toString().equals(typeName);
	}
	
	private Boolean getRedstoneAbove(Location sideBlockLoc) {
		return player.getWorld().getBlockAt(sideBlockLoc).getRelative(0, 1, 0).getType().toString().equals("REDSTONE_TORCH_ON");
	}
}

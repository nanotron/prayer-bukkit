package com.ernestmillan.prayer;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Constructs shelter around a player.
 * @author Ernest Millan
*/
public class PrayerShelter {
	
	private Player player;
	private World world;
	
	public PrayerShelter(Player grantee) {
		this.player = grantee;
		this.world = grantee.getWorld();
	}
	
	public void build() {
		Material base_mat = baseMaterial();
		
		// Player blocks
		Block players_block = player.getLocation().getBlock();
		Block block_beneath_player = players_block.getRelative(0, -1, 0);
		block_beneath_player.setType(base_mat);
		
		// Air things out.
		for(int a=-2; a <= 2; a++) { 
			for(int b=0; b <= 2; b++) {
				for(int c=-2; c <= 2; c++) {	
					Location test_loc = new Location(world,players_block.getX()+a,players_block.getY()+b,players_block.getZ()+c);
					test_loc.getBlock().setType(Material.AIR);
				}
			}
		}
		
		// Floor
		for(int x=-2; x <= 2; x++) { 
			for(int z=-2; z <= 2; z++) {
				Block x_blocks = block_beneath_player.getRelative(x, 0, z);
				x_blocks.setType(base_mat);
				Block z_blocks = block_beneath_player.getRelative(z, 0, x);
				z_blocks.setType(base_mat);
			}
		}
		
		// Floor Torch
		players_block.setType(Material.TORCH);
		
		// Walls - Beginning with bottom-center block
		constructWallSides(players_block.getRelative(2, 0, 0), base_mat);
		constructWallSides(players_block.getRelative(-2, 0, 0), base_mat);
		constructWalls(players_block.getRelative(0, 0, 2), base_mat);
		constructWalls(players_block.getRelative(0, 0, -2), base_mat);
		
		// Air out the Entrance
		for(int az=-3; az<=-2; az++) {
			players_block.getRelative(0, 0, az).setType(Material.AIR);
			players_block.getRelative(0, 1, az).setType(Material.AIR);
		}
		
		// Doors
		buildDoor(players_block, base_mat);
		
		// Roof
		Block center_of_roof = players_block.getRelative(0, 3, 0);
		for(int rx=-1; rx <= 1; rx++) { 
			for(int rz=-1; rz <= 1; rz++) {
				Block rx_blocks = center_of_roof.getRelative(rx, 0, rz);
				rx_blocks.setType(base_mat);
				Block rz_blocks = center_of_roof.getRelative(rz, 0, rx);
				rz_blocks.setType(base_mat);
			}
		}
		center_of_roof.setType(Material.GLASS);
	}
	
	private Material baseMaterial() {
		String current_biome_name = world.getBiome((int)player.getLocation().getX(), (int)player.getLocation().getZ()).toString();
		Material base_mat;
		
		if(current_biome_name.equalsIgnoreCase("HELL")) {
			base_mat = Material.NETHER_BRICK;
		} else if(current_biome_name.equalsIgnoreCase("SKY")) {
			base_mat = Material.OBSIDIAN;
		} else if(current_biome_name.equalsIgnoreCase("RIVER")) {
			base_mat = Material.GLASS;
		} else if( current_biome_name.equalsIgnoreCase("OCEAN")) {
			base_mat = Material.SPONGE;	
		} else if(current_biome_name.startsWith("DESERT")) {
			base_mat = Material.SANDSTONE;
		} else if(current_biome_name.startsWith("ICE_") || current_biome_name.startsWith("FROZEN_") || current_biome_name.equalsIgnoreCase("TAIGA")) {
			base_mat = Material.SNOW_BLOCK;	
		} else if(current_biome_name.startsWith("JUNGLE")) {
			base_mat = Material.MOSSY_COBBLESTONE;
		} else if(current_biome_name.equalsIgnoreCase("SWAMPLAND")) {
			base_mat = Material.WOOD;
		} else {
			base_mat = Material.WOOD;
		}
		
		Random generator = new Random();
		int roll = generator.nextInt(500) + 1; // 1 of 500
		if(roll == 1) {
			base_mat = Material.EMERALD_BLOCK;
		}
		
		return base_mat;
	}
	
	private void constructWalls(Block wall_middle_bottom, Material base_mat) {
		// Levels 1, 2, 3
		for(int wby = 0; wby <= 2; wby++) {
			for(int wbx = -1; wbx <= 1; wbx++) {
				wall_middle_bottom.getRelative(wbx, wby, 0).setType(base_mat);
			}
		}
		// Level 2
		wall_middle_bottom.getRelative(0, 1, 0).setType(Material.THIN_GLASS);
	}
	
	private void constructWallSides(Block wall_middle_bottom, Material base_mat) {
		// Levels 1, 2, 3
		for(int wby = 0; wby <= 2; wby++) {
			for(int wbz = -2; wbz <= 2; wbz++) {
				wall_middle_bottom.getRelative(0, wby, wbz).setType(base_mat);
			}
		}
		// Level 2
		wall_middle_bottom.getRelative(0, 1, 0).setType(Material.THIN_GLASS);
	}
	
	private void buildDoor(Block players_block, Material base_mat) {
		Block door_block = players_block.getRelative(0, 0, -2);
		Block door_block_top = players_block.getRelative(0, 1, -2);
		
		if(base_mat.toString().equalsIgnoreCase("GLASS")) {		
			// Fence Gates for Water Seal
			door_block.setType(Material.FENCE_GATE);
		} else {
			// Regular Door
			door_block.setType(Material.WOODEN_DOOR);
			door_block.setData((byte) 1, true);
			
			door_block_top.setType(Material.WOODEN_DOOR);
			door_block_top.setData((byte) 9, true);
		}
	}
}

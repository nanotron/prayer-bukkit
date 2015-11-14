package com.ernestmillan.prayer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Global listeners.
 * @author Ernest Millan
 */
public class PrayerListeners implements Listener {
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){	
		if(PrayerConfig.enable_player_move_event && event.getPlayer() instanceof Player){
			processAltarTest((Player)event.getPlayer(), false, false);
		}
	}
	
	@EventHandler
	public void onPlayerBlockPlace(BlockPlaceEvent event){	
		if(event.getPlayer() instanceof Player){
			if(isAltarBlock(event.getBlockPlaced().getTypeId())) {
				processAltarTest((Player)event.getPlayer(), true, false);
			}
		}
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent event) {
		if(event.getPlayer() instanceof Player) {
			if(event.getLine(0).equalsIgnoreCase(PrayerConfig.altar_label)) {
				processAltarTest((Player)event.getPlayer(), true, true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerBreak(BlockBreakEvent event){	
		if(event.getPlayer() instanceof Player){
			if(isAltarBlock(event.getBlock().getTypeId())) {
				processAltarTest((Player)event.getPlayer(), false, false);
			}
		}
	}
	
	private void processAltarTest(Player player, Boolean fromPlacedBlock, Boolean confirmedAltarSign) {
		PrayerAltar anAltar = new PrayerAltar(player, fromPlacedBlock);
		PrayerAltar.player_near_altar.put(player, anAltar.isNearAltar(confirmedAltarSign));
	}
	
	private boolean isAltarBlock(int placed_block) {
		int[] altar_block_ids = {49,116,323,76,91,89,122};
		
		for(int x=0;x<altar_block_ids.length;x++) {
			if(altar_block_ids[x] == placed_block) {
				return true;
			}
		}
		return false;
	}
	
}

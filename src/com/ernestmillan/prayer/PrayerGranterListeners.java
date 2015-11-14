package com.ernestmillan.prayer;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

/**
 * The listeners which affect both a granter or a grantee.
 */
public class PrayerGranterListeners implements Listener {
	
	PrayerGranter granterInstance;
	PrayerExecutor executorInstance;
	
	public PrayerGranterListeners(PrayerExecutor executorInstance, PrayerGranter granterInstance) {
		this.executorInstance = executorInstance;
		this.granterInstance = granterInstance;
	}

	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event) {	
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();

			if(granterInstance.is_flying && isEventOnGrantee(player)) {
				if((event.getCause() == DamageCause.FALL) && !PrayerExecutor.isPlayerInCreative(granterInstance.grantee)) {
					event.setCancelled(true);
					granterInstance.is_flying = false;
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerStrike(EntityDamageByEntityEvent event) {			
		if(granterInstance.is_firehand && granterInstance.grantee instanceof Player && event.getDamager().toString().contains(granterInstance.grantee.getName())) {
			event.getEntity().setFireTicks(PrayerConfig.option_firehand_duration_of_fire_dealt);
		}
	}
	
	@EventHandler
	public void onPlayerDeath(EntityDeathEvent event) {
		if(event.getEntity() instanceof Player){
			Player player = (Player) event.getEntity();
			
			if(isEventOnGrantee(player)) {
				if(PrayerConfig.reset_granted_cooldown_on_death) {
					resetCooldownGranted(player);
				}
				if(granterInstance.is_firehand) {
					granterInstance.firehandTimer.purge();
					granterInstance.is_firehand = false;
				}
				if(granterInstance.is_flying) {
					granterInstance.flightTimer.purge();
					granterInstance.is_flying = false;
				}
				PrayerExecutor.pray_counter.put(player, 0);
			}
		}
	}
	
	@EventHandler
	public void onPlayerXPChange(EnchantItemEvent event) {
		if(event != null && event.getEnchanter() != null && event.getEnchanter() instanceof Player) {
			Player player = (Player)event.getEnchanter();
			
			if(player != null) {
				double percentage_of_change = ((double)event.getExpLevelCost() / (double)player.getLevel());
				int prayer_count = (PrayerExecutor.pray_counter != null && PrayerExecutor.pray_counter.get(player) != null) ? PrayerExecutor.pray_counter.get(player) : 0;
				int adjusted_prayer_count = (int) (prayer_count - Math.ceil(percentage_of_change * (double) prayer_count));		
				if(PrayerExecutor.pray_counter != null && PrayerExecutor.pray_counter.get(player) != null) {
					PrayerExecutor.pray_counter.put(player, adjusted_prayer_count);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerSleep(PlayerBedLeaveEvent event) {
		Player player = (Player) event.getPlayer();
		
		PrayerExecutor.pray_counter.put(player, 0);
		if(PrayerConfig.reset_granted_cooldown_on_sleep) {
			resetCooldownGranted(player);
		}
	}
	
	private Boolean isEventOnGrantee(Player player) {
		return granterInstance.grantee.getName().equalsIgnoreCase(player.getName());
	}
	
	private void resetCooldownGranted(Player player) {
		if(PrayerGranter.cooldown_between_granted_active.get(player) != null && PrayerGranter.cooldown_between_granted_active.get(player)) {
			if(granterInstance.cooldownGrantedTimer != null) {
				granterInstance.cooldownGrantedTimer.purge();
			}
			PrayerGranter.cooldown_between_granted_active.put(player, false);
		}
	}
}

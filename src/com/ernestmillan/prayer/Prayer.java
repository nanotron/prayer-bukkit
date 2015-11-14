/**
 * Minecraft Prayer Plugin for Bukkit 			  
 * by Ernest Millan	/ emillan    				  
 * Version 1.2.3						  
 * for bukkit-1.7.9-R0.2.jar		  
 */

package com.ernestmillan.prayer;

import java.util.ArrayList;

import org.bukkit.plugin.java.JavaPlugin;

public class Prayer extends JavaPlugin {

	public static ArrayList<String> available_pray_options = new ArrayList<String>();
	public static ArrayList<String> available_curse_options = new ArrayList<String>();
	
	@Override
	public void onEnable() {	
		PrayerConfig myPrayerConfig = new PrayerConfig(this);
		myPrayerConfig.process();
		
		this.getServer().getPluginManager().registerEvents(new PrayerListeners(), this);
		
		PrayerExecutor myPrayerExecutor = new PrayerExecutor(this, false);
		getCommand("pray").setExecutor(myPrayerExecutor);
		
		PrayerExecutor myCurseExecutor = new PrayerExecutor(this, true);
		getCommand("curse").setExecutor(myCurseExecutor);
		
		PrayerUtil.log("Thy Prayer plugin hath been enabled.");
	}
	
	@Override
	public void onDisable() {
		PrayerUtil.log("Thy Prayer plugin hath been disabled.");
	}
}



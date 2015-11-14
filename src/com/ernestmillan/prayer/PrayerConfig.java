package com.ernestmillan.prayer;

import java.util.ArrayList;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Initializes configuration related values and settings.
 */
public class PrayerConfig {
	
	private JavaPlugin plugin;
	public static String deity_name, altar_label, curse_waters_to_material, item_in_hand_name;
	public static boolean allow_altar_type_basic, allow_altar_type_nether, allow_altar_type_end, reset_granted_cooldown_on_sleep, reset_granted_cooldown_on_death, enable_altar_creation_lightning_effect, allow_prayer_for_others_across_worlds, enable_curse_command, 
	enable_player_move_event, enable_fixed_prayer_session_quota, enable_verbose_logging, enable_dev_logging, enable_white_list, enable_prayer_for_others, allow_villager_influence, allow_altar_influence, 
	allow_skull_influence, allow_unlimited_operator_prayer, allow_non_operator_prayer, require_item_in_hand, require_altar_for_prayer, allow_prayer_in_creative, allow_unlimited_prayer_in_creative, 
	allow_prayer_in_normal_world, allow_prayer_in_nether, allow_prayer_in_end, enable_cooldown_between_granted_prayers, enable_cooldown_between_attempted_prayers;
	public static int duration_nightvision_ms, duration_nightvision, duration_invisibility, duration_invisibility_ms, fixed_prayer_session_quota, excessive_prayer_damage_level, request_op_damage_level, 
	duration_air_ms, duration_air, duration_firehand_ms, duration_firehand, duration_flight, duration_jump_ms, duration_jump, duration_speed_ms, duration_speed, duration_storm_ms, duration_storm, 
	duration_strength_ms, duration_strength, cooldown_duration_of_granted_prayer, cooldown_duration_of_attempted_prayer, option_minions_amount, option_storm_fish_amount, 
	option_firehand_duration_of_fire_dealt_ms, option_firehand_duration_of_fire_dealt;
	
	public PrayerConfig(JavaPlugin plugin) {
		this.plugin = plugin;
		plugin.getConfig().options().copyDefaults(true);
		plugin.saveConfig();
	}
	
	public void process() {
		int default_duration = 180000; // 3 minutes
		
		// Strings
		altar_label = "[altar]";
		deity_name = plugin.getConfig().getString("deity-name-text", "the gods");
		curse_waters_to_material = plugin.getConfig().getString("curse-waters-to-material", "fire");
		item_in_hand_name = plugin.getConfig().getString("item-in-hand-for-prayer-name", "book");
		
		// Booleans
		enable_verbose_logging = plugin.getConfig().getBoolean("enable-verbose-logging", false);
		enable_dev_logging = plugin.getConfig().getBoolean("enable-dev-logging", false);
		enable_white_list = plugin.getConfig().getBoolean("enable-white-list", false);
		enable_prayer_for_others = plugin.getConfig().getBoolean("enable-prayer-for-others", true);
		allow_prayer_for_others_across_worlds = plugin.getConfig().getBoolean("allow-prayer-for-others-across-worlds", true);
		allow_unlimited_operator_prayer = plugin.getConfig().getBoolean("allow-unlimited-operator-prayer", true);
		allow_non_operator_prayer = plugin.getConfig().getBoolean("allow-non-operator-prayer", true);
		require_item_in_hand = plugin.getConfig().getBoolean("require-item-in-hand-for-prayer", true);
		require_altar_for_prayer = plugin.getConfig().getBoolean("require-altar-for-prayer", false);
		enable_curse_command = plugin.getConfig().getBoolean("enable-curse-command", true);
		allow_prayer_in_creative = plugin.getConfig().getBoolean("allow-prayer-in-creative", true);
		allow_unlimited_prayer_in_creative = plugin.getConfig().getBoolean("allow-unlimited-prayer-in-creative", true);
		allow_prayer_in_normal_world = plugin.getConfig().getBoolean("allow-prayer-in-normal-world", true);
		allow_prayer_in_nether = plugin.getConfig().getBoolean("allow-prayer-in-nether", true);
		allow_prayer_in_end = plugin.getConfig().getBoolean("allow-prayer-in-end", true);
		allow_villager_influence = plugin.getConfig().getBoolean("allow-villager-influence", true);
		allow_altar_influence = plugin.getConfig().getBoolean("allow-altar-influence", true);
		allow_altar_type_basic = plugin.getConfig().getBoolean("allow-altar-type-basic", true);
		allow_altar_type_nether = plugin.getConfig().getBoolean("allow-altar-type-nether", true);
		allow_altar_type_end = plugin.getConfig().getBoolean("allow-altar-type-end", true);
		allow_skull_influence = plugin.getConfig().getBoolean("allow-skull-influence", true);
		enable_cooldown_between_granted_prayers = plugin.getConfig().getBoolean("enable-cooldown-between-granted-prayers", true);
		enable_cooldown_between_attempted_prayers = plugin.getConfig().getBoolean("enable-cooldown-between-attempted-prayers", false);
		enable_fixed_prayer_session_quota = plugin.getConfig().getBoolean("enable-fixed-prayer-session-quota", false);
		enable_player_move_event = plugin.getConfig().getBoolean("enable-player-move-event", true);
		enable_altar_creation_lightning_effect = plugin.getConfig().getBoolean("enable-altar-creation-lightning-effect", true);
		reset_granted_cooldown_on_sleep = plugin.getConfig().getBoolean("reset-granted-cooldown-on-sleep", true);
		reset_granted_cooldown_on_death = plugin.getConfig().getBoolean("reset-granted-cooldown-on-death", true);
		
		// Ints
		fixed_prayer_session_quota = plugin.getConfig().getInt("fixed-prayer-session-quota", 10);
		excessive_prayer_damage_level = plugin.getConfig().getInt("excessive-prayer-damage-level", 12);
		request_op_damage_level = plugin.getConfig().getInt("request-op-damage-level", 10);
		cooldown_duration_of_granted_prayer = plugin.getConfig().getInt("cooldown-duration-of-granted-prayer", 60000);
		cooldown_duration_of_attempted_prayer = plugin.getConfig().getInt("cooldown-duration-of-attempted-prayer", 5000);
		option_minions_amount = plugin.getConfig().getInt("option-minions-amount", 5);
		option_storm_fish_amount = plugin.getConfig().getInt("option-storm-fish-amount", -1);

		// Ints w MS conversion
		option_firehand_duration_of_fire_dealt_ms = plugin.getConfig().getInt("option-firehand-duration-of-fire-dealt", 60000);
		option_firehand_duration_of_fire_dealt = convertMillisecondsToTicks(option_firehand_duration_of_fire_dealt_ms);
		duration_air_ms = plugin.getConfig().getInt("duration-of-air", default_duration);
		duration_air = convertMillisecondsToTicks(duration_air_ms);
		duration_firehand_ms = plugin.getConfig().getInt("duration-of-firehand", default_duration);
		duration_firehand = convertMillisecondsToTicks(duration_firehand_ms);
		duration_flight = plugin.getConfig().getInt("duration-of-flight", default_duration);
		duration_invisibility_ms = plugin.getConfig().getInt("duration-of-invisibility", default_duration);
		duration_invisibility = convertMillisecondsToTicks(duration_invisibility_ms);
		duration_jump_ms = plugin.getConfig().getInt("duration-of-jump", default_duration);
		duration_jump = convertMillisecondsToTicks(duration_jump_ms);
		duration_nightvision_ms = plugin.getConfig().getInt("duration-of-nightvision", default_duration);
		duration_nightvision = convertMillisecondsToTicks(duration_nightvision_ms);
		duration_speed_ms = plugin.getConfig().getInt("duration-of-speed", default_duration);
		duration_speed = convertMillisecondsToTicks(duration_speed_ms);
		duration_storm_ms = plugin.getConfig().getInt("duration-of-storm", default_duration);
		duration_storm = convertMillisecondsToTicks(duration_storm_ms);
		duration_strength_ms = plugin.getConfig().getInt("duration-of-strength", default_duration);
		duration_strength = convertMillisecondsToTicks(duration_strength_ms);
		
		constructOptionsFromMap("pray", "available-prayer-options", Prayer.available_pray_options);
		constructOptionsFromMap("curse", "available-curse-options", Prayer.available_curse_options);
		
		createCustomConfigs();
	}
	
	private void createCustomConfigs() {
		PrayerCustomConfig white_list = new PrayerCustomConfig(plugin, "white-list.txt");
		PrayerCustomConfig black_list = new PrayerCustomConfig(plugin, "black-list.txt");
		
		white_list.generateFile();
		black_list.generateFile();
	}
	
	public static int convertMillisecondsToTicks(int ms) {
		return Math.round(ms/50);
	}
	
	private void constructOptionsFromMap(String type_txt, String configSection, ArrayList<String> options_list_array) {
		if(plugin.getConfig().isConfigurationSection(configSection)) {
			ConfigurationSection avail_prayer_config = plugin.getConfig().getConfigurationSection(configSection);
			Map<String, Object> options_map = avail_prayer_config.getValues(true);
			
			int index = 0;
			for (Map.Entry<String, Object> entry : options_map.entrySet()) {
				if(entry.getValue().toString().equalsIgnoreCase("true")) {
					//Prayer.log("key: " + entry.getKey() + ", value: " + entry.getValue());
					options_list_array.add(index, entry.getKey());
					index++;
				}
			}
			
			if(type_txt == "pray") {
				type_txt = "pray and /bless";
			}
				
			PrayerUtil.log("Enabled /"+type_txt+" options: "+options_list_array);
		} else {
			PrayerUtil.log("Unable to enable Prayer options. Please check config.yml or delete it to reset.");
		}
	}
	
}

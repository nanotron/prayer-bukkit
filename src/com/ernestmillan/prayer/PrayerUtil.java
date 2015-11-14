package com.ernestmillan.prayer;

import java.text.DecimalFormat;
import java.util.logging.Logger;

import org.bukkit.entity.Player;

/*
* @author Ernest Millan
*/

public class PrayerUtil {

	public static String getPlayerDirection(Player player) {
	    double rotation = (player.getLocation().getYaw() - 90) % 360;
	    if (rotation < 0) {
	        rotation += 360.0;
	    }
	     if (0 <= rotation && rotation < 22.5) {
	        return "N";
	    } else if (22.5 <= rotation && rotation < 67.5) {
	        return "NE";
	    } else if (67.5 <= rotation && rotation < 112.5) {
	        return "E";
	    } else if (112.5 <= rotation && rotation < 157.5) {
	        return "SE";
	    } else if (157.5 <= rotation && rotation < 202.5) {
	        return "S";
	    } else if (202.5 <= rotation && rotation < 247.5) {
	        return "SW";
	    } else if (247.5 <= rotation && rotation < 292.5) {
	        return "W";
	    } else if (292.5 <= rotation && rotation < 337.5) {
	        return "NW";
	    } else if (337.5 <= rotation && rotation < 360.0) {
	        return "N";
	    } else {
	        return null;
	    }
	}

	public static String genDurationText(double duration_ms) {
		String duration_text;
		double in_minutes = duration_ms/60000;
		DecimalFormat no_deci = new DecimalFormat("#####");
		
		if(duration_ms >= 60000) {
			String plural = (in_minutes == 1) ? "" : "s";
			String time_txt = no_deci.format(in_minutes);
			duration_text = time_txt+" minute"+plural;
		} else {
			double duration_result = in_minutes*60;
			String plural = (duration_result == 1) ? "" : "s";
			String time_txt = no_deci.format(duration_result);
			duration_text = time_txt+" second"+plural;
		}		
		return duration_text;
	}
	
	public static String getDurationTextComplete(int milliseconds) {
		String time;
		int seconds = milliseconds / 1000;
		
		if(seconds < 60) {
			time = seconds+"s";
		} else {
			int minutes = seconds / 60;
			if(minutes < 60) {
				time = minutes+"m "+(seconds-(minutes*60))+"s";
			} else {
				int hours = minutes / 60;
				time = hours+"h "+(minutes-(hours*60))+"m "+(seconds-(minutes*60))+"s";
			}
		}
		return time;
	}

	public static void log(String text) {
		Logger log = Logger.getLogger("Pray");
		log.info("[Prayer] "+text);
	}

}

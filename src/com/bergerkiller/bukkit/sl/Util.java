package com.bergerkiller.bukkit.sl;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Util {
		
	public static String replaceColors(String line) {
		int index = 0;
		while (true) {
			index = line.indexOf('&', index);
			if (index >= 0 && index < line.length() - 1) {
				char next = line.charAt(index + 1);
				if (next == '0' || next == '1' || next == '2' || next == '3' || next == '4' ||
						next == '5' || next == '6' || next == '7' || next == '8' || next == '9' ||
						next == 'a' || next == 'b' || next == 'c' || next == 'd' || next == 'e' || next == 'f') {
					line = line.substring(0, index) + '§' + line.substring(index + 1);
				}
				index++;
			} else {
				break;
			}
		}
		return line;
	}
	public static String[] replaceColors(String... lines) {
		for (int i = 0; i < lines.length; i++) {
			lines[i] = replaceColors(lines[i]);
		}
		return lines;
	}
				
	public static void hideBlock(Block b, Player forPlayer) {
		forPlayer.sendBlockChange(b.getLocation(), 0, (byte) 0);
	}
	public static void hideBlock(Block b) {
		for (Player p : b.getWorld().getPlayers()) {
			hideBlock(b, p);
		}
	}
		
	public static ChatColor getColor(char code, ChatColor def) {
		for (ChatColor color : ChatColor.values()) {
			if (code == color.toString().charAt(1)) {
				return color;
			}
		}
		return def;
	}
	
	public static String getVarName(String line) {
		int perstart = line.indexOf("%");
		if (perstart != -1) {
			int perend = line.lastIndexOf("%");
			String varname = "";
			if (perend == perstart) {
				//left or right...
				if (perstart == 0) {
					//R
					varname = line.substring(1);
				} else if (perstart == line.length() - 1) {
					//L
					varname = line.substring(0, line.length() - 1);
				} else if (line.substring(perstart).contains(" ")) {
					//L
					varname = line.substring(0, perstart);
				} else {
					//R
					varname = line.substring(perstart + 1);
				}
			} else {
				//Get in between the two %
				varname = line.substring(perstart + 1, perend);
			}
			if (!varname.equals("")) {
				if (!varname.contains(" ")) {
					return varname;
				}
			}
		}
		return null;
	}
	
	/*
	 * See http://www.rgagnon.com/javadetails/java-0106.html
	 */
	public static String now(String dateformat) {
		return now(new SimpleDateFormat(dateformat));
	}
	public static String now(SimpleDateFormat format) {
		return format.format(Calendar.getInstance().getTime()).trim();
	}
	    	
}

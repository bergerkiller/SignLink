package com.bergerkiller.bukkit.sl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.Directional;

public class Util {
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[SignLink] " + message);
	}
	
	public static boolean isSign(Block block) {
		if (block == null) return false;
		Material type = block.getType();
		return type == Material.WALL_SIGN || type == Material.SIGN_POST;
	}
	public static boolean isSign(Location blockloc) {
		if (blockloc == null) return false;
		return isSign(blockloc.getBlock());
	}
	public static BlockFace getFacing(Block block) {
		return ((Directional) block.getType().getNewData(block.getData())).getFacing();
	}
	public static Sign getSign(Block b) {
		return (Sign) b.getState();
	}
	
	public static boolean isLoaded(Block b) {
		return isLoaded(b.getLocation());
	}
	public static boolean isLoaded(Location l) {
		return l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4);
	}
	
	public static BlockFace[] getFaces() {
		return getFaces(false);
	}
	public static BlockFace[] getFaces(boolean addDown) {
		if (addDown) {
			return new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, 
					BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
		} else {
			return new BlockFace[] {BlockFace.NORTH, BlockFace.EAST, 
					BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP};
		}
	}
	public static ChatColor getColor(char code) {
		for (ChatColor color : ChatColor.values()) {
			if (code == color.toString().charAt(1)) {
				return color;
			}
		}
		return ChatColor.BLACK;
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
			varname = varname.trim();
			if (!varname.equals("")) {
				return varname;
			}
		}
		return null;
	}
	
	/*
	 * See http://www.rgagnon.com/javadetails/java-0106.html
	 */
	public static String now(String dateformat) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(dateformat);
		return sdf.format(cal.getTime()).trim();
	}
	
	/**
	 * CommandBook getTime function, credit go to them for this!
	 * @param time - The time to parse
	 * @return The name of this time
	 */
    public static String getTimeString(long time) {
        int hours = (int) ((time / 1000 + 8) % 24);
        int minutes = (int) (60 * (time % 1000) / 1000);
        return String.format("%02d:%02d (%d:%02d %s)",
                hours, minutes, (hours % 12) == 0 ? 12 : hours % 12, minutes,
                hours < 12 ? "am" : "pm");
    }
	
}

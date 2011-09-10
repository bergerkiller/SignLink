package com.bergerkiller.bukkit.sl;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.server.Packet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
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
	public static void setFacing(Block block, BlockFace facing) {
		org.bukkit.material.Sign sign = new org.bukkit.material.Sign();
		sign.setFacingDirection(facing);
		block.setData(sign.getData(), true);
	}
	
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
	
	public static Sign getSign(Block b) {
		return (Sign) b.getState();
	}
	
	public static void delay(Runnable runnable, long delay) {
		SignLink.plugin.getServer().getScheduler().scheduleSyncDelayedTask(SignLink.plugin, runnable, delay);
	}
	
	public static void sendPacket(Player player, Packet packet) {
		((CraftPlayer) player).getHandle().netServerHandler.sendPacket(packet);
	}
	
	public static void hideBlock(Block b, Player forPlayer) {
		forPlayer.sendBlockChange(b.getLocation(), 0, (byte) 0);
	}
	public static void hideBlock(Block b) {
		for (Player p : b.getWorld().getPlayers()) {
			hideBlock(b, p);
		}
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

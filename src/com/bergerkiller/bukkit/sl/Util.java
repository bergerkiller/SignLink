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
import org.bukkit.util.Vector;

public class Util {
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[SignLink] " + message);
	}
	
	public static Location move(Location loc, Vector offset) {
        // Convert rotation to radians
        float ryaw = -loc.getYaw() / 180f * (float) Math.PI;
        float rpitch = loc.getPitch() / 180f * (float) Math.PI;

        //Conversions found by (a lot of) testing
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        z -= offset.getX() * Math.sin(ryaw);
        z += offset.getY() * Math.cos(ryaw) * Math.sin(rpitch);
        z += offset.getZ() * Math.cos(ryaw) * Math.cos(rpitch);
        x += offset.getX() * Math.cos(ryaw);
        x += offset.getY() * Math.sin(rpitch) * Math.sin(ryaw);
        x += offset.getZ() * Math.sin(ryaw) * Math.cos(rpitch);
        y += offset.getY() * Math.cos(rpitch);
        y -= offset.getZ() * Math.sin(rpitch);
        return new Location(loc.getWorld(), x, y, z, loc.getYaw(), loc.getPitch());
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
	
    public static String[] remove(String[] input, int index) {
    	String[] rval = new String[input.length - 1];
    	int i = 0;
    	for (int ii = 0; ii < input.length; ii++) {
    		if (ii != index) {
    			rval[i] = input[ii];
    			i++;
    		}
    	}
    	return rval;
    }
    	
}

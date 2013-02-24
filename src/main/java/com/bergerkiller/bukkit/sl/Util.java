package com.bergerkiller.bukkit.sl;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Util {
	public static void hideBlock(Block b, Player forPlayer) {
		forPlayer.sendBlockChange(b.getLocation(), 0, (byte) 0);
	}
	public static void hideBlock(Block b) {
		for (Player p : b.getWorld().getPlayers()) {
			hideBlock(b, p);
		}
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
			if (!varname.isEmpty() && !varname.contains(" ")) {
				return varname;
			}
		}
		return null;
	}
}

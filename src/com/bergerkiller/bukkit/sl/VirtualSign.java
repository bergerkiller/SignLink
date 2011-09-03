package com.bergerkiller.bukkit.sl;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class VirtualSign {
	private static HashMap<BlockLocation, VirtualSign> virtualSigns = new HashMap<BlockLocation, VirtualSign>();
	public static VirtualSign add(Block b, String[] lines) {
		BlockLocation at = new BlockLocation(b);
		VirtualSign sign = new VirtualSign();
		sign.location = b.getLocation();
		sign.sign = Util.getSign(b);
		if (lines == null || lines.length < 4) {
		    lines = sign.sign.getLines();
		}
		sign.oldlines = new String[4];
		for (int i = 0; i < 4;i++) {
			sign.oldlines[i] = lines[i];
		}
		
		virtualSigns.put(at, sign);
		return sign;
	}
	public static VirtualSign add(Block b) {
		return add(b, null);
	}
	public static VirtualSign get(Location at) {
		BlockLocation loc = new BlockLocation(at);
		if (Util.isSign(at)) {
			VirtualSign sign = virtualSigns.get(loc);
			if (sign == null || !sign.isValid()) sign = add(at.getBlock());
			return sign;
		} else {
			virtualSigns.remove(loc);
			return null;
		}
	}
	public static VirtualSign get(Block b) {
		return get(b.getLocation());
	}
	public static boolean remove(Block b) {
		return virtualSigns.remove(new BlockLocation(b)) != null;
	}
	
	public boolean isLoaded() {
		return Util.isLoaded(this.getBlock());
	}
	public boolean isValid() {
		return Util.isSign(this.sign.getBlock());
	}
	
	private boolean changed = false;
	private Sign sign;
	private Location location;
	private String[] oldlines;
	
	public void setLine(int index, String line) {
		if (!this.sign.getLine(index).equals(line)) {
			this.sign.setLine(index, line);
			changed = true;
		}
	}
	public String getLine(int index) {
		if (this.oldlines[index] == null) return "";
		return this.oldlines[index];
	}
	public String getVirtualLine(int index) {
		return this.sign.getLine(index);
	}
	public Block getBlock() {
		return this.sign.getBlock();
	}
	public Location getLocation() {
		return this.getBlock().getLocation();
	}
	public World getWorld() {
		return this.sign.getWorld();
	}
	
	public void remove() {
		for (Map.Entry<BlockLocation, VirtualSign> entry : virtualSigns.entrySet()) {
			if (entry.getValue() == this) {
				virtualSigns.remove(entry.getKey());
				return;
			}
		}
		Variables.removeLocation(this.location.getBlock());
		
	}
	public void restore() {
		for (int i = 0;i < 4;i++) {
			this.setLine(i, this.oldlines[i]);
		}
		this.update(true);
	}
	public boolean update(boolean forced) {
		if (this.isValid()) {
			if (this.changed || forced) {
				sign.update();
				this.changed = false;
			}
			return true;
		} else {
			remove(this.sign.getBlock());
			return false;
		}
	}
	public static void updateAll(boolean forced) {
		for (VirtualSign sign : virtualSigns.values()) {
			if (!sign.update(forced)) {
				updateAll(forced);
				return;
			}
		}
	}
	public static void restoreAll() {
		for (VirtualSign sign : virtualSigns.values()) {
			sign.restore();
		}
	}
}
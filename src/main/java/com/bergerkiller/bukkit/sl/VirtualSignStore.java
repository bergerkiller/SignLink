package com.bergerkiller.bukkit.sl;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.collections.BlockMap;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class VirtualSignStore {
	private static BlockMap<VirtualSign> virtualSigns;

	public static void deinit() {
		virtualSigns.clear();
		virtualSigns = null;
	}

	public static void init() {
		virtualSigns = new BlockMap<VirtualSign>();
	}

	public static synchronized VirtualSign add(Block b, String[] lines) {
		if (virtualSigns == null) {
			return null;
		}
		VirtualSign vsign = new VirtualSign(b, lines);
		virtualSigns.put(b, vsign);
		return vsign;
	}

	public static VirtualSign add(Block b) {
		return add(b, null);
	}

	public static synchronized VirtualSign get(Location at) {
		return get(at.getBlock());
	}

	public static synchronized VirtualSign get(Block b) {
		return virtualSigns == null ? null : virtualSigns.get(b);
	}

	public static synchronized VirtualSign getOrCreate(Location at) {
		return getOrCreate(at.getBlock());
	}

	public static synchronized VirtualSign getOrCreate(Block b) {
		if (virtualSigns == null) {
			return null;
		}
		if (MaterialUtil.ISSIGN.get(b)) {
			VirtualSign sign = virtualSigns.get(b);
			if (sign == null || !sign.isValid()) {
				sign = add(b);
			}
			return sign;
		} else {
			virtualSigns.remove(b);
			return null;
		}
	}

	public static synchronized VirtualSign[] getAll() {
		if (virtualSigns == null) {
			return null;
		}
		return virtualSigns.values().toArray(new VirtualSign[0]);
	}

	public static synchronized boolean exists(Location at) {
		return exists(at.getBlock());
	}

	public static synchronized boolean exists(Block at) {
		return virtualSigns != null && virtualSigns.containsKey(at);
	}

	/**
	 * Removes a Vitual Sign from the storage.
	 * 
	 * @param signBlock to remove
	 * @return True if a Virtual Sign was removed, False if not
	 */
	public static synchronized boolean remove(Block signBlock) {
		if (virtualSigns == null || virtualSigns.remove(signBlock) == null) {
			return false;
		}
		for (Variable var : Variables.getAll()) {
			var.removeLocation(signBlock);
		}	
		return true;
	}

	public static synchronized void removeAll(World world) {
		Iterator<VirtualSign> iter = virtualSigns.values().iterator();
		while (iter.hasNext()) {
			if (iter.next().getWorld() == world) {
				iter.remove();
			}
		}
	}

	public static void updateAll() {
		for (VirtualSign sign : getAll()) {
			sign.update();
		}
	}

	public static void forcedUpdate(final Player forplayer, long delay) {
		if (forplayer == null) {
			return;
		}
		new Task(SignLink.plugin) {
			public void run() {
				forcedUpdate(forplayer);
			}
		}.start(delay);
	}

	public static synchronized void forcedUpdate(Player forplayer) {
		if (forplayer == null) {
			return;
		}
		for (VirtualSign sign : virtualSigns.values()) {
			sign.update(forplayer);
		}
	}

	public static synchronized void clearPlayer(String playerName) {
		for (VirtualSign sign : virtualSigns.values()) {
			sign.resetLines(playerName);
		}
	}

	public static synchronized void invalidateAll(Player player) {
		for (VirtualSign vs : virtualSigns.values()) {
			if (vs.isInRange(player)) {
				vs.invalidate(player);
			}
		}
	}
}

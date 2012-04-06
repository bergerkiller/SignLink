package com.bergerkiller.bukkit.sl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.sl.API.Variables;

import com.bergerkiller.bukkit.common.BlockMap;

public class VirtualSign {
	private static BlockMap<VirtualSign> virtualSigns;
	public static void deinit() {
		virtualSigns.clear();
		virtualSigns = null;
	}
	public static void init() {
		virtualSigns = new BlockMap<VirtualSign>();
	}
	public static VirtualSign add(Block b, String[] lines) {
		if (virtualSigns == null) return null;
		VirtualSign vsign = new VirtualSign();
		vsign.sign = BlockUtil.getSign(b);
		if (lines == null || lines.length < 4) {
		    lines = vsign.sign.getLines();
		}
		vsign.defaultlines = new VirtualLines(lines);
		vsign.playerlines = new HashMap<String, VirtualLines>();
		for (int i = 0; i < 4; i++) {
			vsign.oldlines[i] = vsign.sign.getLine(i);
		}
		synchronized (virtualSigns) {
			virtualSigns.put(b, vsign);
		}
		return vsign;
	}
	public static VirtualSign add(Block b) {
		return add(b, null);
	}
	public static VirtualSign get(Location at) {
		if (virtualSigns == null) return null;
		synchronized (virtualSigns) {
			Block b = at.getBlock();
			if (BlockUtil.isSign(b)) {
				VirtualSign sign = virtualSigns.get(b);
				if (sign == null || !sign.isValid()) sign = add(at.getBlock());
				return sign;
			} else {
				virtualSigns.remove(b);
				return null;
			}
		}
	}
	public static VirtualSign get(Block b) {
		return get(b.getLocation());
	}
	public static VirtualSign[] getAll() {
		if (virtualSigns == null) return null;
		synchronized (virtualSigns) {
			return virtualSigns.values().toArray(new VirtualSign[0]);
		}
	}

	public static boolean exists(Location at) {
		return exists(at.getBlock());
	}
	public static boolean exists(Block at) {
		if (virtualSigns == null) return false;
		synchronized (virtualSigns) {
			return virtualSigns.containsKey(at);
		}
	}
	
	public static boolean remove(VirtualSign sign) {
		return remove(sign.getBlock());
	}
	public static boolean remove(Block b) {
		if (virtualSigns == null) return false;
		synchronized (virtualSigns) {
			return virtualSigns.remove(b) != null;
		}
	}
	public static void removeAll(World world) {
		for (VirtualSign vs : getAll()) {
			if (vs.getWorld() == world) {
				remove(vs);
			}
		}
	}
	public static void updateAll() {
		for (VirtualSign sign : getAll()) {
			sign.update();
		}
	}
	public static void forcedUpdate(final Player forplayer, long delay) {
		if (forplayer == null) return;
		new Task(SignLink.plugin) {
			public void run() {
				forcedUpdate(forplayer);
			}
		}.start(delay);
	}
	public static void forcedUpdate(Player forplayer) {
		if (forplayer == null) return;
		Iterator<VirtualSign> iter = virtualSigns.values().iterator();
		while (iter.hasNext()) {
			iter.next().update(forplayer);
		}
	}
	public static void clearPlayer(String playername) {
		for (VirtualSign sign : virtualSigns.values()) {
			sign.playerlines.remove(playername);
		}
	}
	
	private Sign sign;
	private String[] oldlines = new String[4];
	private HashMap<String, VirtualLines> playerlines;
	private VirtualLines defaultlines;
	private HashSet<VirtualLines> outofrange = new HashSet<VirtualLines>();
	private boolean ignorePackets = false;
	private boolean wasUnloaded = false;
	private int signcheckcounter = 0;
	
	boolean ignorePacket() {
		if (ignorePackets) {
			return true;
		} else {
			ignorePackets = false;
			return false;
		}
	}
		
	public void resetLines() {
		this.playerlines.clear();
	}
	public void resetLines(Player player) {
		resetLines(player.getName());
	}
	public void resetLines(String playerName) {
		playerlines.remove(playerName);
	}
	
	public void remove() {
		remove(this);
		Variables.removeLocation(this.getBlock());
	}
	
	public VirtualLines getLines(String playerName) {
		if (playerName == null) return getLines();
		VirtualLines lines = playerlines.get(playerName);
		if (lines == null) {
			lines = new VirtualLines(defaultlines.get());
			lines.setChanged(true);
			playerlines.put(playerName, lines);
		}
		return lines;
	}
	public VirtualLines getLines(Player player) {
		if (player == null) return getLines();
		return getLines(player.getName());
	}
	public VirtualLines getLines() {
		return this.defaultlines;
	}
	public void setDefaultLine(int index, String value) {
		getLines().set(index, value);
	}
	public void setLine(int index, String value, String... players) {
		if (players == null || players.length == 0) {
			//Set all lines to this value at this index
			for (VirtualLines lines : playerlines.values()) {
				lines.set(index, value);
			}
			getLines().set(index, value);
		} else {
			for (String player : players) {
				getLines(player).set(index, value);
			}
		}
	}
	public String getLine(int index) {
		return getLine(index, null);
	}
	public String getLine(int index, String player) {
		return getLines(player).get(index);
	}
	
	public String[] getRealLines() {
		return this.sign.getLines();
	}
	public String getRealLine(int index) {
		return this.sign.getLine(index);
	}
	public void setRealLine(int index, String line) {
		this.sign.setLine(index, line);
	}
	
	public World getWorld() {
		return this.sign.getWorld();
	}
	public int getX() {
		return this.sign.getX();
	}
	public int getY() {
		return this.sign.getY();
	}
	public int getZ() {
		return this.sign.getZ();
	}
	public int getChunkX() {
		return this.getX() >> 4;
	}
	public int getChunkZ() {
		return this.getZ() >> 4;
	}
	public Block getBlock() {
		return this.sign.getBlock();
	}
	public Location getLocation() {
		return new Location(getWorld(), getX(), getY(), getZ());
	}
	
	public BlockFace getFacing() {
		Block b = this.getBlock();
		return ((Directional) b.getType().getNewData(b.getData())).getFacing();
	}
	public void setFacing(BlockFace facing) {
		org.bukkit.material.Sign sign = new org.bukkit.material.Sign();
		sign.setFacingDirection(facing);
		getBlock().setData(sign.getData(), true);
	}
	
	public boolean isLoaded() {
		return getWorld().isChunkLoaded(getChunkX(), getChunkZ());
	}
	public boolean isValid() {
		return BlockUtil.isSign(getBlock()) && this.sign.getLines() != null;
	}
	public boolean isInRange(Player player) {
		return isInRange(player.getLocation());
	}
	public boolean isInRange(Location loc) {
		if (loc.getWorld() != this.getWorld()) return false;
		int dx = (int) Math.abs(loc.getX() - getX());
		int dy = (int) Math.abs(loc.getY() - getY());
		int dz = (int) Math.abs(loc.getZ() - getZ());
		final int maxdistance = 60;
		return dx < maxdistance && dy < maxdistance && dz < maxdistance;
	}
	
	public String toString() {
		return "VirtualSign {" + this.getX() + ", " + this.getY() + ", " + this.getZ() + ", " + this.getWorld().getName() + "}";
	}
	
	public void invalidate(Player player) {
		getLines(player).setChanged();
	}
	public void invalidate(String player) {
		getLines(player).setChanged();
	}
		
	public static void invalidateAll(Player player) {
		synchronized (virtualSigns) {
			for (VirtualSign vs : virtualSigns.values()) {
				if (vs.isInRange(player)) vs.invalidate(player);
			}
		}
	}
	
	public void update() {
		this.update(false);
	}
	public void update(boolean forced) {			
		if (!this.isLoaded()) {
			wasUnloaded = true;
			return;
		} else if (wasUnloaded) {
			Block b = this.getBlock();
			if (!BlockUtil.isSign(b)) {
				this.remove();
				return;
			}
			this.sign = BlockUtil.getSign(b);
			wasUnloaded = false;
		}
		if (!this.isValid()) {
			this.remove();
			return;
		} else {
			//update the sign if needed (just in case the tile got swapped or destroyed?)
			if (TileEntityVirtualSign.replace(this.getX(), this.getY(), this.getZ(), this.getWorld()) || signcheckcounter++ % 20 == 0) {
				Block b = this.getBlock();
				if (!BlockUtil.isSign(b)) {
					this.remove();
					return;
				}
				this.sign = BlockUtil.getSign(b);
			}
		}
		//Allow packets to be sent (after a tick)
		this.ignorePackets = false;
		
		//real-time changes to the text
		for (int i = 0; i < 4; i++) {
			if (!this.oldlines[i].equals(this.sign.getLine(i))) {
				Block signblock = this.getBlock();
				String varname = Util.getVarName(this.oldlines[i]);
				if (varname != null) {
					Variables.get(varname).removeLocation(signblock, i);
				}
				this.oldlines[i] = this.sign.getLine(i);
				this.setLine(i, this.oldlines[i]);
				varname = Util.getVarName(this.oldlines[i]);
				if (varname != null) {
					Variables.get(varname).addLocation(signblock, i);
				}
			}
		}
		
		if (this.getLines().hasChanged()) {
			forced = true;
			this.getLines().setChanged(false);
		}
		
		for (Player player : getWorld().getPlayers()) {
			if (forced) {
				update(player);
			} else {
				VirtualLines lines = getLines(player);
				if (isInRange(player)) {
					if (outofrange.remove(lines) || lines.hasChanged()) {
						this.update(lines, player);
						lines.setChanged(false);
					}
				} else {
					outofrange.add(lines);
				}
			}
		}
		for (VirtualLines lines : playerlines.values()) {
			lines.setChanged(false);
		}
	}

	public void update(Player player) {
		update(getLines(player), player);
	}
	public void update(VirtualLines lines, Player player) {
		lines.updateSign(player, getX(), getY(), getZ());
	}
	
}
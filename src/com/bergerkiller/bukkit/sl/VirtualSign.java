package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;

import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class VirtualSign {
	private static HashMap<BlockLocation, VirtualSign> virtualSigns = new HashMap<BlockLocation, VirtualSign>();
	public static VirtualSign add(Block b, String[] lines) {
		BlockLocation at = new BlockLocation(b);
		VirtualSign vsign = new VirtualSign();
		vsign.sign = Util.getSign(b);
		if (lines == null || lines.length < 4) {
		    lines = vsign.sign.getLines();
		}
		vsign.defaultlines = new VirtualLines(lines);
		vsign.playerlines = new HashMap<String, VirtualLines>();
		for (int i = 0; i < 4; i++) {
			vsign.oldlines[i] = vsign.sign.getLine(i);
		}
		
		virtualSigns.put(at, vsign);
		return vsign;
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
	public static boolean remove(VirtualSign sign) {
		return remove(sign.getBlock());
	}
	public static boolean remove(Block b) {
		return virtualSigns.remove(new BlockLocation(b)) != null;
	}
	public static void updateAll() {
		for (VirtualSign sign : virtualSigns.values().toArray(new VirtualSign[0])) {
			sign.ignorePackets = false;
			sign.update();
		}
	}
	public static void forcedUpdate(Player forplayer, long delay) {
		Task t = new Task(forplayer) {
			public void run() {
				VirtualSign.forcedUpdate((Player) getArg(0));
			}
		};
		t.startDelayed(delay);
	}
	public static void forcedUpdate(Player forplayer) {
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
	public static void updatePlayerRange() {
		for (VirtualSign sign : virtualSigns.values()) {
			for (Player p : sign.getWorld().getPlayers()) {
				if (!sign.isInRange(p)) {
					VirtualLines lines = sign.getLines(p);
					if (!lines.hasChanged()) {
						
					}
				}
			}
		}
	}
	
	private Sign sign;
	private String[] oldlines = new String[4];
	private HashMap<String, VirtualLines> playerlines;
	private VirtualLines defaultlines;
	private HashSet<VirtualLines> outofrange = new HashSet<VirtualLines>();
	private boolean ignorePackets = false;
	
	public boolean ignorePacket() {
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
		Material type = getBlock().getType();
		return type == Material.SIGN_POST || type == Material.WALL_SIGN;
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
	
	public void invalidate(Player player) {
		getLines(player).setChanged();
	}
	public void invalidate(String player) {
		getLines(player).setChanged();
	}
		
	public void update() {
		this.update(false);
	}
	public void update(boolean forced) {	
		//Check for realtime changes to the text
		boolean realtimechange = false;
		for (int i = 0; i < 4; i++) {
			if (this.oldlines[i] != this.sign.getLine(i)) {
				realtimechange = true;
				this.oldlines[i] = this.sign.getLine(i);
			}
		}
		if (realtimechange) {
			//Actual lines changed...this sign needs to be re-added
			this.playerlines.clear();
			for (int i = 0; i < 4; i++) {
				setLine(i, this.oldlines[i]);
			}
			ArrayList<Variable> variables = new ArrayList<Variable>();
			if (Variables.find(null, variables, this.getLocation())) {
				for (Variable var : variables) {
					var.updateAll();
				}
			}
		}
		if (forced) {
			for (Player player : getWorld().getPlayers()) {
				update(player);
			}
			return;
		}
		if (!this.isLoaded()) return;
		if (this.isValid()) {
			//Replace the entity if this is needed
			TileEntityVirtualSign.replace(getX(), getY(), getZ(), getWorld());

			for (Player player : getWorld().getPlayers()) {
				VirtualLines lines = getLines(player);
				if (isInRange(player)) {
					if (outofrange.remove(lines) || lines.hasChanged()) {
						this.update(lines, player);
					}
				} else {
					outofrange.add(lines);
				}
			}
			for (VirtualLines lines : playerlines.values()) {
				lines.setChanged(false);
			}
		} else {
			this.remove();
		}
	}

	public void update(Player player) {
		update(getLines(player), player);
	}
	public void update(VirtualLines lines, Player player) {
		lines.updateSign(player, getX(), getY(), getZ());
	}
	
}
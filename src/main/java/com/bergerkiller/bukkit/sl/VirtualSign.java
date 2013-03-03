package com.bergerkiller.bukkit.sl;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.material.Directional;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.sl.API.Variables;

public class VirtualSign extends VirtualSignStore {
	private Sign sign;
	private final String[] oldlines = new String[4];
	private final HashMap<String, VirtualLines> playerlines = new HashMap<String, VirtualLines>();
	private final VirtualLines defaultlines;
	private HashSet<VirtualLines> outofrange = new HashSet<VirtualLines>();
	private boolean wasUnloaded = false;
	private int signcheckcounter = 0;

	protected VirtualSign(Block signBlock, String[] lines) {
		this.sign = BlockUtil.getSign(signBlock);
		if (lines == null || lines.length < 4) {
		    lines = this.sign.getLines();
		}
		this.defaultlines = new VirtualLines(lines);
		for (int i = 0; i < 4; i++) {
			this.oldlines[i] = lines[i];
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
		remove(this.getBlock());
	}

	public VirtualLines getLines(String playerName) {
		if (playerName == null) {
			return getLines();
		}
		VirtualLines lines = playerlines.get(playerName);
		if (lines == null) {
			lines = new VirtualLines(defaultlines.get());
			lines.setChanged(true);
			playerlines.put(playerName, lines);
		}
		return lines;
	}
	public VirtualLines getLines(Player player) {
		if (player == null) {
			return getLines();
		}
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
		return MaterialUtil.ISSIGN.get(getBlock()) && this.sign.getLines() != null;
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

	public void update() {			
		if (!this.isLoaded()) {
			wasUnloaded = true;
			return;
		} else if (wasUnloaded) {
			Block b = this.getBlock();
			if (!MaterialUtil.ISSIGN.get(b)) {
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
			if (signcheckcounter++ % 20 == 0) {
				Block b = this.getBlock();
				if (!MaterialUtil.ISSIGN.get(b)) {
					this.remove();
					return;
				}
				this.sign = BlockUtil.getSign(b);
			}
		}

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

		for (Player player : getWorld().getPlayers()) {
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
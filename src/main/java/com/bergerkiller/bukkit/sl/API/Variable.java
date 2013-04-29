package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.LogicUtil;
import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.SignDirection;
import com.bergerkiller.bukkit.sl.VirtualSign;

/**
 * All-encompassing class that stores the information of a single variable
 */
public class Variable implements VariableValue {
	private String defaultvalue;
	private Ticker defaultticker;
	private String name;
	private ArrayList<LinkedSign> boundTo = new ArrayList<LinkedSign>();
	private HashMap<String, PlayerVariable> playervariables = new HashMap<String, PlayerVariable>();

	Variable(String defaultvalue, String name) {
		this.defaultvalue = defaultvalue;
		this.name = name;
		this.defaultticker = new Ticker(this.defaultvalue);
	}

	/**
	 * Gets the name of this Variable
	 * 
	 * @return variable name
	 */
	public String getName() {
		return this.name;
	}

	@Override
	public void clear() {
		this.playervariables.clear();
		this.set("%" + this.name + "%");
		this.defaultticker = new Ticker(this.defaultvalue);
	}

	@Override
	public Ticker getTicker() {
		for (PlayerVariable pvar : this.forAll()) {
			pvar.ticker = this.defaultticker;
		}
		return this.defaultticker;
	}

	/**
	 * Gets the ticker used for all (new) players that have no specific ticker or value set
	 * 
	 * @return Default ticker
	 */
	public Ticker getDefaultTicker() {
		return this.defaultticker;
	}

	/**
	 * Gets the default value used for all (new) players that have no specific value set
	 * 
	 * @return Default value
	 */
	public String getDefault() {
		return this.defaultvalue;
	}

	/**
	 * Gets the Variable value for the player specified
	 * 
	 * @param playername to get the variable value of
	 * @return Player-specific variable value, or the default if none is set
	 */
	public String get(String playername) {
		if (playername != null) {
			PlayerVariable pvar = playervariables.get(playername.toLowerCase());
			if (pvar != null) {
				pvar.get();
			}
		}
		return this.getDefault();
	}

	@Override
	public void set(String value) {
		if (value == null) {
			value = "%" + this.name + "%";
		}
		// Is a change required?
		if (this.defaultvalue.equals(value) && this.playervariables.isEmpty()) {
			return;
		}

		// Fire the event, and update the text
		VariableChangeEvent event = new VariableChangeEvent(this, value, null, VariableChangeType.GLOBAL);
		if (!CommonUtil.callEvent(event).isCancelled()) {
			this.defaultvalue = event.getNewValue();
			this.defaultticker.reset(this.defaultvalue);
			this.playervariables.clear();
			this.setSigns(this.defaultvalue, this.defaultticker.hasWrapAround(), null);
		}
	}

	/**
	 * Sets the default value for all (new) players that don't have a specific value set
	 * 
	 * @param value to set to
	 */
	public void setDefault(String value) {
		if (value == null) value = "%" + this.name + "%";
		VariableChangeEvent event = new VariableChangeEvent(this, value, null, VariableChangeType.DEFAULT);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			this.defaultvalue = event.getNewValue();
			this.defaultticker.reset(this.defaultvalue);
			this.updateAll();
		}
	}

	/**
	 * Gets all the individual player variables used by this Variable
	 * 
	 * @return Player variables
	 */
	public Collection<PlayerVariable> forAll() {
		return playervariables.values();
	}

	/**
	 * Gets a variable specific for a single player
	 * 
	 * @param player
	 * @return Player-specific variable
	 */
	public PlayerVariable forPlayer(Player player) {
		return this.forPlayer(player.getName());
	}

	/**
	 * Gets a variable specific for a single player
	 * 
	 * @param playername
	 * @return Player-specific variable
	 */
	public PlayerVariable forPlayer(String playername) {
		PlayerVariable pvar = playervariables.get(playername.toLowerCase());
		if (pvar == null) {
			pvar = new PlayerVariable(playername, this);
			playervariables.put(playername.toLowerCase(), pvar);
		}
		return pvar;
	}

	/**
	 * Gets a variable specific for a group of players
	 * 
	 * @param player
	 * @return Player group variable
	 */
	public GroupVariable forGroup(Player... players) {
		String[] playernames = new String[players.length];
		for (int i = 0; i < players.length; i++) {
			playernames[i] = players[i].getName();
		}
		return this.forGroup(playernames);
	}

	/**
	 * Gets a variable specific for a group of players
	 * 
	 * @param playernames
	 * @return Player group variable
	 */
	public GroupVariable forGroup(String... playernames) {
		PlayerVariable[] vars = new PlayerVariable[playernames.length];
	    for (int i = 0; i < vars.length; i++) {
	    	vars[i] = forPlayer(playernames[i]);
	    }
		return new GroupVariable(vars, this);
	}

	/**
	 * Updates a single sign
	 * 
	 * @param sign to update
	 */
	public void update(LinkedSign sign) {
    	sign.setText(this.defaultticker.current(), this.defaultticker.hasWrapAround());
    	for (PlayerVariable var : forAll()) {
    		sign.setText(var.getTicker().current(), var.getTicker().hasWrapAround(), var.getPlayer());
    	}
	}

	/**
	 * Updates a single sign
	 * 
	 * @param signBlock where this variable is displayed
	 */
	public void update(Block signBlock) {
		if (signBlock != null) {
		    for (LinkedSign sign : getSigns()) {
		        if (BlockUtil.equals(signBlock, sign.getStartBlock())) {
		        	update(sign);
		        }
		    }
		}
	}

	@Override
	public void updateAll() {
		for (LinkedSign sign : getSigns()) {
			update(sign);
		}
	}

	void setSigns(String value, boolean wrapAround, String[] playernames) {
		for (LinkedSign sign : getSigns()) {
			sign.setText(value, wrapAround, playernames);
		}
	}

	void updateTickers() {
		// Update
		boolean changed = false;
		changed |= this.defaultticker.update();
		for (PlayerVariable pvar : this.forAll()) {
			changed |= pvar.ticker.update();
		}
		if (changed) this.updateAll();
		// Reset
		this.defaultticker.checked.clear();
		for (PlayerVariable pvar : this.forAll()) {
			pvar.ticker.checked.clear();
		}
	}

	/**
	 * Updates the sign block order of all signs that display this Variable
	 */
	public void updateSignOrder() {
		for (LinkedSign sign : getSigns()) {
			sign.updateSignOrder();
		}
	}

	/**
	 * Updates the sign block order of all signs near a block that displays this Variable
	 * 
	 * @param near block
	 */
	public void updateSignOrder(Block near) {
		for (LinkedSign sign : this.boundTo) {
			if (!sign.location.world.equals(near.getWorld().getName())) {
				continue;
			}
			ArrayList<VirtualSign> signs = sign.getSigns();
			if (!LogicUtil.nullOrEmpty(signs)) {
				for (VirtualSign vsign : signs) {
					if (Math.abs(vsign.getX() - near.getX()) >= 2) continue;
					if (Math.abs(vsign.getY() - near.getY()) >= 2) continue;
					if (Math.abs(vsign.getZ() - near.getZ()) >= 2) continue;
					sign.updateSignOrder();
					this.update(sign);
					break;
				}
			}
		}
	}

	/**
	 * Updates the sign block order of all signs on a world that display this Variable
	 * 
	 * @param world
	 */
	public void updateSignOrder(World world) {
		for (LinkedSign sign : getSigns()) {
			if (sign.location.world.equals(world.getName())) {
				sign.updateSignOrder();
			}
		}
	}

	/**
	 * Gets all the signs on which this Variable is displayed
	 * 
	 * @return signs
	 */
	public LinkedSign[] getSigns() {
		return this.boundTo.toArray(new LinkedSign[0]);
	}

	/**
	 * Gets all the signs on a block on which this Variable is displayed
	 * 
	 * @param onBlock which it is displayed
	 * @return signs
	 */
	public LinkedSign[] getSigns(Block onBlock) {
		if (onBlock == null || boundTo.isEmpty()) {
			return new LinkedSign[0];
		}
		ArrayList<LinkedSign> signs = new ArrayList<LinkedSign>(boundTo.size());
		for (LinkedSign sign : boundTo) {
			Block block = sign.getStartBlock();
			if (block != null && block.equals(onBlock)) {
				signs.add(sign);
			}
		}
		return signs.toArray(new LinkedSign[0]);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public boolean addLocation(String worldname, int x, int y, int z, int lineAt, SignDirection direction) {
		return addLocation(new LinkedSign(worldname, x, y, z, lineAt, direction));
	}

	public boolean addLocation(Block signblock, int lineAt) {
		return addLocation(new LinkedSign(signblock, lineAt));
	}

	public boolean addLocation(LinkedSign sign) {
		//Not already added?
		for (LinkedSign ls : boundTo) {
			if (ls == sign) {
				return false;
			}
			if (ls.location.equals(sign.location)) {
				if (ls.line == sign.line) {
					this.removeLocation(ls);
					break;
				}
			}
		}

		SignAddEvent event = new SignAddEvent(this, sign);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			boundTo.add(sign);
			update(sign);
			return true;
		}
		return false;
	}

	public boolean removeLocation(Block signblock, int lineAt) {
		boolean rem = false;
		Iterator<LinkedSign> iter = this.boundTo.iterator();
		while (iter.hasNext()) {
			LinkedSign sign = iter.next();
			if (BlockUtil.equals(sign.getStartBlock(), signblock)) {
				if (sign.line == lineAt || lineAt == -1) {
					if (removeLocation(sign, false)) {
						iter.remove();
						rem = true;
					}
				}
			}
		}
		return rem;
	}

	public boolean removeLocation(Block signblock) {
		return this.removeLocation(signblock, -1);
	}

	public boolean removeLocation(LinkedSign sign) {
		return this.removeLocation(sign, true);
	}

	private boolean removeLocation(LinkedSign sign, boolean removeBoundTo) {
		SignRemoveEvent event = new SignRemoveEvent(this, sign);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!removeBoundTo || boundTo.remove(sign)) {
			ArrayList<VirtualSign> signs = sign.getSigns(false);
			if (signs != null) {
				for (VirtualSign vsign : signs) {
					vsign.setLine(sign.line, vsign.getRealLine(sign.line));
				}
			}
			return true;
		}
		return false;
	}

	public boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Block at) {
		return find(signs, variables, at.getLocation());
	}

	public boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Location at) {
		boolean found = false;
		for (LinkedSign sign : boundTo) {
			if (sign.location.x == at.getBlockX() && sign.location.y == at.getBlockY() && sign.location.z == at.getBlockZ()) {
				if (sign.location.world == at.getWorld().getName()) {
					found = true;
					if (signs != null) signs.add(sign);
					if (variables != null) variables.add(this);
				}
			}
		}
		return found;
	}

	/**
	 * Returns this same Variable instance, there is no use to call this method
	 */
	@Override
	@Deprecated
	public Variable getVariable() {
		return this;
	}

	/**
	 * Returns the default value, to prevent confusion, this method is deprecated
	 */
	@Override
	@Deprecated
	public String get() {
		return this.getDefault();
	}
}

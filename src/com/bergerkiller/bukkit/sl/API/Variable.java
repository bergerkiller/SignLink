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
import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variable {
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
	
	public String getName() {
		return this.name;
	}
	
	public void clear() {
		this.playervariables.clear();
		this.set("%" + this.name + "%");
		this.defaultticker = new Ticker(this.defaultvalue);
	}
	
	public Ticker getDefaultTicker() {
		return this.defaultticker;
	}
	public Ticker getTicker() {
		for (PlayerVariable pvar : this.forAll()) {
			pvar.ticker = this.defaultticker;
		}
		return this.defaultticker;
	}
	
	public String getDefault() {
		return this.defaultvalue;
	}
	public String get(String playername) {
		if (playername == null) return this.getDefault();
		PlayerVariable pvar = playervariables.get(playername.toLowerCase());
		if (pvar != null) return pvar.get();
		return this.getDefault();
	}
	public void set(String value) {
		if (value == null) value = "%" + this.name + "%";
		//is a change required?
		if (this.defaultvalue.equals(value)) {
			if (this.playervariables.isEmpty()) {
				return;
			}
		}
		
		VariableChangeEvent event = new VariableChangeEvent(this, value, null, VariableChangeType.GLOBAL);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			this.defaultvalue = event.getNewValue();
			this.defaultticker.reset(this.defaultvalue);
			this.playervariables.clear();
			this.setSigns(this.defaultvalue, null);
		}
	}
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
	
	public Collection<PlayerVariable> forAll() {
		return playervariables.values();
	}
	public PlayerVariable forPlayer(Player player) {
		return this.forPlayer(player.getName());
	}
	public PlayerVariable forPlayer(String playername) {
		PlayerVariable pvar = playervariables.get(playername.toLowerCase());
		if (pvar == null) {
			pvar = new PlayerVariable(playername, this);
			playervariables.put(playername.toLowerCase(), pvar);
		}
		return pvar;
	}
	public GroupVariable forGroup(Player... players) {
		String[] playernames = new String[players.length];
		for (int i = 0; i < players.length; i++) playernames[i] = players[i].getName();
		return this.forGroup(playernames);
	}
	public GroupVariable forGroup(String... playernames) {
		PlayerVariable[] vars = new PlayerVariable[playernames.length];
	    for (int i = 0; i < vars.length; i++) {
	    	vars[i] = forPlayer(playernames[i]);
	    }
		return new GroupVariable(vars, this);
	}
	
	public boolean isUsedByPlugin() {
		return Variables.isUsedByPlugin(this.name);
	}
		
	public void update(LinkedSign sign) {
    	sign.setText(this.defaultticker.current());
    	for (PlayerVariable var : forAll()) {
    		sign.setText(var.getTicker().current(), var.getPlayer());
    	}
	}
	public void update(Block on) {
		if (on == null) return;
	    for (LinkedSign sign : getSigns()) {
	        if (BlockUtil.equals(on, sign.getStartBlock())) {
	        	update(sign);
	        }
	    }
	}
	public void updateAll() {
		for (LinkedSign sign : getSigns()) {
			update(sign);
		}
	}
		
	void setSigns(String value, String[] playernames) {
		for (LinkedSign sign : getSigns()) {
			sign.setText(value, playernames);
		}
	}
	
	void updateTickers() {
		//update
		boolean changed = false;
		changed |= this.defaultticker.update();
		for (PlayerVariable pvar : this.forAll()) {
			changed |= pvar.ticker.update();
		}
		if (changed) this.updateAll();
		//reset
		this.defaultticker.checked = false;
		for (PlayerVariable pvar : this.forAll()) {
			pvar.ticker.checked = false;
		}
	}
	
	public void updateSignOrder() {
		for (LinkedSign sign : getSigns()) {
			sign.updateSignOrder();
		}
	}
	public void updateSignOrder(Block near) {
		for (LinkedSign sign : this.boundTo) {
			if (!sign.worldname.equals(near.getWorld().getName())) continue;
			ArrayList<VirtualSign> signs = sign.getSigns();
			if (signs == null) continue;
			if (signs.size() == 0) continue;
			for (VirtualSign vsign : signs) {
				if (vsign.getX() - near.getX() < -2) continue;
				if (vsign.getX() - near.getX() > 2) continue;
				if (vsign.getZ() - near.getZ() < -2) continue;
				if (vsign.getZ() - near.getZ() > 2) continue;
				if (vsign.getY() - near.getY() < -2) continue;
				if (vsign.getY() - near.getY() > 2) continue;
				sign.updateSignOrder();
				this.update(sign);
				break;
			}
		}
	}
	public void updateSignOrder(World world) {
		for (LinkedSign sign : getSigns()) {
			if (sign.worldname.equals(world.getName())) {
				sign.updateSignOrder();
			}
		}
	}
	
	public LinkedSign[] getSigns() {
		return this.boundTo.toArray(new LinkedSign[0]);
	}	
	public LinkedSign[] getSigns(Block on) {
		ArrayList<LinkedSign> signs = new ArrayList<LinkedSign>();
		if (on != null) {
			for (LinkedSign sign : boundTo) {
				Location l = sign.getStartLocation();
				if (l != null && l.equals(on.getLocation())) {
					signs.add(sign);
				}
			}
		}
		return signs.toArray(new LinkedSign[0]);
	}
	
	public String toString() {
		return this.name;
	}
	
	public boolean addLocation(String worldname, int x, int y, int z, int lineAt, LinkedSign.Direction direction) {
		return addLocation(new LinkedSign(worldname, x, y, z, lineAt, direction));
	}
	public boolean addLocation(Block signblock, int lineAt) {
		return addLocation(new LinkedSign(signblock, lineAt));
	}
	public boolean addLocation(LinkedSign sign) {
		//Not already added?
		for (LinkedSign ls : boundTo) {
			if (ls == sign) return false;
			if (ls.x == sign.x && ls.y == sign.y && ls.z == sign.z) {
				if (ls.worldname.equalsIgnoreCase(sign.worldname)) {
					if (ls.line == sign.line) {
						this.removeLocation(ls);
						break;
					}
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
			if (sign.x == at.getBlockX() && sign.y == at.getBlockY() && sign.z == at.getBlockZ()) {
				if (sign.worldname == at.getWorld().getName()) {
					found = true;
					if (signs != null) signs.add(sign);
					if (variables != null) variables.add(this);
				}
			}
		}
		return found;
	}
}

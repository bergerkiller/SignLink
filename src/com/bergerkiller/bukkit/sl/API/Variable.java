package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variable {
	private String defaultvalue;
	private String name;
	private ArrayList<LinkedSign> boundTo = new ArrayList<LinkedSign>();
	private HashMap<String, String> playervalues = new HashMap<String, String>();
	
	public Variable(String value, String name) {
		this.defaultvalue = value;
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}
	public String get() {
		return this.defaultvalue;
	}
	public String get(String player) {
		String val = playervalues.get(player);
		if (val == null) return this.get();
		return val;
	}
	
	public void set(String value) {
		VariableChangeEvent event = new VariableChangeEvent(this, value, null);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			this.defaultvalue = event.getNewValue();
			for (LinkedSign sign : getSigns()) {
				sign.setText(this.defaultvalue);
			}
		}
	}
	public void set(String value, String[] players) {
		VariableChangeEvent event = new VariableChangeEvent(this, value, players);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			for (String player : players) {
				playervalues.put(player, value);
			}
			for (LinkedSign sign : getSigns()) {
				sign.setText(value, players);
			}
		}
	}
	public void resetValue(String... players) {
		set(defaultvalue, players);
	}
	public void updateAll() {
		set(defaultvalue);
		String[] tmparr = new String[0];
		for (Map.Entry<String, String> entry : playervalues.entrySet()) {
			tmparr[0] = entry.getKey();
			set(entry.getValue(), tmparr);
		}
	}
	
	public PlayerVariable forPlayers(String... playernames) {
		return new PlayerVariable(this, playernames);
	}
	public PlayerVariable forPlayers(Player... players) {
		String[] names = new String[players.length];
		for (int i = 0; i < players.length; i++) {
			names[i] = players[i].getName();
		}
		return forPlayers(names);
	}
	
	public void updateSignOrder() {
		for (LinkedSign sign : getSigns()) {
			sign.updateSignOrder();
		}
	}
	public LinkedSign[] getSigns() {
		return this.boundTo.toArray(new LinkedSign[0]);
	}	

	public boolean addLocation(String worldname, int x, int y, int z, int lineAt, LinkedSign.Direction direction) {
		return addLocation(new LinkedSign(worldname, x, y, z, lineAt, direction));
	}
	public boolean addLocation(Block signblock, int lineAt) {
		return addLocation(new LinkedSign(signblock, lineAt));
	}
	public boolean addLocation(LinkedSign sign) {
		SignAddEvent event = new SignAddEvent(this, sign);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			boundTo.add(sign);
			sign.update(true);
			return true;
		} else {
			return false;
		}
	}
	public boolean removeLocation(Block signblock) {
		for (LinkedSign sign : boundTo) {
			if (signblock.getLocation().equals(sign.getStartLocation())) {
				return removeLocation(sign);
			}
		}
		return false;
	}
	public boolean removeLocation(LinkedSign sign) {
		SignRemoveEvent event = new SignRemoveEvent(this, sign);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (boundTo.remove(sign)) {
			for (VirtualSign vsign : sign.getSigns()) {
				vsign.setLine(sign.line, vsign.getRealLine(sign.line));
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

package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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
		this.set(value, true);
	}
	private void set(String value, boolean clearplayers) {
		VariableChangeEvent event = new VariableChangeEvent(this, value, null);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			if (clearplayers) playervalues.clear();
			this.defaultvalue = event.getNewValue();
			for (LinkedSign sign : getSigns()) {
				sign.setText(this.defaultvalue);
			}
		}
	}
	public void set(String value, List<String> players) {
		if (players == null || players.size() == 0) {
			this.set(value);
		} else {
			this.set(value, players.toArray(new String[0]));
		}
	}
	public void set(String value, String[] players) {
		if (players.length == 0) {
			set(value);
		} else {
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
	}
	
	public void resetValue(String... players) {
		set(defaultvalue, players);
	}
	
	public void update(LinkedSign sign) {
    	sign.setText(this.defaultvalue);
    	for (Map.Entry<String, String> entry : playervalues.entrySet()) {
    		sign.setText(entry.getValue(), entry.getKey());
    	}
	}
	public void update(Block on) {
		if (on == null) return;
	    for (LinkedSign sign : getSigns()) {
	        Location l = sign.getStartLocation();
	        if (on.getLocation().equals(l)) {
	        	update(sign);
	        }
	    }
	}
	public void updateAll() {
		for (LinkedSign sign : getSigns()) {
			update(sign);
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
	public void updateSignOrder(Block start) {
		for (LinkedSign sign : getSigns()) {
			Location loc = sign.getStartLocation();
			if (loc != null) {
				if (loc.equals(start.getLocation())) {
					sign.updateSignOrder();
				}
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

	public boolean addLocation(String worldname, int x, int y, int z, int lineAt, LinkedSign.Direction direction) {
		return addLocation(new LinkedSign(worldname, x, y, z, lineAt, direction));
	}
	public boolean addLocation(Block signblock, int lineAt) {
		return addLocation(new LinkedSign(signblock, lineAt));
	}
	public boolean addLocation(LinkedSign sign) {
		//Not already added?
		for (LinkedSign ls : boundTo) {
			if (ls.x == sign.x && ls.y == sign.y && ls.z == sign.z) {
				if (ls.worldname.equalsIgnoreCase(sign.worldname)) {
					if (ls.line == sign.line) {
						return false;
					}
				}
			}
		}
		
		SignAddEvent event = new SignAddEvent(this, sign);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			boundTo.add(sign);
			update(sign);
			sign.update(true);
			return true;
		}
		return false;
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
			ArrayList<VirtualSign> signs = sign.getSigns();
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

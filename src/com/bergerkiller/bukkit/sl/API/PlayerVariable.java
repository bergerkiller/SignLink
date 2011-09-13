package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class PlayerVariable {

	private ArrayList<String> players = new ArrayList<String>();
	private Variable variable;
	
	public PlayerVariable(Variable source,  String[] players) {
		for (String player : players) {
			this.players.add(player);
		}
		this.variable = source;
	}
	
	public String[] getPlayers() {
		return this.players.toArray(new String[0]);
	}
	public void addPlayer(String name) {
		this.players.add(name);
	}
	public void addPlayer(Player player) {
		addPlayer(player.getName());
	}
	public void removePlayer(String name) {
		this.players.remove(name);
	}
	public void removePlayer(Player player) {
		removePlayer(player.getName());
	}
	
	public String get(String playername) {
		return variable.get(playername);
	}
	public String[] get() {
		String[] values = new String[players.size()];
		for (int i = 0; i < 4; i++) {
			values[i] = get(players.get(i));
		}
		return values;
	}
	
	public void set(String value) {
		variable.set(value, getPlayers());
	}

	public Variable getVariable() {
		return this.variable;
	}
}

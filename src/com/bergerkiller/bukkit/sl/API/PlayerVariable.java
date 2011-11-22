package com.bergerkiller.bukkit.sl.API;

import org.bukkit.Bukkit;

public class PlayerVariable {

	private Variable variable;
	String value;
	private String playername;
	Ticker ticker;
	
	public PlayerVariable(String playername, Variable variable) {
		this(playername, variable, variable.getDefault());
	}
	public PlayerVariable(String playername, Variable variable, String value) {
		this.playername = playername;
		this.value = value;
		this.variable = variable;
		this.ticker = this.variable.getDefaultTicker();
	}
	
	public String get() {
		return this.value;
	}
	public String getPlayer() {
		return this.playername;
	}
			
	public void clear() {
		this.ticker = this.variable.getDefaultTicker();
		this.set("%" + this.variable.getName() + "%");
	}
	
	public boolean set(String value) {
		//is a change required?
		if (this.value.equals(value)) {
			return true;
		}
		VariableChangeEvent event = new VariableChangeEvent(this.variable, value, new PlayerVariable[] {this}, VariableChangeType.PLAYER);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			this.value = value;
			getTicker().reset(value);
			this.variable.setSigns(value, new String[] {this.playername});
			return true;
		} else {
			return false;
		}
	}

	public Variable getVariable() {
		return this.variable;
	}
	
	public boolean isTickerShared() {
		return this.ticker.isShared();
	}
	public Ticker getTicker() {
		if (this.isTickerShared()) {
			this.ticker = this.ticker.clone();
			this.ticker.players = new String[] {this.playername};
		}
		return this.ticker;
	}

	void setTicker(Ticker ticker) {
		this.ticker = ticker;
	}
}

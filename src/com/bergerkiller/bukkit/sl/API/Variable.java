package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.SignLink;
import com.bergerkiller.bukkit.sl.Task;

public class Variable {
	public Variable(String value, String name) {
		this.value = value;
		this.name = name;
	}
	
	private String value;
	private String name;
	
	public String getName() {
		return this.name;
	}
	public String getValue() {
		return this.value;
	}
	public void setValue(String value) {
		VariableChangeEvent event = new VariableChangeEvent(this, value);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			this.value = event.getNewValue();
			for (LinkedSign sign : getSigns()) {
				sign.setText(this.value);
			}
		}
	}
	
	public void updateSignOrder() {
		for (LinkedSign sign : getSigns()) {
			sign.updateSignOrder();
		}
	}
	
	public LinkedSign[] getSigns() {
		return this.boundTo.toArray(new LinkedSign[0]);
	}
	
	public void update() {
		for (LinkedSign sign : getSigns()) {
			sign.update(false);
		}
	}
	
	public void update(long delay) {
		Task t = new Task(SignLink.plugin, this) {
			public void run() {
				Variable var = (Variable) getArg(0);
				var.update();
			}
		};
		t.startDelayed(delay);
	}
	
	private ArrayList<LinkedSign> boundTo = new ArrayList<LinkedSign>();
	
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
		if (!event.isCancelled()) {
			return boundTo.remove(sign);
		} else {
			return false;
		}
	}
		
}

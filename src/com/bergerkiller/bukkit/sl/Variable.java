package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;

import org.bukkit.block.Block;

public class Variable {
	public Variable(String value) {
		this.value = value;
	}
	
	private String value;
	public String getValue() {
		return this.value;
	}
	public void setValue(String value) {
		if (!value.equals(this.value)) {
			this.value = value;
			for (LinkedSign sign : getSigns()) {
				sign.setText(value);
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
			sign.update();
		}
	}
	
	private ArrayList<LinkedSign> boundTo = new ArrayList<LinkedSign>();
	
	public void addLocation(String worldname, int x, int y, int z, int lineAt, LinkedSign.Direction direction) {
		boundTo.add(new LinkedSign(worldname, x, y, z, lineAt, direction));
	}
	public void addLocation(Block signblock, int lineAt) {
		boundTo.add(new LinkedSign(signblock, lineAt));
	}
	public boolean removeLocation(Block signblock) {
		for (LinkedSign sign : boundTo) {
			if (signblock.getLocation().equals(sign.getStartLocation())) {
				boundTo.remove(sign);
				return true;
			}
		}
		return false;
	}
		
}

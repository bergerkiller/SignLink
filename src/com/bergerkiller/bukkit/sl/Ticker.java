package com.bergerkiller.bukkit.sl;

public class Ticker {
	public Ticker(String text) {
		this.text = text;
	}
	private String text;
	
	public String next() {
		char c = this.text.charAt(0);
		this.text = this.text.substring(1) + c;
		if (c == '§') return next();
		return this.text;
	}

}

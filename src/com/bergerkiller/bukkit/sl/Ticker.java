package com.bergerkiller.bukkit.sl;

public class Ticker {
	public Ticker(String varname, String text, long interval, byte mode) {
		this.varname = varname;
		this.text = text;
		this.interval = interval;
		this.mode = mode;
		this.counter = 0;
		if (this.interval < 1) this.interval = 1;
	}
	public String varname;
	private String text;
	private long interval;
	private long counter;
	private byte mode;
		
	
 	private boolean countNext() {
		counter += 1;
		if (counter == interval) {
			counter = 0;
			return true;
		} else {
			return false;
		}
	}
 	
 	public String getNext() {
 		if (mode == 1) {
 			return previous();
 		} else if (mode == 2) {
 			return next();
 		} else {
 			return current();
 		}
 	}
	
	public String current() {
		return this.text;
	}
	public String previous() {
		if (!countNext()) return this.text;
		char c = this.text.charAt(this.text.length() - 1);
		this.text = c + this.text.substring(0, this.text.length() - 1);
		if (c == '§') return previous();
		return this.text;
	}
	public String next() {
		if (!countNext()) return this.text;
		char c = this.text.charAt(0);
		this.text = this.text.substring(1) + c;
		if (c == '§') return next();
		return this.text;
	}

}

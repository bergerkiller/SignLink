package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.List;

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
	public List<String> players;
	private int pauseindex = 0;
	private ArrayList<Pause> pauses = new ArrayList<Pause>();
	private Pause getNextPause() {
		if (pauses.size() == 0) return null;
		Pause p = pauses.get(pauseindex);

		//Play the next part as long possible
		if (p.currentdelay >= p.delay) {
			//Not playing anymore
			p.active = true;
			if (p.currentduration > p.duration) {
				//reset and go to next pause
				p.currentdelay = 0;
				p.currentduration = 0;
				pauseindex++;
				if (pauseindex > pauses.size() - 1) {
					pauseindex = 0;
				}
			} else {
				p.currentduration++;
			}
		} else {
			p.active = false;
			p.currentdelay++;
		}

		return p;
	}
	private boolean isPaused() {
		Pause p = getNextPause();
		return p != null && p.active;
	}
	
	public void addPause(int delay, int duration) {
		Pause p = new Pause();
		p.delay = delay;
		p.duration = duration;
		pauses.add(p);
	}
	
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
 		if (!countNext()) return current();
 		if (isPaused()) {
 			return current();
 		}
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
		char c = this.text.charAt(this.text.length() - 1);
		this.text = c + this.text.substring(0, this.text.length() - 1);
		if (c == '§') return previous();
		return this.text;
	}
	public String next() {
		char c = this.text.charAt(0);
		this.text = this.text.substring(1) + c;
		if (c == '§') return next();
		return this.text;
	}
	
	public class Pause {
		public int currentdelay = 0;
		public int delay;
		public int currentduration = 0;
		public int duration;
		public boolean active = false;
	}

}

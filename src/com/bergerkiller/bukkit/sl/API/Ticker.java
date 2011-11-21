package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.List;

import com.bergerkiller.bukkit.sl.Configuration;

public class Ticker {
	
	//These two are used to check if it is updated
	String[] players;
	boolean checked;
	
	private String value;	
	private ArrayList<Pause> pauses = new ArrayList<Pause>();
	private int pauseindex = 0;
	public long interval = 1;
	private long counter = 0;
	public TickMode mode = TickMode.NONE;
	
	public Ticker(String initialvalue) {
		this.value = initialvalue;
		this.players = null;
	}
	public Ticker(String initialvalue, String player) {
		this(initialvalue, new String[] {player});
	}
	public Ticker(String initialvalue, String[] players) {
		this.value = initialvalue;
		this.players = players;
	}
	
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
	public void clearPauses() {
		this.pauses.clear();
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
 	
 	public String next() {
 		if (!countNext()) return current();
 		if (isPaused()) {
 			return current();
 		}
 		if (mode == TickMode.LEFT) {
 			return this.left();
 		} else if (mode == TickMode.RIGHT) {
 			return this.right();
 		} else {
 			return this.current();
 		}
 	}
 	 	
 	boolean isShared() {
 		return this.players == null || this.players.length != 1;
 	}
 	
    void update(Variable var) {
    	if (this.checked) return;
    	this.checked = true;
 		if (!countNext()) return;
 		if (isPaused()) return;
 		String value;
 		if (mode == TickMode.LEFT) {
 		    value = this.left();
 		} else if (mode == TickMode.RIGHT) {
 			value = this.right();
 		} else {
 			return;
 		}
 		var.setSigns(value, true, this.players);
    }
 	
 	public void reset(String value) {
 		this.pauseindex = 0;
 		this.counter = 0;
 		this.value = value;
 		for (Pause p : this.pauses) {
 			p.currentdelay = 0;
 			p.currentduration = 0;
 			p.active = false;
 		}
 	}
 	
 	public void load(Configuration config, String root) {
 		String tickmode = config.getString(root + ".ticker", "NONE");
 		if (tickmode.equalsIgnoreCase("LEFT")) {
 			this.mode = TickMode.LEFT;
 		} else if (tickmode.equalsIgnoreCase("RIGHT")) {
 			this.mode = TickMode.RIGHT;
 		}
 		this.interval = config.getInt(root + ".tickerInterval", (int) this.interval);
 		if (config.isSet(root + ".tickerInterval")) {
 			List<Integer> delays = config.getListOf(root + ".pauseDelays");
 			List<Integer> durations = config.getListOf(root + ".pauseDurations");
 			if (delays.size() == durations.size()) {
 				for (int i = 0; i < delays.size(); i++) {
 					int delay = delays.get(i);
 					int duration = durations.get(i);
 					this.addPause(delay, duration);
 				}
 			}
 		}
 	}
 	public void save(Configuration config, String root) {
 		if (this.mode != TickMode.NONE) {
 			config.set(root + ".ticker", this.mode.toString());
 		} else {
 			config.set(root + ".ticker", null);
 		}
 		if (this.interval != 1) {
 			config.set(root + ".tickerInterval", (int) this.interval);
 		} else {
 			config.set(root + ".tickerInterval", null);
 		}
 		List<Integer> delays = null;
 		List<Integer> durations = null;
 		if (this.pauses.size() > 0) {
 			delays = new ArrayList<Integer>();
 			durations = new ArrayList<Integer>();
 	 	    for (Pause p : this.pauses) {
 	 	    	delays.add(p.delay);
 	 	    	durations.add(p.duration);
 	 	    }
 		}
 	    config.set(root + ".pauseDelays", delays);
 	    config.set(root + ".pauseDurations", durations);
 	}
	
	public String current() {
		return this.value;
	}
	public String left() {
		char c = this.value.charAt(this.value.length() - 1);
		this.value = c + this.value.substring(0, this.value.length() - 1);
		if (c == '§') return left();
		return this.value;
	}
	public String right() {
		char c = this.value.charAt(0);
		this.value = this.value.substring(1) + c;
		if (c == '§') return right();
		return this.value;
	}
	
	
	private class Pause {
		public int currentdelay = 0;
		public int delay;
		public int currentduration = 0;
		public int duration;
		public boolean active = false;
		public Pause clone() {
			Pause p = new Pause();
			p.currentdelay = this.currentdelay;
			p.delay = this.delay;
			p.currentduration = this.duration;
			p.duration = this.duration;
			p.active = this.active;
			return p;
		}
	}

	public Ticker clone() {
		Ticker t = new Ticker(this.value, this.players);
		t.pauseindex = this.pauseindex;
		t.interval = this.interval;
		t.counter = this.counter;
		t.mode = this.mode;
	    for (Pause p : this.pauses) {
	    	t.pauses.add(p.clone());
	    }
	    return t;
	}
}

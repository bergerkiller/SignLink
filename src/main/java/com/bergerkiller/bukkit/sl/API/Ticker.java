package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.utils.StringUtil;

/**
 * Ticks text on a per-tick basis to allow textual animations
 */
public class Ticker {
	//These two are used to check if it is updated
	protected String[] players = null;
	protected boolean checked;
	private TickedChar[] valueElements;
	private String value = "";
	private ArrayList<Pause> pauses = new ArrayList<Pause>();
	private int pauseindex = 0;
	public long interval = 1;
	private long counter = 0;
	public TickMode mode = TickMode.NONE;

	public Ticker(String initialvalue, String player) {
		this(initialvalue, new String[] {player});
	}
	public Ticker(String initialvalue, String[] players) {
		this(initialvalue);
		this.players = players;
	}
	public Ticker(String initialvalue) {
		this.value = initialvalue;
	}

	/**
	 * Represents a single ticked character in the ticker text
	 */
	private static class TickedChar {
		public final char character;
		public final ChatColor color;
		public TickedChar(char character, ChatColor color) {
			this.character = character;
			this.color = color;
		}

		public static String getText(TickedChar[] characters) {
			StringBuilder text = new StringBuilder(characters.length + 20);
			ChatColor currentColor = null;
			for (TickedChar c : characters) {
				if (c.color != currentColor && c.color != null) {
					currentColor = c.color;
					text.append(c.color);
				}
				text.append(c.character);
			}
			return text.toString();
		}

		public static TickedChar[] getChars(String text) {
			ChatColor currentColor = null;
			ArrayList<TickedChar> rval = new ArrayList<TickedChar>(text.length());
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				// Update coloring
				if (c == StringUtil.CHAT_STYLE_CHAR) {
					if (i < text.length() - 1) {
						i++;
						currentColor = StringUtil.getColor(text.charAt(i), currentColor);
					}
					continue;
				}
				// New character
				rval.add(new TickedChar(c, currentColor));
			}
			return rval.toArray(new TickedChar[0]);
		}
	}

	private Pause getNextPause() {
		if (pauses.isEmpty()) {
			return null;
		}
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

	/**
	 * Adds a pause to this ticker
	 * 
	 * @param delay in ticks before the pause occurs
	 * @param duration in ticks of the pause
	 */
	public void addPause(int delay, int duration) {
		Pause p = new Pause();
		p.delay = delay;
		p.duration = duration;
		this.pauses.add(p);
	}

	/**
	 * Clears all the ticker pauses
	 */
	public void clearPauses() {
		this.pauses.clear();
	}

 	private boolean countNext() {
		this.counter++;
		if (this.counter >= this.interval) {
			this.counter = 0;
			return true;
		} else {
			return false;
		}
	}
 
 	/**
 	 * Gets the next ticker value depending on the settings
 	 * 
 	 * @return Next ticker value
 	 */
 	public String next() {
 		if (!countNext()) return current();
 		if (!isPaused()) {
 	 		switch (this.mode) {
 	 	 		case LEFT : this.left(); break;
 	 	 		case RIGHT : this.right(); break;
 	 	 	}
 		}
 		return this.current();
 	}

 	boolean isShared() {
 		return this.players == null || this.players.length != 1;
 	}

	boolean update() {
    	if (this.checked) return false;
    	this.checked = true;
 		if (!this.countNext()) return false;
 		if (this.isPaused()) return false;
 		switch (this.mode) {
 		case LEFT : this.left(); return true;
 		case RIGHT : this.right(); return true;
 		default : return false;
 		}
    }
 
	/**
	 * Resets the settings of this ticker
	 * 
	 * @param value to set the text to
	 */
 	public void reset(String value) {
 		this.pauseindex = 0;
 		this.counter = 0;
 		this.valueElements = TickedChar.getChars(value);
 		this.value = value;
 		for (Pause p : this.pauses) {
 			p.currentdelay = 0;
 			p.currentduration = 0;
 			p.active = false;
 		}
 	}
 
 	/**
 	 * Loads ticker information
 	 * 
 	 * @param node to load from
 	 */
 	public void load(ConfigurationNode node) {
 		String tickmode = node.get("ticker", "NONE");
 		if (tickmode.equalsIgnoreCase("LEFT")) {
 			this.mode = TickMode.LEFT;
 		} else if (tickmode.equalsIgnoreCase("RIGHT")) {
 			this.mode = TickMode.RIGHT;
 		}
 		this.interval = node.get("tickerInterval", (int) this.interval);
 		if (node.contains("tickerInterval")) {
 			List<Integer> delays = node.getList("pauseDelays", Integer.class);
 			List<Integer> durations = node.getList("pauseDurations", Integer.class);
 			if (delays.size() == durations.size()) {
 				for (int i = 0; i < delays.size(); i++) {
 					int delay = delays.get(i);
 					int duration = durations.get(i);
 					this.addPause(delay, duration);
 				}
 			}
 		}
 	}

 	/**
 	 * Saves this ticker
 	 * 
 	 * @param node to save in
 	 */
 	public void save(ConfigurationNode node) {
 		if (this.mode != TickMode.NONE) {
 			node.set("ticker", this.mode.toString());
 		} else {
 			node.set("ticker", null);
 		}
 		if (this.interval != 1) {
 			node.set("tickerInterval", (int) this.interval);
 		} else {
 			node.set("tickerInterval", null);
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
 	    node.set("pauseDelays", delays);
 	    node.set("pauseDurations", durations);
 	}
 
 	/**
 	 * Gets the current value of this ticker
 	 * 
 	 * @return Current ticker value
 	 */
	public String current() {
		return this.value;
	}

	/**
	 * Gets the next value when ticking the text to the left
	 * 
	 * @return Next value
	 */
	public String left() {
		// Translate elements one to the left
		if (this.valueElements.length >= 2) {
			TickedChar first = this.valueElements[0];
			for (int i = 1; i < this.valueElements.length; i++) {
				this.valueElements[i - 1] = this.valueElements[i];
			}
			this.valueElements[this.valueElements.length - 1] = first;
			// Update
			this.value = TickedChar.getText(this.valueElements);
		}
		return this.value;
	}

	/**
	 * Gets the next value when ticking the text to the right
	 * 
	 * @return Next value
	 */
	public String right() {
		// Translate elements one to the right
		if (this.valueElements.length >= 2) {
			TickedChar last = this.valueElements[this.valueElements.length - 1];
			for (int i = this.valueElements.length - 1; i >= 1; i--) {
				this.valueElements[i] = this.valueElements[i - 1];
			}
			this.valueElements[0] = last;
			// Update
			this.value = TickedChar.getText(this.valueElements);
		}
		return this.value;
	}

	private class Pause {
		public int currentdelay = 0;
		public int delay;
		public int currentduration = 0;
		public int duration;
		public boolean active = false;
		@Override
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

	@Override
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

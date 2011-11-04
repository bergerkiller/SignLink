package com.bergerkiller.bukkit.sl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;


import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.bergerkiller.bukkit.sl.LinkedSign.Direction;

public class SignLink extends JavaPlugin {
	public static SignLink plugin;
	
	public static boolean updateSigns = false;
	public static boolean allowSignEdit = true;

	private SLBlockListener blockListener = new SLBlockListener();
	private SLLowBlockListener blockListenerLow = new SLLowBlockListener();
	private SLPlayerListener playerListener = new SLPlayerListener();
	
	public void loadValues() {
		//Load tickers
		Configuration values = new Configuration(this.getDataFolder() + File.separator + "values.yml");
		if (!values.exists()) {			
			values.set("test.ticker", "LEFT");
			values.set("test.tickerInterval", 3);
			values.set("test.value", "This is a test message being ticked from right to left. ");
			values.set("sign.ticker", "NONE");
			values.set("sign.value", "This is a regular message you can set and is updated only once.");
			values.save();
		}
		ArrayList<Ticker> tickers = new ArrayList<Ticker>();
		values.load();
		for (String key : values.getKeys(false)) {
			String tickmode = values.getString(key + ".ticker", "NONE");
			byte mode = 0;
			if (tickmode.equalsIgnoreCase("LEFT")) {
				mode = 2;
			} else if (tickmode.equalsIgnoreCase("RIGHT")) {
				mode = 1;
			}
			int interval = values.getInt(key + ".tickerInterval", 1);
			String message = values.getString(key + ".value", "None");
			List<Integer> delays = values.getListOf(key + ".pauseDelays");
			List<Integer> durations = values.getListOf(key + ".pauseDurations");
			Ticker t = new Ticker(key, message, interval, mode);
			t.players = values.getListOf(key + ".forPlayers");
			if (delays.size() == durations.size()) {
				for (int i = 0; i < delays.size(); i++) {
					int delay = delays.get(i);
					int duration = durations.get(i);
					t.addPause(delay, duration);
				}
			}
			tickers.add(t);
		}
		if (tickertask != null && tickertask.isRunning()) {
			tickertask.stop();
		}
		
		//Start tickers
		tickertask = new Task(this, tickers, new ArrayList<Variable>()) {
			@SuppressWarnings("unchecked")
			public void run() {
				ArrayList<Ticker> tickers = (ArrayList<Ticker>) getArg(0);
				if (tickers.size() == 0) return;
				ArrayList<Variable> vars = (ArrayList<Variable>) getArg(1);
				if (vars.size() == 0) {
					for (Ticker ticker : tickers) {
						vars.add(Variables.get(ticker.varname));
					}
				}
				//Actual stuff here
				for (int i = 0; i < vars.size(); i++) {
					vars.get(i).set(tickers.get(i).getNext(), tickers.get(i).players);
				}
			}
		};
		tickertask.startRepeating(1);
	}
	
	public void updatePlayerName(Player p) {
		Variables.get("playername").forPlayers(p).set(p.getName());
	}
	
	public void onEnable() {
		plugin = this;
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListenerLow, Priority.Lowest, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);

		getCommand("togglesignupdate").setExecutor(this);
		getCommand("reloadsignlink").setExecutor(this);
		
		//Load sign locations from file
		Variable currvar = null;
		for (String textline : SafeReader.readAll(this.getDataFolder() + File.separator + "linkedsigns.txt")) {
			if (textline.startsWith("#")) continue;
			try {
				if (currvar != null && textline.startsWith("\t")) {
				    int namestart = textline.indexOf("\"");
				    if (namestart != -1) {
				    	int nameend = textline.indexOf("\"", namestart + 1);
				    	if (namestart != nameend && nameend != -1) {
				    		String worldname = textline.substring(namestart + 1, nameend);
				    		String[] args = textline.substring(nameend + 1).trim().split(" ");
				    		if (args.length == 5) {
				    			int x = Integer.parseInt(args[0]);
				    			int y = Integer.parseInt(args[1]);
				    			int z = Integer.parseInt(args[2]);
				    			int line = Integer.parseInt(args[3]);
				    			Direction direction = Direction.NONE;
				    			if (args[4].equalsIgnoreCase("LEFT")) {
				    				direction = Direction.LEFT;
				    			} else if (args[4].equalsIgnoreCase("RIGHT")) {
				    				direction = Direction.RIGHT;
				    			}
				    			if (line >= 0 && line < 4) {
					    			currvar.addLocation(worldname, x, y, z, line, direction);
				    			} else {
				    				Util.log(Level.WARNING, "Failed to parse line: " + textline);
				    				Util.log(Level.WARNING, "Line index out of range: " + line);
				    			}
				    		}
				    	}
				    }
				} else {
					currvar = Variables.get(textline);
				}
			} catch (Exception ex) {
				Util.log(Level.WARNING, "Failed to parse line: " + textline);
				ex.printStackTrace();
			}
		}
		
		//General %time% and %date% update thread
		timetask = new Task(this) {
			public void run() {
				Variables.get("time").set(Util.now("H:mm:ss"));
				Variables.get("date").set(Util.now("yyyy.MM.dd"));
			}
		};
		timetask.startRepeating(10, 5, false);
		
		loadValues();
		
		//Start updating
		updatetask = new Task(this) {
			public void run() {
				VirtualSign.updateAll();
			}
		};
		updatetask.startRepeating(1L);
		
		updateSigns = true;
		
		for (Player p : getServer().getOnlinePlayers()) {
			updatePlayerName(p);
		}
		
		Util.log(Level.INFO, " version " + this.getDescription().getVersion() + " is enabled!");
	}
	
	private Task updatetask;
	private Task timetask;
	private Task tickertask;
		
	public void onDisable() {
		Task.stop(timetask);
		Task.stop(tickertask);
		Task.stop(updatetask);
		
		//Save sign locations to file
		SafeWriter writer = new SafeWriter(this.getDataFolder() + File.separator + "linkedsigns.txt");
		writer.writeLine("# Stores the variables displayed by various signs. Format: ");
		writer.writeLine("# variablename");
		writer.writeLine("# \t\"worldname\" x y z line direction");
		writer.writeLine("# Where the direction can be NONE, LEFT and RIGHT. (line overlap)");
		for (String varname : Variables.getNames()) {
			writer.writeLine(varname);
			for (LinkedSign sign : Variables.get(varname).getSigns()) {
				String textline = "\t\"" + sign.worldname + "\" ";
				textline += sign.x + " " + sign.y + " " + sign.z + " " + sign.line;
				if (sign.direction == Direction.LEFT) {
					textline += " LEFT";
				} else if (sign.direction == Direction.RIGHT) {
					textline += " RIGHT";
				} else {
					textline += " NONE";
				}
				writer.writeLine(textline);
			}
		}
		writer.close();
		
		Variables.deinit();
		VirtualSign.deinit();
		Util.log(Level.INFO, " is disabled!");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (cmdLabel.equalsIgnoreCase("togglesignupdate")) {
			if (!(sender instanceof Player) || ((Player) sender).hasPermission("signlink.toggleupdate")) {
				updateSigns = !updateSigns;
				if (updateSigns) {
					sender.sendMessage("Signs are now being updated!");
				} else {
					sender.sendMessage("Signs are now no longer being updated!");
				}
			}
		} else if (cmdLabel.equalsIgnoreCase("reloadsignlink")) {
			if (!(sender instanceof Player) || ((Player) sender).hasPermission("signlink.reload")) {
				loadValues();
				sender.sendMessage("SignLink reloaded the Variable values");
			}
		}
 		return true;
	}
	
}

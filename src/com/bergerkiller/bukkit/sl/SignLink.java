package com.bergerkiller.bukkit.sl;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.sl.API.GroupVariable;
import com.bergerkiller.bukkit.sl.API.PlayerVariable;
import com.bergerkiller.bukkit.sl.API.TickMode;
import com.bergerkiller.bukkit.sl.API.Ticker;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.bergerkiller.bukkit.sl.LinkedSign.Direction;

public class SignLink extends JavaPlugin {
	public static SignLink plugin;
	
	public static boolean updateSigns = false;
	public static boolean allowSignEdit = true;
	public static boolean usePermissions = false;
	private SimpleDateFormat dateFormat;
	private SimpleDateFormat timeFormat;
	
	private static Logger logger = Logger.getLogger("Minecraft");
	public static void log(Level level, String message) {
		logger.log(level, "[SignLink] " + message);
	}
	
	public void loadValues() {
		Configuration values = new Configuration(this.getDataFolder() + File.separator + "values.yml");
		if (!values.exists()) {			
			values.set("test.ticker", "LEFT");
			values.set("test.tickerInterval", 3);
			values.set("test.value", "This is a test message being ticked from right to left. ");
			values.set("sign.ticker", "NONE");
			values.set("sign.value", "This is a regular message you can set and is updated only once.");
			values.save();
		}
		values.load();
		for (String key : values.getKeys(false)) {
			Variable var = Variables.get(key);
			var.setDefault(values.getString(key + ".value", "%" + key + "%"));
			//the default ticker
			var.getDefaultTicker().load(values, key);
			for (String player : values.getKeys(key + ".forPlayers")) {
				String root = key + ".forPlayers." + player;
				String value = values.getString(root + ".value", null);
				PlayerVariable pvar = var.forPlayer(player);
				if (value != null) pvar.set(value);
				pvar.getTicker().load(values, root);
			}
		}
	}
	
	public void saveValues() {
		Configuration values = new Configuration(this.getDataFolder() + File.separator + "values.yml");
		for (Variable var : Variables.all()) {
			String key = var.getName();
			if (Variables.isUsedByPlugin(key)) continue;
			values.set(key + ".value", var.getDefault());
			var.getDefaultTicker().save(values, key);
			for (PlayerVariable pvar : var.forAll()) {
				String root = key + ".forPlayers." + pvar.getPlayer();
				values.set(root + ".value", pvar.get());
				if (!pvar.isTickerShared()) {
					pvar.getTicker().save(values, root);
				}
			}
		}
		values.save();
	}
		
	public void updatePlayerName(Player p) {
		Variables.get("playername").forPlayer(p).set(p.getName());
	}
	
	public void onEnable() {
		plugin = this;
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvents(new SLBlockListener(), this);
		pm.registerEvents(new SLLowBlockListener(), this);

		getCommand("togglesignupdate").setExecutor(this);
		getCommand("reloadsignlink").setExecutor(this);
		getCommand("variable").setExecutor(this);
		
		Configuration config = new Configuration(this);
		config.load();
		String timeFormat = config.parse("timeFormat", "H:mm:ss");
		String dateFormat = config.parse("dateFormat", "yyyy.MM.dd");
		usePermissions = config.parse("usePermissions", false);
		try {
			this.timeFormat = new SimpleDateFormat(timeFormat);
		} catch (IllegalArgumentException ex) {
			Util.log(Level.WARNING, "Time format: " + timeFormat + " has not been recognized!");
			timeFormat = "H:mm:ss";
			this.timeFormat = new SimpleDateFormat(timeFormat);
		}
		try {
			this.dateFormat = new SimpleDateFormat(dateFormat);
		} catch (IllegalArgumentException ex) {
			Util.log(Level.WARNING, "Date format: " + dateFormat + " has not been recognized!");
			dateFormat = "yyyy.MM.dd";
			this.dateFormat = new SimpleDateFormat(dateFormat);
		}
		config.save();
		
		VirtualSign.init();
		
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
			private long prevtpstime = System.currentTimeMillis();
			public void run() {
				Variables.get("time").set(Util.now(SignLink.plugin.timeFormat));
				Variables.get("date").set(Util.now(SignLink.plugin.dateFormat));
				long newtime = System.currentTimeMillis();
				float ticktime = (float) (newtime - prevtpstime) / 5000;
				if (ticktime == 0) ticktime = 1;
				int per = (int) (5 / ticktime);
				if (per > 100) per = 100;
				Variables.get("tps").set(per + "%");
				prevtpstime = newtime;
			}
		};
		timetask.startRepeating(5, 5, false);
		
		loadValues();
		
		//Start updating
		updatetask = new Task(this) {
			public void run() {
				Variables.updateTickers();
				VirtualSign.updateAll();
			}
		};
		updatetask.startRepeating(1L);
		
		updateSigns = true;
		
		for (Player p : getServer().getOnlinePlayers()) {
			updatePlayerName(p);
		}
		Permission.init(this);
		
		Util.log(Level.INFO, " version " + this.getDescription().getVersion() + " is enabled!");
	}
	
	private Task updatetask;
	private Task timetask;
		
	public void onDisable() {
		Task.stop(timetask);
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
		
		//Save variable values and tickers to file
		this.saveValues();
		
		Variables.deinit();
		VirtualSign.deinit();
		Permission.deinit();
		Util.log(Level.INFO, " is disabled!");
	}
	
	private class VariableEdit {
		public VariableEdit(Variable var) {
			this.variable = var;
			this.players = new String[0];
		}
		public Variable variable;
		public String[] players;
		public boolean global() {
			return this.players.length == 0;
		}
		public GroupVariable group() {
			return this.variable.forGroup(this.players);
		}
	}
	
	private HashMap<String, VariableEdit> editingvars = new HashMap<String, VariableEdit>();
	
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {
		if (cmdLabel.equalsIgnoreCase("togglesignupdate")) {
			if (!(sender instanceof Player) || Permission.has((Player) sender, "toggleupdate")) {
				updateSigns = !updateSigns;
				if (updateSigns) {
					sender.sendMessage("Signs are now being updated!");
				} else {
					sender.sendMessage("Signs are now no longer being updated!");
				}
			}
		} else if (cmdLabel.equalsIgnoreCase("reloadsignlink")) {
			if (!(sender instanceof Player) || Permission.has((Player) sender, "reload")) {
				loadValues();
				sender.sendMessage("SignLink reloaded the Variable values");
			}
		} else if (cmdLabel.equalsIgnoreCase("variable") || cmdLabel.equalsIgnoreCase("var")) {
			if (sender instanceof Player) {
				Player p = (Player) sender;
				if (args.length > 0) {
					cmdLabel = args[0];
					args = Util.remove(args, 0);
					if (cmdLabel.equalsIgnoreCase("edit") || cmdLabel.equalsIgnoreCase("add")) {
						if (args.length >= 1) {
							if (Permission.hasGlobal(p, "edit.", args[0])) {
								VariableEdit edit = new VariableEdit(Variables.get(args[0]));
								edit.players = new String[args.length - 1];
								for (int i = 1; i < args.length; i++) {
									edit.players[i - 1] = args[i];
								}
								editingvars.put(p.getName().toLowerCase(), edit);
								p.sendMessage(ChatColor.GREEN + "You are now editing variable '" + args[0] + "'");
								if (edit.players.length > 0) {
									String msg = ChatColor.YELLOW + "For players:";
									for (String player : edit.players) {
										msg += " " + player;
									}
								    p.sendMessage(msg);
								}
							} else {
								p.sendMessage(ChatColor.RED + "You don't have permission to use this!");
								return true;
							}
						} else {
							p.sendMessage(ChatColor.RED + "Please specify a variable name!");
						}
					} else {
						VariableEdit var = editingvars.get(p.getName().toLowerCase());
						if (var != null) {
							//Variable property coding here
							//===============================
							if (cmdLabel.equalsIgnoreCase("for") || cmdLabel.equalsIgnoreCase("forplayers")) {
								if (args.length == 0) {
									var.players = new String[0];
									p.sendMessage(ChatColor.GREEN + "You are now editing this variable for all players!");
								} else {
									var.players = args;
									p.sendMessage(ChatColor.GREEN + "You are now editing this variable for the selected players!");
								}
							} else if (cmdLabel.equalsIgnoreCase("get")) {
								if (var.global()) {
									p.sendMessage(ChatColor.YELLOW + "Current value is: " + var.variable.getDefault());
								} else {
									p.sendMessage(ChatColor.YELLOW + "Current value is: " + var.variable.get(var.players[0]));
								}
							} else if (cmdLabel.equalsIgnoreCase("setdefault") || cmdLabel.equalsIgnoreCase("setdef")) {
								if (args.length == 0) {
									var.variable.setDefault("");
									p.sendMessage(ChatColor.YELLOW + "Default variable value emptied!");
								} else {
									String value = "";
								    for (String part : args) {
								    	if (value != "") value += " ";
								    	value += part;
								    }
								    value = Util.replaceColors(value);
								    var.variable.setDefault(value);
									p.sendMessage(ChatColor.YELLOW + "Default variable value set to '" + value + "'!");
								}
							} else if (cmdLabel.equalsIgnoreCase("set")) {
								if (args.length == 0) {
									if (var.global()) {
										var.variable.set("");
									} else {
										var.group().set("");
									}
									p.sendMessage(ChatColor.YELLOW + "Variable value emptied!");
								} else {
									String value = "";
								    for (String part : args) {
								    	if (value != "") value += " ";
								    	value += part;
								    }
								    value = Util.replaceColors(value);
									if (var.global()) {
										var.variable.set(value);
									} else {
										var.group().set(value);
									}
									p.sendMessage(ChatColor.YELLOW + "Variable value set to '" + value + "'!");
								}
							} else if (cmdLabel.equalsIgnoreCase("clear")) {
								if (var.global()) {
									var.variable.clear();
								} else {
									var.group().clear();
								}
								p.sendMessage(ChatColor.YELLOW + "Variable has been cleared!");
							} else if (cmdLabel.equals("addpause") || cmdLabel.equalsIgnoreCase("pause")) {
								if (args.length == 2) {
									try {
										int delay = Integer.parseInt(args[0]);
										int duration = Integer.parseInt(args[1]);
										Ticker t;
										if (var.global()) {
											t = var.variable.getTicker();
										} else {
											t = var.group().getTicker();
										}
										t.addPause(delay, duration);
										p.sendMessage(ChatColor.GREEN + "Ticker pause added!");
									} catch (Exception ex) {
										p.sendMessage(ChatColor.RED + "Please specify valid pause delay and duration values!");
									}
								} else {
									p.sendMessage(ChatColor.RED + "Please specify the delay and duration for this pause!");
								}
							} else if (cmdLabel.equalsIgnoreCase("clearpauses") || cmdLabel.equalsIgnoreCase("clearpause")) {
								Ticker t;
								if (var.global()) {
									t = var.variable.getTicker();
								} else {
									t = var.group().getTicker();
								}
								t.clearPauses();
								p.sendMessage(ChatColor.YELLOW + "Ticker pauses cleared!");
							} else if (cmdLabel.equalsIgnoreCase("setticker")) {
								if (args.length >= 1) {
									TickMode mode = TickMode.NONE;
									if (args[0].equalsIgnoreCase("left")) mode = TickMode.LEFT;
									if (args[0].equalsIgnoreCase("right")) mode = TickMode.RIGHT;
									int interval = 1;
									if (args.length > 1) {
										try {
											interval = Integer.parseInt(args[1]);
										} catch (Exception ex) {}
									}
									Ticker t;
									if (var.global()) {
										t = var.variable.getTicker();
									} else {
										t = var.group().getTicker();
									}
									t.mode = mode;
									t.interval = interval;
									p.sendMessage(ChatColor.GREEN + "You set a '" + mode.toString().toLowerCase() + "' ticker ticking every " + interval + " ticks!");
								} else {
									p.sendMessage(ChatColor.RED + "Please specify the ticker direction!");
								}
							}
							//===============================
						} else {
							p.sendMessage(ChatColor.RED + "Please edit or add a variable first!");
						}
					}
				} else {
					p.sendMessage(ChatColor.RED + "Please specify a sub-command!");
				}
			} else {
				sender.sendMessage("This command is only for players!");
			}
		}
 		return true;
	}
	
}

package com.bergerkiller.bukkit.sl;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;

import com.bergerkiller.bukkit.common.config.ConfigurationNode;
import com.bergerkiller.bukkit.common.config.FileConfiguration;
import com.bergerkiller.bukkit.common.PluginBase;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.EnumUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.sl.API.GroupVariable;
import com.bergerkiller.bukkit.sl.API.PlayerVariable;
import com.bergerkiller.bukkit.sl.API.TickMode;
import com.bergerkiller.bukkit.sl.API.Ticker;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;
import com.bergerkiller.bukkit.sl.LinkedSign.Direction;

public class SignLink extends PluginBase {
	public static SignLink plugin;
	
	public static boolean updateSigns = false;
	public static boolean allowSignEdit = true;
	public static boolean usePermissions = false;

	private SimpleDateFormat dateFormat;
	private SimpleDateFormat timeFormat;
		
	public void loadValues() {
		FileConfiguration values = new FileConfiguration(this, "values.yml");
		if (!values.exists()) {			
			values.set("test.ticker", "LEFT");
			values.set("test.tickerInterval", 3);
			values.set("test.value", "This is a test message being ticked from right to left. ");
			values.set("sign.ticker", "NONE");
			values.set("sign.value", "This is a regular message you can set and is updated only once.");
			values.save();
		}
		values.load();
		for (ConfigurationNode node : values.getNodes()) {
			Variable var = Variables.get(node.getName());
			var.setDefault(node.get("value", "%" + var.getName() + "%"));
			var.getDefaultTicker().load(node);
			for (ConfigurationNode forplayer : node.getNode("forPlayers").getNodes()) {
				String value = forplayer.get("value", String.class, null);
				PlayerVariable pvar = var.forPlayer(forplayer.getName());
				if (value != null) pvar.set(value);
				pvar.getTicker().load(forplayer);
			}
		}
	}	
	public void saveValues() {
		FileConfiguration values = new FileConfiguration(this, "values.yml");
		for (Variable var : Variables.all()) {
			if (var.isUsedByPlugin()) continue;
			ConfigurationNode node = values.getNode(var.getName());
			node.set("value", var.getDefault());
			var.getDefaultTicker().save(node);
			for (PlayerVariable pvar : var.forAll()) {
				ConfigurationNode forplayer = node.getNode("forPlayers").getNode(pvar.getPlayer());
				forplayer.set("value", pvar.get());
				if (!pvar.isTickerShared()) {
					pvar.getTicker().save(forplayer);
				}
			}
		}
		values.save();
	}
		
	public void updatePlayerName(Player p) {
		Variables.get("playername").forPlayer(p).set(p.getName());
	}
	
	public void enable() {
		plugin = this;
		this.register(SLListener.class);
		this.register("togglesignupdate", "reloadsignlink", "variable");

		FileConfiguration config = new FileConfiguration(this);
		config.load();
		String timeFormat = config.get("timeFormat", "H:mm:ss");
		String dateFormat = config.get("dateFormat", "yyyy.MM.dd");
		usePermissions = config.get("usePermissions", false);
		allowSignEdit = config.get("allowSignEdit", true);
		try {
			this.timeFormat = new SimpleDateFormat(timeFormat);
		} catch (IllegalArgumentException ex) {
			log(Level.WARNING, "Time format: " + timeFormat + " has not been recognized!");
			timeFormat = "H:mm:ss";
			this.timeFormat = new SimpleDateFormat(timeFormat);
		}
		try {
			this.dateFormat = new SimpleDateFormat(dateFormat);
		} catch (IllegalArgumentException ex) {
			log(Level.WARNING, "Date format: " + dateFormat + " has not been recognized!");
			dateFormat = "yyyy.MM.dd";
			this.dateFormat = new SimpleDateFormat(dateFormat);
		}
		config.save();
		
		VirtualSign.init();
		
		//Load sign locations from file
		
		config = new FileConfiguration(this, "linkedsigns.yml");
		config.load();
		for (String node : config.getKeys()) {
			Variable currvar = Variables.get(node);
			for (String textline : config.getList(node, String.class)) {
				try {
					String[] bits = textline.split("_");
					Direction direction = EnumUtil.parse(Direction.class, bits[bits.length - 1], Direction.NONE);
					int line = Integer.parseInt(bits[bits.length - 2]);
	    			int x = Integer.parseInt(bits[bits.length - 5]);
	    			int y = Integer.parseInt(bits[bits.length - 4]);
	    			int z = Integer.parseInt(bits[bits.length - 3]);
	    			StringBuilder worldnameb = new StringBuilder();
	    			for (int i = 0; i <= bits.length - 6; i++) {
	    				if (i > 0) worldnameb.append('_');
	    				worldnameb.append(bits[i]);
	    			}
	    			currvar.addLocation(worldnameb.toString(), x, y, z, line, direction);
				} catch (Exception ex) {
					log(Level.WARNING, "Failed to parse line: " + textline);
					ex.printStackTrace();
				}
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
		}.start(5, 5);
		
		loadValues();
		
		//Start updating
		updatetask = new Task(this) {
			public void run() {
				try {
					Variables.updateTickers();
					VirtualSign.updateAll();
				} catch (Throwable t) {
					log(Level.SEVERE, "An error occured while updating the signs:");
					t.printStackTrace();
				}
			}
		}.start(1, 1);
				
		updateSigns = true;
		
		for (Player p : getServer().getOnlinePlayers()) {
			updatePlayerName(p);
		}
		Permission.init(this);
	}
	
	private Task updatetask;
	private Task timetask;
		
	public void disable() {
		Task.stop(timetask);
		Task.stop(updatetask);
		
		//Save sign locations to file
		FileConfiguration config = new FileConfiguration(this, "linkedsigns.yml");
		for (String varname : Variables.getNames()) {
			List<String> nodes = config.getList(varname, String.class);
			for (LinkedSign sign : Variables.get(varname).getSigns()) {
				StringBuilder builder = new StringBuilder(40);
				builder.append(sign.worldname).append('_').append(sign.x);
				builder.append('_').append(sign.y);
				builder.append('_').append(sign.z);
				builder.append('_').append(sign.line);
				if (sign.direction == Direction.LEFT) {
					builder.append('_').append("LEFT");
				} else if (sign.direction == Direction.RIGHT) {
					builder.append('_').append("RIGHT");
				} else {
					builder.append('_').append("NONE");
				}
				nodes.add(builder.toString());
			}
			if (nodes.isEmpty()) config.remove(varname);
		}
		config.save();
		
		//Save variable values and tickers to file
		this.saveValues();
		
		Variables.deinit();
		VirtualSign.deinit();
		Permission.deinit();
		log(Level.INFO, " is disabled!");
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
	
	public boolean command(CommandSender sender, String cmdLabel, String[] args) {
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
					args = StringUtil.remove(args, 0);
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
	@Override
	public void permissions() {
		this.loadPermission("signlink.addsign", PermissionDefault.OP, "Allows you to build signs containing variables");
		this.loadPermission("signlink.toggleupdate", PermissionDefault.OP, "Allows you to set if signs are being updated or not");
		this.loadPermission("signlink.reload", PermissionDefault.OP, "Allows you to reload the values.yml");
		this.loadPermission("signlink.edit.*", PermissionDefault.OP, "Allows you to edit all variables");
	}
	
}

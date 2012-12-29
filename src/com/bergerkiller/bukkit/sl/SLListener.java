package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class SLListener implements Listener {
	public static HashSet<Location> stopAutoColor = new HashSet<Location>();

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSignChange(final SignChangeEvent event) {
		if (!event.isCancelled()) {			
			//Convert colors
			for (int i = 0; i < event.getLines().length; i++) {
				event.setLine(i, StringUtil.ampToColor(event.getLine(i)));
			}

			//General stuff...
			boolean allowvar = Permission.ADDSIGN.has(event.getPlayer());
			final ArrayList<String> varnames = new ArrayList<String>();
			for (int i = 0; i < 4; i++) {
				String varname = Util.getVarName(event.getLine(i));
				if (varname != null) {
					if (allowvar) {
						Variable var = Variables.get(varname);
						if (var.addLocation(event.getBlock(), i)) {
							varnames.add(varname);
						} else {
							event.getPlayer().sendMessage(ChatColor.RED + "Failed to create a sign linking to variable '" + varname + "'!");
						}
					} else {
						event.getPlayer().sendMessage(ChatColor.DARK_RED + "You don't have permission to use dynamic text on signs!");
						return;
					}
				}
			}
			if (varnames.isEmpty()) {
				return;
			}
			if (varnames.size() == 1) {
				event.getPlayer().sendMessage(ChatColor.GREEN + "You made a sign linking to variable: " + varnames.get(0));
			} else {
				String msg = ChatColor.GREEN + "You made a sign linking to variables:";
				for (String var : varnames) {
					msg += " " + var;
				}
				event.getPlayer().sendMessage(msg);
			}
			new Task(SignLink.plugin) {
				public void run() {
					if (event.isCancelled()) return;
					if (!VirtualSign.exists(event.getBlock())) {
						VirtualSign.add(event.getBlock(), event.getLines());
					}
					Variables.updateSignOrder(event.getBlock());
				}
			}.start();
		}
	}
		
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			Variables.removeLocation(event.getBlock());
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		VirtualSign.invalidateAll(event.getPlayer());
	}
}

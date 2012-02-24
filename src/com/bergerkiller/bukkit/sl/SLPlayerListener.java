package com.bergerkiller.bukkit.sl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SLPlayerListener implements Listener {
	
 @EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}
 
 @EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		VirtualSign.invalidateAll(event.getPlayer());
	}

}

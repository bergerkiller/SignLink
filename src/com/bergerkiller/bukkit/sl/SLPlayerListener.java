package com.bergerkiller.bukkit.sl;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class SLPlayerListener extends PlayerListener {

	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		VirtualSign.invalidateAll(event.getPlayer());
	}

}

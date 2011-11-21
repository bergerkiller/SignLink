package com.bergerkiller.bukkit.sl;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;

public class SLPlayerListener extends PlayerListener {

	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}

}

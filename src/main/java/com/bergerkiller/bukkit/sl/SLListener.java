package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.bergerkiller.bukkit.common.MessageBuilder;
import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.StringUtil;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class SLListener implements Listener, PacketListener {
	protected static boolean ignore = false;

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (ignore) {
			return;
		}
		if (event.getType() == PacketType.UPDATE_SIGN) {
			CommonPacket packet = event.getPacket();
			Block block = PacketFields.UPDATE_SIGN.getBlock(packet.getHandle(), event.getPlayer().getWorld());
			VirtualSign sign = VirtualSign.get(block);
			if (sign != null) {
				// Set the lines of the packet to the valid lines
				packet.write(PacketFields.UPDATE_SIGN.lines, sign.getLines(event.getPlayer()).get());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSignChange(final SignChangeEvent event) {
		//Convert colors
		for (int i = 0; i < VirtualLines.LINE_COUNT; i++) {
			event.setLine(i, StringUtil.ampToColor(event.getLine(i)));
		}

		// Update sign order and other information the next tick (after this sign is placed)
		CommonUtil.nextTick(new Runnable() {
			public void run() {
				if (event.isCancelled()) {
					return;
				}
				if (!VirtualSign.exists(event.getBlock())) {
					Sign sign = BlockUtil.getSign(event.getBlock());
					if (sign != null) {
						VirtualSign.add(sign, event.getLines());
					}
				}
				Variables.updateSignOrder(event.getBlock());
			}
		});
	
		//General stuff...
		boolean allowvar = Permission.ADDSIGN.has(event.getPlayer());
		final ArrayList<String> varnames = new ArrayList<String>();
		for (int i = 0; i < VirtualLines.LINE_COUNT; i++) {
			String varname = Variables.parseVariableName(event.getLine(i));
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

		// Send a message to the player showing that SignLink has responded
		MessageBuilder message = new MessageBuilder().green("You made a sign linking to ");
		if (varnames.size() == 1) {
			message.append("variable: ").yellow(varnames.get(0));
		} else {
			message.append("variables: ").yellow(StringUtil.combine(" ", varnames));
		}
		message.send(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Variables.removeLocation(event.getBlock());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player p = event.getPlayer();
		Variables.get("playername").forPlayer(p).set(p.getName());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		VirtualSign.invalidateAll(event.getPlayer());
	}
}

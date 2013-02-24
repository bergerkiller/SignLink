package com.bergerkiller.bukkit.sl;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.events.PacketReceiveEvent;
import com.bergerkiller.bukkit.common.events.PacketSendEvent;
import com.bergerkiller.bukkit.common.protocol.CommonPacket;
import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.protocol.PacketListener;
import com.bergerkiller.bukkit.common.protocol.PacketType;

public class SLPacketListener implements PacketListener {
	public static boolean ignore = false;

	@Override
	public void onPacketReceive(PacketReceiveEvent event) {
	}

	@Override
	public void onPacketSend(PacketSendEvent event) {
		if (ignore) {
			return;
		}
		CommonPacket packet = event.getPacket();
		if (packet.getType() == PacketType.UPDATE_SIGN) {
			Block block = PacketFields.UPDATE_SIGN.getBlock(packet.getHandle(), event.getPlayer().getWorld());
			VirtualSign sign = VirtualSign.get(block);
			if (sign != null) {
				if (!sign.ignorePacket()) {
					sign.update();
				}
				event.setCancelled(true);
			}
		}
	}
}

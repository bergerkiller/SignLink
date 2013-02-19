package com.bergerkiller.bukkit.sl;

import org.bukkit.entity.Player;

import com.bergerkiller.bukkit.common.protocol.PacketFields;
import com.bergerkiller.bukkit.common.utils.PacketUtil;

public class VirtualLines {
	private String[] lines;
	private boolean changed = false;
	
	public VirtualLines(String[] lines) {
		this.lines = new String[4];
		for (int i = 0; i < 4; i++) {
			this.lines[i] = lines[i];
		}
	}
	
	public void set(int index, String value) {
		if (this.changed || !this.lines[index].equals(value)) {
			this.changed = true;
			this.lines[index] = value;
		}
	}
	public String get(int index) {
		return lines[index];
	}
	public String[] get() {
		return this.lines;
	}
	
	public boolean hasChanged() {
		return this.changed;
	}
	public void setChanged() {
		setChanged(true);
	}
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	public void updateSign(Player player, int x, int y, int z) {
		if (SignLink.updateSigns && player != null) {
			String[] lines = new String[4];
			for (int i = 0; i < 4; i++) {
				if (this.lines[i].length() > 15) {
					lines[i] = this.lines[i].substring(0, 15);
				} else {
					lines[i] = this.lines[i];
				}
			}
			SLPacketListener.ignore = true;
			PacketUtil.sendPacket(player, PacketFields.UPDATE_SIGN.newInstance(x, y, z, lines));
			SLPacketListener.ignore = false;
		}
	}
}

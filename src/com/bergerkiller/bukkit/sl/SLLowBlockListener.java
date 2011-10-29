package com.bergerkiller.bukkit.sl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

public class SLLowBlockListener extends BlockListener {
	
	@Override
	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled()) {
			//Get the ACTUAL sign being edited :)
			Location alternative = SLBlockListener.editedSigns.remove(event.getBlock().getLocation());
			if (alternative != null) {
				Block b = alternative.getBlock();
				if (Util.isSign(b)) {
					//Remove the old one
					event.getBlock().setTypeId(0);
					//Cancel
					event.setCancelled(true);
					//We need to target another sign...first handle text change
					event = new SignChangeEvent(b, event.getPlayer(), event.getLines());
					Bukkit.getServer().getPluginManager().callEvent(event);
				    if (!event.isCancelled()) {
				    	//handle the new text
						VirtualSign sign = VirtualSign.get(b);
						for (int i = 0; i < 4; i++) {
							sign.setRealLine(i, event.getLine(i));
						}
						sign.update(true);
				    }
					return;
				}
			}
		}
	}
}

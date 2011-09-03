package com.bergerkiller.bukkit.sl;

import org.bukkit.ChatColor;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;

public class SLBlockListener extends BlockListener {
		
	@Override
	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled()) {
			boolean allowvar = event.getPlayer().hasPermission("signlink.addsign");
			for (int i = 0;i < 4; i++) {
				String varname = Util.getVarName(event.getLine(i));
				if (varname != null) {
					if (allowvar) {
						VirtualSign.add(event.getBlock(), event.getLines());
						Variable var = Variables.get(varname);
						var.addLocation(event.getBlock(), i);
						var.update(1);
						event.getPlayer().sendMessage(ChatColor.GREEN + "You made a sign linking to variable: " + varname);
					} else {
						event.getPlayer().sendMessage(ChatColor.DARK_RED + "You don't have permission to use dynamic text on signs!");
						break;
					} 
				}
			}
			Variables.updateSignOrder();
		}
	}
		
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			Variables.removeLocation(event.getBlock());
		}
	}
	
}

package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.server.Packet130UpdateSign;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class SLBlockListener extends BlockListener {
	public static HashMap<Location, Location> editedSigns = new HashMap<Location, Location>();
	public static HashSet<Location> stopAutoColor = new HashSet<Location>();
	
	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		if (Util.isSign(event.getBlockPlaced())) {
			if (Util.isSign(event.getBlockAgainst())) {
				//Sign on sign placement
				//Get the sign before we possible break it
				VirtualSign sign = VirtualSign.get(event.getBlockAgainst());
				//Is the player allowed to break (edit) this sign?
				BlockBreakEvent breakEvent = new BlockBreakEvent(event.getBlockAgainst(), event.getPlayer());
				SignLink.plugin.getServer().getPluginManager().callEvent(breakEvent);
				if (!breakEvent.isCancelled()) {
					Block b = event.getBlockPlaced();
					//Player can edit this sign.
					
					//Get the required info
					String[] lines = new String[4];
					for (int i = 0; i < 4; i++) {
						lines[i] = sign.getRealLine(i).replace('§', '&');
					}
							
					//Hide the old sign from the user
					//Without this the sign doesn't update for some reason
					Util.hideBlock(event.getBlockAgainst());
					
					//Update the sign for the player
					Task t = new Task(event.getPlayer(), b, lines) {
						public void run() {
							Player p = (Player) getArg(0);
							Block b = (Block) getArg(1);
							String[] lines = (String[]) getArg(2);
							setText(p, b, lines);
						}
					};
					t.startDelayed(0);
					
					//Hide the newly placed sign (it's obstructing the view)
					t = new Task(event.getBlockPlaced()) {
						public void run() {
							Util.hideBlock((Block) getArg(0));
						}
					};
					t.startDelayed(0);
					
					//All worked out, now we need to watch the sign for changes...
					editedSigns.put(event.getBlockPlaced().getLocation(), event.getBlockAgainst().getLocation());
					
					//Restore the item for a bit...
					ItemStack item = event.getPlayer().getItemInHand();
					event.getPlayer().setItemInHand(item);
				} else {
					//No permission to edit
					event.setCancelled(true);
				}
			}
		}
	}
	
	public static void setText(Player forPlayer, Block signblock, String[] lines) {		
		int x = signblock.getLocation().getBlockX();
		int y = signblock.getLocation().getBlockY();
		int z = signblock.getLocation().getBlockZ();
		Util.sendPacket(forPlayer, new Packet130UpdateSign(x, y, z, lines));
	}
		
	@Override
	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled()) {			
			//Convert colors
			Util.replaceColors(event.getLines());
			
			Task t = new Task(event) {
				public void run() {
					SignChangeEvent event = (SignChangeEvent) getArg(0);
					if (event.isCancelled()) return;
					//General stuff...
					boolean allowvar = Permission.has(event.getPlayer(), "addsign");
					if (!VirtualSign.exists(event.getBlock())) {
						VirtualSign.add(event.getBlock(), event.getLines());
					}
					ArrayList<String> varnames = new ArrayList<String>();
					for (int i = 0; i < 4; i++) {
						String varname = Util.getVarName(event.getLine(i));
						if (varname != null) {
							if (allowvar) {
								Variable var = Variables.get(varname);
								if (var.addLocation(event.getBlock(), i)) {
									varnames.add(varname);
								} else {
									event.getPlayer().sendMessage(ChatColor.RED + "Failed to create a sign linking to variable '" + var + "'!");
								}
							} else {
								event.getPlayer().sendMessage(ChatColor.DARK_RED + "You don't have permission to use dynamic text on signs!");
								return;
							}
						}
					}
					if (varnames.size() == 1) {
						event.getPlayer().sendMessage(ChatColor.GREEN + "You made a sign linking to variable: " + varnames.get(0));
					} else if (varnames.size() > 0) {
						String msg = ChatColor.GREEN + "You made a sign linking to variables:";
						for (String var : varnames) {
							msg += " " + var;
						}
						event.getPlayer().sendMessage(msg);
					}
					Variables.updateSignOrder(event.getBlock());
				}
			};
			t.startDelayed(0);
		}
	}
		
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			Variables.removeLocation(event.getBlock());
		}
	}
	
}

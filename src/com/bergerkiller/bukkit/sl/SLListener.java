package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.minecraft.server.Packet130UpdateSign;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.CommonUtil;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.sl.API.Variable;
import com.bergerkiller.bukkit.sl.API.Variables;

public class SLListener implements Listener {

	public static HashMap<Location, Location> editedSigns = new HashMap<Location, Location>();
	public static HashSet<Location> stopAutoColor = new HashSet<Location>();
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (!SignLink.allowSignEdit) return;
		if (BlockUtil.isSign(event.getBlockPlaced())) {
			if (BlockUtil.isSign(event.getBlockAgainst())) {
				//Sign on sign placement
				//Get the sign before we possible break it
				VirtualSign sign = VirtualSign.get(event.getBlockAgainst());
				//Is the player allowed to break (edit) this sign?				
				BlockBreakEvent breakEvent = new BlockBreakEvent(event.getBlockAgainst(), event.getPlayer());
				if (!CommonUtil.callEvent(breakEvent).isCancelled()) {
					final Block placed = event.getBlockPlaced();
					final Player player = event.getPlayer();
					//Player can edit this sign.
					
					//Get the required info
					final String[] lines = new String[4];
					for (int i = 0; i < 4; i++) {
						lines[i] = sign.getRealLine(i).replace('§', '&');
					}
					
					//Update the sign for the player
					new Task(SignLink.plugin) {
						public void run() {
							setText(player, placed, lines);
						}
					}.start();
					
					//Hide the newly placed sign (it's obstructing the view)
					new Task(SignLink.plugin) {
						public void run() {
							Util.hideBlock(placed);
						}
					}.start(1);
					
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
		PacketUtil.sendPacket(forPlayer, new Packet130UpdateSign(x, y, z, lines), false);
	}
		
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSignChange(SignChangeEvent event) {
		if (!event.isCancelled()) {			
			//Convert colors
			Util.replaceColors(event.getLines());
			
			new Task(SignLink.plugin, event) {
				public void run() {
					SignChangeEvent event = arg(0, SignChangeEvent.class);
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
									event.getPlayer().sendMessage(ChatColor.RED + "Failed to create a sign linking to variable '" + varname + "'!");
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
			}.start();
		}
	}
		
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			Variables.removeLocation(event.getBlock());
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onSignChangeLow(SignChangeEvent event) {
		if (!event.isCancelled()) {
			//Get the ACTUAL sign being edited :)
			Location alternative = editedSigns.remove(event.getBlock().getLocation());
			if (alternative != null) {
				Block b = alternative.getBlock();
				if (BlockUtil.isSign(b)) {
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
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		VirtualSign.invalidateAll(event.getPlayer());
	}

}

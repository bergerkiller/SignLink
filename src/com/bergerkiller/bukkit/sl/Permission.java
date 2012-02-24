package com.bergerkiller.bukkit.sl;

import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public class Permission {
	private static PermissionHandler permissionHandler = null; //Permissions 3.* ONLY
	public static void init(JavaPlugin plugin) {
		if (SignLink.usePermissions) {
			Plugin permissionsPlugin = plugin.getServer().getPluginManager().getPlugin("Permissions");
			if (permissionsPlugin == null) {
				SignLink.plugin.log(Level.WARNING, "Permission system not detected, defaulting to build-in permissions!");
			} else {
				permissionHandler = ((Permissions) permissionsPlugin).getHandler();
				SignLink.plugin.log(Level.INFO, "Found and will use permissions plugin "+((Permissions)permissionsPlugin).getDescription().getFullName());
			}
		} else {
			SignLink.plugin.log(Level.INFO, "Using build-in 'Bukkit SuperPerms' as permissions plugin!");;
		}
	}
	public static void deinit() {
		permissionHandler = null;
	}
	
	public static boolean has(Player player, String node) {
		if (permissionHandler != null) {
			//Permissions 3.*
			return permissionHandler.has(player, "signlink." + node);
		} else {
			//Build-in permissions
			return player.hasPermission("signlink." + node);
		}
	}
	public static boolean hasGlobal(Player player, String node, String name) {
		return has(player, node + name) || has(player, node + "*");
	}
		
}

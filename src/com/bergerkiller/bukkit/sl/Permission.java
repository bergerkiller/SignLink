package com.bergerkiller.bukkit.sl;

import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.bergerkiller.bukkit.common.permissions.IPermissionDefault;
import com.bergerkiller.bukkit.common.permissions.NoPermissionException;
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

public enum Permission implements IPermissionDefault {
	ADDSIGN("addsign", PermissionDefault.OP, "Allows you to build signs containing variables"),
	TOGGLEUPDATE("toggleupdate", PermissionDefault.OP, "Allows you to set if signs are being updated or not"),
	RELOAD("reload", PermissionDefault.OP, "Allows you to reload the values.yml"),
	EDIT("edit", PermissionDefault.OP, "Allows you to edit all variables", 1),
	GLOBALDELETE("globaldelete", PermissionDefault.OP, "Allows you to delete all variables from the server");

	private final String node;
	private final String name;
	private final PermissionDefault def;
	private final String desc;

	private Permission(final String name, final PermissionDefault def, final String desc) {
		this(name, def, desc, 0);
	}

	private Permission(final String name, final PermissionDefault def, final String desc, final int argCount) {
		this.node = "signlink." + name;
		this.def = def;
		this.desc = desc;
		StringBuilder builder = new StringBuilder(this.node);
		for (int i = 0; i < argCount; i++) {
			builder.append(".*");
		}
		this.name = builder.toString();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public PermissionDefault getDefault() {
		return this.def;
	}

	@Override
	public String getDescription() {
		return this.desc;
	}

	public boolean hasGlobal(CommandSender player, String name) {
		return has(player, name) || has(player, "*");
	}

	public boolean hasGlobal(CommandSender player, String name1, String name2) {
		return has(player, name1, name2) || has(player, name1, "*") || has(player, "*", name2) || has(player, "*", "*");
	}

	public boolean has(CommandSender player) {
		return has(player, new String[0]);
	}

	public boolean has(CommandSender player, String... args) {
		String node = this.node;
		if (args.length > 0) {
			StringBuilder builder = new StringBuilder(node);
			for (String arg : args) {
				builder.append('.').append(arg);
			}
			node = builder.toString();
		}
		if (permissionHandler != null) {
			//Permissions 3.*
			return !(player instanceof Player) || permissionHandler.has((Player) player, node);
		} else {
			//Build-in permissions
			return player.hasPermission(node);
		}
	}

	public void handle(CommandSender player) throws NoPermissionException {
		if (!has(player)) {
			throw new NoPermissionException();
		}
	}

	public void handle(CommandSender player, String... args) throws NoPermissionException {
		if (!has(player, args)) {
			throw new NoPermissionException();
		}
	}

	@Override
	public String toString() {
		return this.getName();
	}
	
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
}

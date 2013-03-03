package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.utils.MaterialUtil;
import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.SignLink;
import com.bergerkiller.bukkit.sl.Util;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variables {
	private static HashMap<String, Variable> variables = new HashMap<String, Variable>();

	public static synchronized void deinit() {
		variables.clear();
	}

	/**
	 * Checks whether a certain variable name is in use by SignLink
	 * 
	 * @param name to check
	 * @return True if it is part of SignLink, False if not
	 */
	public static boolean isUsedByPlugin(String name) {
		if (name.equalsIgnoreCase("time")) return true;
		if (name.equalsIgnoreCase("date")) return true;
		if (name.equalsIgnoreCase("playername")) return true;
		if (name.equalsIgnoreCase("tps")) return true;
		return false;
	}

	/**
	 * Updates all the tickers of all the Variables on the server
	 */
	public static synchronized void updateTickers() {
		for (Variable var : all()) {
			var.updateTickers();
		}
	}
	
	/**
	 * Gets all the variables on the server.
	 * @deprecated: This method is not thread-safe.
	 * Use {@link getAll()} instead.
	 * 
	 * @return Collection of all variables
	 */
	@Deprecated
	public static Collection<Variable> all() {
		return variables.values();
	}

	/**
	 * Updates the sign block orders of all signs showing variables
	 */
	public static synchronized void updateSignOrder() {
		for (Variable var : all()) {
			var.updateSignOrder();
		}
	}

	/**
	 * Updates the sign block orders of all signs showing variables on a given world
	 * 
	 * @param world to update
	 */
	public static synchronized void updateSignOrder(World world) {
		for (Variable var : all()) {
			var.updateSignOrder(world);
		}
	}

	/**
	 * Updates the sign block orders of all signs showing variables near a block
	 * 
	 * @param near
	 */
	public static synchronized void updateSignOrder(Block near) {
		for (Variable var : all()) {
			var.updateSignOrder(near);
		}
	}

	/**
	 * Gets a new mutual list of all variables available
	 * 
	 * @return Variables
	 */
	public static synchronized List<Variable> getAllAsList() {
		return new ArrayList<Variable>(variables.values());
	}

	/**
	 * Gets an array of all variables available
	 * 
	 * @return Variables
	 */
	public static synchronized Variable[] getAll() {
		return variables.values().toArray(new Variable[0]);
	}

	/**
	 * Gets an array of all variable names
	 * 
	 * @return Variable names
	 */
	public static synchronized String[] getNames() {
		return variables.keySet().toArray(new String[0]);
	}

	/**
	 * Gets or creates a variable of the given name
	 * 
	 * @param name of the variable
	 * @return the Variable, or null if the name is of an unsupported format
	 */
	public static synchronized Variable get(String name) {
		if (name == null || name.contains("\000")) {
			return null;
		}
		Variable var = variables.get(name);
		if (var == null) {
			var = new Variable("%" + name + "%", name);
			variables.put(name, var);
		}
		return var;
	}

	/**
	 * Gets the variable displayed on a line on a sign
	 * 
	 * @param sign on which the variable can be found
	 * @param line on which the variable can be found
	 * @return The Variable, or null if there is none
	 */
	public static synchronized Variable get(VirtualSign sign, int line) {
		return get(Util.getVarName(sign.getRealLine(line)));
	}

	/**
	 * Gets the variable displayed on a line on a sign
	 * 
	 * @param signblock on which the variable can be found
	 * @param line on which the variable can be found
	 * @return The Variable, or null if there is none
	 */
	public static synchronized Variable get(Block signblock, int line) {
		if (MaterialUtil.ISSIGN.get(signblock)) {
			return get(VirtualSign.getOrCreate(signblock), line);
		}
		return null;
	}

	/**
	 * Removes (and thus clears) a Variable from the server
	 * 
	 * @param name of the Variable to remove
	 * @return True if the variable was removed, False if it was not found
	 */
	public static synchronized boolean remove(String name) {
		Variable var = variables.remove(name);
		if (var != null) {
			SignLink.plugin.removeEditing(var);
			return true;
		}
		return false;
	}

	/**
	 * Removes a sign from all variables
	 * 
	 * @param signblock to remove
	 * @return True if the sign was found and removed, False if not
	 */
	public static boolean removeLocation(Block signblock) {
		return VirtualSign.remove(signblock);
	}

	public static synchronized boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Block at) {
		return find(signs, variables, at.getLocation());
	}

	public static synchronized boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Location at) {
		boolean found = false;
		for (Variable var : all()) {
			if (var.find(signs, variables, at)) {
				found = true;
			}
		}
		return found;
	}
}
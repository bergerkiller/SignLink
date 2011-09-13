package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.Util;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variables {
	private static HashMap<String, Variable> variables = new HashMap<String, Variable>();
	
	public static void updateSignOrder() {
		for (Variable var : variables.values()) {
			var.updateSignOrder();
		}
	}
	
	public static String[] getNames() {
		return variables.keySet().toArray(new String[0]);
	}
	
	public static Variable get(String name) {
		if (name == null) return null;
		Variable var = variables.get(name);
		if (var == null) {
			var = new Variable("%" + name + "%", name);
			variables.put(name, var);
		}
		return var;
	}
	public static Variable get(VirtualSign sign, int line) {
		return get(Util.getVarName(sign.getRealLine(line)));
	}
	public static Variable get(Block signblock, int line) {
		if (Util.isSign(signblock)) {
			return get(VirtualSign.get(signblock), line);
		}
		return null;
	}
	public static boolean remove(String name) {
		return variables.remove(name) != null;
	}
			
	public static boolean removeLocation(Block signblock) {
		for (Variable var : variables.values()) {
			var.removeLocation(signblock);
		}
		return VirtualSign.remove(signblock);
	}
	
	public static boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Block at) {
		return find(signs, variables, at.getLocation());
	}
	public static boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Location at) {
		boolean found = false;
		for (Variable var : Variables.variables.values()) {
			if (var.find(signs, variables, at)) {
				found = true;
			}
		}
		return found;
	}
}
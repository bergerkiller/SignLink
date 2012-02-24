package com.bergerkiller.bukkit.sl.API;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.sl.LinkedSign;
import com.bergerkiller.bukkit.sl.Util;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variables {
	private static HashMap<String, Variable> variables = new HashMap<String, Variable>();
	public static void deinit() {
		synchronized (variables) {
			variables.clear();
		}
	}
	
	public static boolean isUsedByPlugin(String name) {
		if (name.equalsIgnoreCase("time")) return true;
		if (name.equalsIgnoreCase("date")) return true;
		if (name.equalsIgnoreCase("playername")) return true;
		if (name.equalsIgnoreCase("tps")) return true;
		return false;
	}
	
	public static void updateTickers() {
		synchronized (variables) {
			for (Variable var : all()) {
				var.updateTickers();
			}
		}
	}
	
	public static Collection<Variable> all() {
		return variables.values();
	}
	
	public static void updateSignOrder() {
		synchronized (variables) {
			for (Variable var : all()) {
				var.updateSignOrder();
			}
		}
	}
	public static void updateSignOrder(World world) {
		synchronized (variables) {
			for (Variable var : all()) {
				var.updateSignOrder(world);
			}
		}
	}
	public static void updateSignOrder(Block near) {
		synchronized (variables) {
			for (Variable var : all()) {
				var.updateSignOrder(near);
			}
		}
	}
	
	public static String[] getNames() {
		synchronized (variables) {
			return variables.keySet().toArray(new String[0]);
		}
	}
	
	public static Variable get(String name) {
		if (name == null) return null;
		if (name.contains("\000")) return null;
		synchronized (variables) {
			Variable var = variables.get(name);
			if (var == null) {
				var = new Variable("%" + name + "%", name);
				variables.put(name, var);
			}
			return var;
		}
	}
	public static Variable get(VirtualSign sign, int line) {
		return get(Util.getVarName(sign.getRealLine(line)));
	}
	public static Variable get(Block signblock, int line) {
		if (BlockUtil.isSign(signblock)) {
			return get(VirtualSign.get(signblock), line);
		}
		return null;
	}
	public static boolean remove(String name) {
		return variables.remove(name) != null;
	}
			
	public static boolean removeLocation(Block signblock) {
		synchronized (variables) {
			for (Variable var : all()) {
				var.removeLocation(signblock);
			}
			return VirtualSign.remove(signblock);
		}
	}
	
	public static boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Block at) {
		return find(signs, variables, at.getLocation());
	}
	public static boolean find(ArrayList<LinkedSign> signs, ArrayList<Variable> variables, Location at) {
		boolean found = false;
		synchronized (variables) {
			for (Variable var : all()) {
				if (var.find(signs, variables, at)) {
					found = true;
				}
			}
		}
		return found;
	}
}
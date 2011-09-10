package com.bergerkiller.bukkit.sl.API;

import java.util.HashMap;

import org.bukkit.block.Block;

import com.bergerkiller.bukkit.sl.Util;
import com.bergerkiller.bukkit.sl.VirtualSign;

public class Variables {
	private static HashMap<String, Variable> variables = new HashMap<String, Variable>();
	
	public static void update(boolean forced) {
		VirtualSign.updateAll(forced);
	}
	public static void update() {
		update(false);
	}
	
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
		return get(Util.getVarName(sign.getLine(line)));
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
	public static Variable set(String name, String value) {
		Variable var = get(name);
		var.setValue(value);
		return var;
	}
	public static Variable set(String name, Variable value) {
		variables.put(name, value);
		return value;
	}
	public static void setList(String name, String... values) {
		for (int i = 0;i < values.length; i++) {
			set(name + "[" + i + "]", values[i]);
		}
	}
	public static int getListDisplaySize(String name) {
		int i = 0;
		while (variables.containsKey(name + "[" + (i + 1) + "]")) {
			i++;
		}
		return i;
	}
	
	public static boolean removeLocation(Block signblock) {
		for (Variable var : variables.values()) {
			var.removeLocation(signblock);
		}
		return VirtualSign.remove(signblock);
	}

}

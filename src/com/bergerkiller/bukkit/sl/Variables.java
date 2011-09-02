package com.bergerkiller.bukkit.sl;

import java.util.HashMap;

import org.bukkit.block.Block;

public class Variables {
	private static HashMap<String, Variable> variables = new HashMap<String, Variable>();
	
	public static void update() {
		VirtualSign.updateAll();
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
		Variable var = variables.get(name);
		if (var == null) {
			var = new Variable("%" + name + "%");
			variables.put(name, var);
		}
		return var;
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

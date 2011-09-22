package com.bergerkiller.bukkit.sl.API;

import org.bukkit.event.Event;

import com.bergerkiller.bukkit.sl.LinkedSign;

public class SignRemoveEvent extends Event {
	private static final long serialVersionUID = 7922584671220983146L;
	private Variable var;
	private LinkedSign sign;
	
	public SignRemoveEvent(Variable from, LinkedSign sign) {
		super("SignRemoveEvent");
		this.var = from;
		this.sign = sign;
	}

	public Variable getVariable() {
		return this.var;
	}
    public LinkedSign getSign() {
    	return this.sign;
    	
    }
		
}

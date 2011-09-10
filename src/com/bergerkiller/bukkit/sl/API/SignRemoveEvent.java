package com.bergerkiller.bukkit.sl.API;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import com.bergerkiller.bukkit.sl.LinkedSign;

public class SignRemoveEvent extends Event implements Cancellable {
	private static final long serialVersionUID = 7922584671220983146L;
	private boolean cancelled = false;
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
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.cancelled = arg0;
	}
	
}

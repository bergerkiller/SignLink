package com.bergerkiller.bukkit.sl.API;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import com.bergerkiller.bukkit.sl.LinkedSign;

public class SignAddEvent extends Event implements Cancellable {
	private static final long serialVersionUID = 8131553986500815222L;
	private boolean cancelled = false;
	private Variable var;
	private LinkedSign sign;
	
	public SignAddEvent(Variable to, LinkedSign sign) {
		super("SignAddEvent");
		this.var = to;
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

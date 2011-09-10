package com.bergerkiller.bukkit.sl.API;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class VariableChangeEvent extends Event implements Cancellable {
	private static final long serialVersionUID = -7712965743389224793L;
	private boolean cancelled = false;
	private String oldvalue;
	private String newvalue;
	private Variable variable;

	public VariableChangeEvent(Variable variable, String newvalue) {
		super("VariableChangeEvent");
		this.oldvalue = variable.getValue();
		this.variable = variable;
		this.newvalue = newvalue;
	}
	
	public String getOldValue() {
		return this.oldvalue;
	}
	public String getNewValue() {
		return this.newvalue;
	}
	public void setNewValue(String value) {
		this.newvalue = value;
	}
	public Variable getVariable() {
		return this.variable;
	}

	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.cancelled = arg0;
	}

}

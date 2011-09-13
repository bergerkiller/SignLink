package com.bergerkiller.bukkit.sl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class BlockLocation {
	
	public BlockLocation(Location l) {
		this.world = l.getWorld();
		this.x = l.getBlockX();
		this.y = l.getBlockY();
		this.z = l.getBlockZ();
	}
	public BlockLocation(Block b) {
		this.world = b.getWorld();
		this.x = b.getLocation().getBlockX();
		this.y = b.getLocation().getBlockY();
		this.z = b.getLocation().getBlockZ();
	}	
    public BlockLocation(World world, int x, int y, int z) {
    	this.world = world;
    	this.x = x;
    	this.y = y;
    	this.z = z;
	}

	public World world;
    public int x;
    public int y;
    public int z;

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + ((this.world != null) ? this.world.hashCode() : 0);
        hash = 53 * hash + (this.x ^ (this.x >> 16));
        hash = 53 * hash + (this.y ^ (this.y >> 16));
        hash = 53 * hash + (this.z ^ (this.z >> 16));
        return hash;
    }
    
    @Override
    public boolean equals(Object comparer) {
    	if (comparer == this) return true;
    	if (comparer instanceof BlockLocation) {
    		BlockLocation other = (BlockLocation) comparer;
    		if (other.world != this.world) return false;
    		if (other.x != this.x) return false;
    		if (other.y != this.y) return false;
    		if (other.z != this.z) return false;
    		return true;
    	} else if (comparer instanceof Location) {
    		Location other = (Location) comparer;
    		if (other.getWorld() != this.world) return false;
    		if (other.getBlockX() != this.x) return false;
    		if (other.getBlockY() != this.y) return false;
    		if (other.getBlockZ() != this.z) return false;
    		return true;
    	} else {
    		return false;
    	}
    }

}

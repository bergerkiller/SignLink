package com.bergerkiller.bukkit.sl;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;

import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet;
import net.minecraft.server.TileEntity;
import net.minecraft.server.TileEntitySign;

public class TileEntityVirtualSign extends TileEntitySign {
	public static boolean replace(int x, int y, int z, World w) {
		net.minecraft.server.World world = ((CraftWorld) w).getHandle();
		TileEntity t = world.getTileEntity(x, y, z);
		if (t != null) {
			if (t instanceof TileEntityVirtualSign) {
				return false;
			} else if (t instanceof TileEntitySign) {
				TileEntityVirtualSign tevs = new TileEntityVirtualSign((TileEntitySign) t);
				world.setTileEntity(x, y, z, tevs);
				return true;
			}
		}
		return true; //not found, need something done here!
	}
	
	public TileEntityVirtualSign(TileEntitySign source) {
		this.b = source.b;
		this.x = source.x;
		this.y = source.y;
		this.z = source.z;
		this.world = source.world;
		this.lines = source.lines;
	}
	
	@Override
    public void b(NBTTagCompound nbttagcompound) {
    	//Was complaining about ID <> class mapping issues
        nbttagcompound.setString("id", "Sign");
        nbttagcompound.setInt("x", this.x);
        nbttagcompound.setInt("y", this.y);
        nbttagcompound.setInt("z", this.z);
        nbttagcompound.setString("Text1", this.lines[0]);
        nbttagcompound.setString("Text2", this.lines[1]);
        nbttagcompound.setString("Text3", this.lines[2]);
        nbttagcompound.setString("Text4", this.lines[3]);
    }
	
    @Override
	public Packet d() {
		//Instead of letting the game do this
		//WE manage all packages!
		Block b = this.world.getWorld().getBlockAt(x, y, z);
		VirtualSign sign = VirtualSign.get(b);
		if (sign != null) {
			if (!sign.ignorePacket()) {
				sign.update(true);
			}
			return null;
		} else {
			return super.d();
		}
	}
	
}

package com.bergerkiller.bukkit.sl;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;

public class SLPlayerListener extends PlayerListener {

	public void onPlayerJoin(PlayerJoinEvent event) {
		SignLink.plugin.updatePlayerName(event.getPlayer());
	}
	
	public class Cuboid {
		public double xMin, xMax, yMin, yMax, zMin, zMax;
		
		public boolean isIn(Location loc) {
			if (loc.getBlockX() < xMin) return false;
			if (loc.getBlockX() > xMax) return false;
			if (loc.getBlockY() < yMin) return false;
			if (loc.getBlockY() > yMax) return false;
			if (loc.getBlockZ() < zMin) return false;
			if (loc.getBlockZ() > zMax) return false;
			return true;
		}
		
		public double widthX() {
			return this.xMax - this.xMin;
		}
		public double widthY() {
			return this.yMax - this.yMin;
		}
		public double widthZ() {
			return this.zMax - this.zMin;
		}
		public double minlength() {
		    double a = Math.min(widthX(), widthZ());
		    return Math.min(widthZ(), a);
		}
		public double length() {
			double x = widthX();
			double y = widthY();
			double z = widthZ();
			return Math.sqrt(x * x + y * y + z * z);
		}
		
	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		Block b = event.getClickedBlock();
		if (b == null) b = event.getPlayer().getTargetBlock(null, 150);
		if (b != null) {
//			//right/left clicked a sign
//			//get location relative to the block
//			Location l = event.getPlayer().getEyeLocation().subtract(b.getLocation());
//			//distance between this location and the bounding box of the block
//			net.minecraft.server.Block nb = net.minecraft.server.Block.byId[b.getTypeId()];
//			Cuboid c = new Cuboid();
//			c.xMin = nb.minX;
//			c.yMin = nb.minY;
//			c.zMin = nb.minZ;
//			c.xMax = nb.maxX;
//			c.yMax = nb.maxY;
//			c.zMax = nb.maxZ;
//			//go closer until we reach destination
//			//take a first major leap
//			double distance = l.length() - c.length();
//			l = Util.move(l, new Vector(0, 0, distance));
//			System.out.println("d: " + distance);
//			double factor = c.minlength();
//			int maxi = 100;
//			while (true) {
//				Location newl = Util.move(l, new Vector(0, 0, factor));
//				if (c.isIn(newl)) {
//					factor /= 2;
//				} else {
//					distance += factor;
//					l = newl;
//				}
//				if (factor < 0.1) break;
//				if (maxi == 0) break;
//				maxi--;
//			}

			//l = Util.move(event.getPlayer().getEyeLocation(), new Vector(0, 0, distance));
			//l.getWorld().dropItem(l, new ItemStack(Material.GLASS, 1));
		}
	}

}

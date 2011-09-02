package com.bergerkiller.bukkit.sl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class LinkedSign {	
	public LinkedSign(String worldname, int x, int y, int z, int lineAt, Direction direction) {
		this.worldname = worldname;
		this.x = x;
		this.y = y;
		this.z = z;
		this.line = lineAt;
		this.direction = direction;
	}
	public LinkedSign(Block from, int line) {
		this.worldname = from.getWorld().getName();
		this.x = from.getLocation().getBlockX();
		this.y = from.getLocation().getBlockY();
		this.z = from.getLocation().getBlockZ();
		this.line = line;
		this.direction = Direction.NONE;
		if (Util.isSign(from)) {
			VirtualSign sign = VirtualSign.get(from);
			String text = sign.getLine(line);
			int peri = text.indexOf("%");
			if (peri != -1) {
				if (text.lastIndexOf("%") == peri) {
					//get direction from text
					if (peri == 0) {
						this.direction = Direction.RIGHT;
					} else if (peri == text.length() - 1) {
						this.direction = Direction.LEFT;
					} else if (text.substring(peri).contains(" ")) {
						this.direction = Direction.LEFT;
					} else {
						this.direction = Direction.RIGHT;
					}
				}
			}
		}
	}
	
	public enum Direction {LEFT, RIGHT, NONE}
	
	public String worldname;
	public int x;
	public int y;
	public int z;
	public int line;
	private boolean updateSignOrder = false;
	public Direction direction;
	private ArrayList<VirtualSign> prevSigns = new ArrayList<VirtualSign>();
	
	public void setText(String value) {	
		if (!SignLink.updateSigns) return; 
		ArrayList<VirtualSign> signs = getSigns();
		if (signs == null) return;
		//Get the start offset
		int startoffset = signs.get(0).getLine(this.line).indexOf("%");
		if (startoffset == -1) startoffset = 0;
		int maxlength = 15 - startoffset;
				
		ArrayList<String> bits = new ArrayList<String>();
		ChatColor color = ChatColor.BLACK;
		ChatColor prevcolor = color;
		String lastbit = "";
		for (int i = 0;i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '§') {
				if (i < value.length() - 1) {
					i++;
					color = Util.getColor(value.charAt(i));
				}
			} else {
				if (prevcolor != color) {
					if (lastbit.length() < maxlength - 2) {
						lastbit += color;
					} else if (lastbit.length() == maxlength - 2) {
						bits.add(lastbit + color);
						maxlength = 15; //Next can be full
						lastbit = (color == ChatColor.BLACK) ? "" : color.toString();
					} else {
						//Greater, not allowed
						bits.add(lastbit);
						maxlength = 15; //Next can be full
						lastbit = (color == ChatColor.BLACK) ? "" : color.toString();
					}
				}
				lastbit += c;
				prevcolor = color;
				if (lastbit.length() == maxlength) {
					bits.add(lastbit);
					maxlength = 15; //Next can be full
					lastbit = (color == ChatColor.BLACK) ? "" : color.toString();
				}
			}
		}
		while (lastbit.length() < maxlength && signs.size() > 1) {
			lastbit += " ";
		}
		bits.add(lastbit);
						
		//Set the signs
		int index = 0;
		for (VirtualSign sign : signs) {
			String line = sign.getLine(this.line);
			if (index == 0 && signs.size() == 1) {
				//set the value in between the two % %
				String start = line.substring(0, startoffset);
				int endindex = line.lastIndexOf("%");
				if (endindex != -1 && endindex != startoffset) {
					String end = line.substring(endindex + 1);
					line = start + bits.get(0);
					int remainder = 15 - line.length() - end.length();
					if (remainder < 0) {
						line = line.substring(0, line.length() + remainder);
					}
					line += end;
				} else {
					line = start + bits.get(0);
				}
			} else if (index == 0) {
				//first, take % in account
				String bit = bits.get(0);
				line = line.substring(0, startoffset) + bit;
			} else if (index == signs.size() - 1) {
				//last, take % in account
				String bit = bits.get(index);
				int endindex = line.lastIndexOf("%") + 1;
				if (endindex > line.length() - 1) endindex = line.length() - 1;
				String end = "";
				if (endindex < line.length() - 1) {
					end = line.substring(endindex);
				}
				endindex = 15 - end.length();
				if (endindex > bit.length() - 1) {
					endindex = bit.length() - 1;
				}
				line = bit.substring(0, endindex) + end;
			} else {
				//A sign in the middle, simply set it
				line = bits.get(index);
			}
			sign.setLine(this.line, line);
			index++;
			if (index == bits.size() - 1) bits.add("               ");
		}
	}

	public void update() {
		ArrayList<VirtualSign> signs = getSigns();
		if (signs != null) {
			for (VirtualSign sign : signs) {
				sign.update();
			}
		}
	}
	
	private Block nextSign(Block from) {
		BlockFace face = Util.getFacing(from);
		switch (face) {
		case NORTH : face = BlockFace.EAST; break;
		case EAST : face = BlockFace.SOUTH; break;
		case SOUTH : face = BlockFace.WEST; break;
		case WEST : face = BlockFace.NORTH; break;
		}
		if (this.direction == Direction.RIGHT) {
			face = face.getOppositeFace();
		}
		return from.getRelative(face);
	}
	
	private boolean validateSigns() {
		if (prevSigns != null && prevSigns.size() != 0) {
			boolean pass = true;
			for (VirtualSign sign : prevSigns) {
				if (!sign.isValid()) {
					sign.remove();
					pass = false;
				}
			}
			return pass;
		}
		return false;
	}
	
	public Block getStartBlock() {
		World w = Bukkit.getServer().getWorld(this.worldname);
		if (w == null) return null;
		return w.getBlockAt(x, y, z);
	}
	public Location getStartLocation() {
		Block b = getStartBlock();
		if (b == null) return null;
		return b.getLocation();
	}
	
	public void updateSignOrder() {
		this.updateSignOrder = true;
	}
	
	public ArrayList<VirtualSign> getSigns() {
		Block start = getStartBlock();
		//Unloaded chunk?
		if (start != null && !Util.isLoaded(start)) return null;

		if (validateSigns() && !updateSignOrder) {
			updateSignOrder = false;
			return prevSigns;
		}
				
		//Regenerate old signs and return
		if (Util.isSign(start)) {
			HashSet<Location> loopCheck = new HashSet<Location>();
			prevSigns = new ArrayList<VirtualSign>();
			prevSigns.add(VirtualSign.get(start));
			if (this.direction == Direction.NONE) return prevSigns;
			while (true) {
				//Check for next signs
				Block next = nextSign(start);
				start = next;
				if (Util.isSign(next)) {
					if (loopCheck.add(next.getLocation())) {
						prevSigns.add(VirtualSign.get(next));
					} else {
						break;
					}
				} else {
					//Jumping a gap? We need a next possible sign
					boolean found = false;
					for (BlockFace face : Util.getFaces(true)) {
						next = start.getRelative(face);
						if (Util.isSign(next)) {
							//Is the next of this sign not the same as the source?
							if (!nextSign(next).getLocation().equals(start.getLocation())) {
								start = next;
								if (loopCheck.add(next.getLocation())) {
									//It's not added, allowed
									prevSigns.add(VirtualSign.get(next));
									found = true;
									break;
								}
							}
						}
					}
					if (!found) {
						break;
					}
				}
			}
			if (this.direction == Direction.LEFT) Collections.reverse(prevSigns);
			return prevSigns;
		}
		return null;
	}
		
}

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

import com.bergerkiller.bukkit.common.utils.BlockUtil;
import com.bergerkiller.bukkit.common.utils.FaceUtil;

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
		if (BlockUtil.isSign(from)) {
			VirtualSign sign = VirtualSign.get(from);
			String text = sign.getRealLine(line);
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
	private String oldtext;
	private ArrayList<VirtualSign> prevSigns = new ArrayList<VirtualSign>();
	
	public void updateText(String... forplayers){
		setText(this.oldtext, forplayers);
	}
	public String getText() {
		return this.oldtext;
	}
	public void setText(String value, String... forplayers) {	
		oldtext = value;
		if (!SignLink.updateSigns) return; 
		ArrayList<VirtualSign> signs = getSigns();
		if (signs == null || signs.size() == 0) return;
		//Get the start offset
		String startline = signs.get(0).getRealLine(this.line);
		int startoffset = startline.indexOf("%");
		if (startoffset == -1) startoffset = 0;
		int maxlength = 15 - startoffset;
				
		//Get the color of the text before this variable
		ChatColor color = ChatColor.BLACK;
		for (int i = 0; i < startoffset; i++) {
			if (startline.charAt(i) == '§') {
				i++;
				color = Util.getColor(startline.charAt(i), color);
			}
		}
				
		ArrayList<String> bits = new ArrayList<String>();
		ChatColor prevcolor = color;
		String lastbit = "";
		for (int i = 0;i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '§') {
				if (i < value.length() - 1) {
					i++;
					color = Util.getColor(value.charAt(i), color);
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
			if (index == bits.size()) {
				//clear the sign
				sign.setLine(this.line, "", forplayers);
			} else {
				String line = sign.getRealLine(this.line);
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
				sign.setLine(this.line, line, forplayers);
				index++;
			}
		}
	}
	
	public void update(boolean forced) {
		ArrayList<VirtualSign> signs = getSigns();
		if (signs != null) {
			for (VirtualSign sign : signs) {
				sign.update(forced);
			}
		}
	}
	
	private HashSet<Location> loopCheck = new HashSet<Location>();
	private Block nextSign(Block from) {
		BlockFace face = BlockUtil.getFacing(from);
		switch (face) {
		case NORTH : face = BlockFace.EAST; break;
		case EAST : face = BlockFace.SOUTH; break;
		case SOUTH : face = BlockFace.WEST; break;
		case WEST : face = BlockFace.NORTH; break;
		}
		if (this.direction == Direction.RIGHT) {
			face = face.getOppositeFace();
		}
		Block next = from.getRelative(face);
		if (!BlockUtil.isSign(next)) {
			//Jumping a gap?
			boolean found = false;
			for (BlockFace f : FaceUtil.attachedFacesDown) {
				Block next2 = next.getRelative(f);
				if (BlockUtil.isSign(next2)) {
					next = next2;
					found = true;
					break;
				}
			}
			if (!found) next = null;
		}
		if (next != null && loopCheck.add(next.getLocation()))  {
			return next;
		}
		return null;
	}
	
	private boolean validateSigns() {
		if (prevSigns != null && prevSigns.size() != 0) {
			for (VirtualSign sign : prevSigns) {
				if (!sign.isValid()) {
					sign.remove();
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public Block getStartBlock() {
		World w = Bukkit.getServer().getWorld(this.worldname);
		if (w == null) return null;
		if (!w.isChunkLoaded(x >> 4, z >> 4)) return null;
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
		return this.getSigns(true);
	}
	public ArrayList<VirtualSign> getSigns(boolean validate) {
		if (!validate) {
			return this.prevSigns;
		}
		Block start = getStartBlock();
		//Unloaded chunk?
		if (start == null) {
			if (this.prevSigns != null) {
				this.prevSigns.clear();
				this.prevSigns = null;
			}
			return null;
		}

		if (validateSigns() && !updateSignOrder) {
			return prevSigns;
		}
		updateSignOrder = false;
				
		//Regenerate old signs and return
		if (BlockUtil.isSign(start)) {
			loopCheck.clear();
			prevSigns = new ArrayList<VirtualSign>();
			prevSigns.add(VirtualSign.get(start));
			if (this.direction == Direction.NONE) return prevSigns;
			while (start != null) {
				//Check for next signs
				start = nextSign(start);
				if (start != null) {
					VirtualSign sign = VirtualSign.get(start);
					String realline = sign.getRealLine(this.line);
					int index = realline.indexOf('%');
					if (index != -1) {
						if (prevSigns.size() > 0) {
							//allow?
							if (index == 0 && index == realline.length() - 1) {
								//the only char on the sign - allowed
							} else if (index == 0) {
								//all left - space to the right?
								if (realline.charAt(index + 1) != ' ') break;
							} else if (index == realline.length() - 1) {
								//all right - space to the left?
								if (realline.charAt(index - 1) != ' ') break;
							} else {
								//centered - surrounded by spaces?
								if (realline.charAt(index - 1) != ' ') break;
								if (realline.charAt(index + 1) != ' ') break;
							}
							start = null;
						}
					}
					prevSigns.add(sign);
				} else {
					break;
				}
			}
			if (this.direction == Direction.LEFT) Collections.reverse(prevSigns);
			return prevSigns;
		}
		return null;
	}
		
}

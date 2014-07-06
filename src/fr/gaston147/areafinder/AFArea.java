package fr.gaston147.areafinder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class AFArea {
	public World world;
	public int x, y, z, w, h, l;
	public boolean[] blocks;
	public String name;
	private List<Player> players;
	
	public AFArea(World world, int x, int y, int z, int w, int h, int l, List<Block> bs, String name) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
		this.h = h;
		this.l = l;
		this.name = name;
		players = new ArrayList<Player>();
		
		blocks = new boolean[w * l];
		for (Block block : bs)
			setBlockAt(block.getX() - x, block.getZ() - z, true);
	}
	
	public void setBlockAt(int x, int z, boolean b) {
		blocks[x + z * w] = b;
	}
	
	public boolean getBlockAt(Block b) {
		return getBlockAt(b.getX() - x, b.getZ() - z);
	}
	
	public boolean getBlockAt(int x, int z) {
		return blocks[x + z * w];
	}
	
	public boolean isInside(Block b) {
		return b.getX() >= x && b.getX() < x + w && b.getY() >= y && b.getY() < y + h && b.getZ() >= z && b.getZ() < z + l && getBlockAt(b.getX() - x, b.getZ() - z);
	}
	
	public boolean isInside(Player player) {
		return isInside(player.getLocation().getBlock());
	}
	
	public void checkPlayers() {
		for (Player player : Bukkit.getOnlinePlayers())
			if (isInside(player) && !players.contains(player))
				players.add(player);
		for (Player player : new ArrayList<Player>(players))
			if (!isInside(player))
				players.remove(player);
	}

	public void checkPlayer(Player player) {
		if (isInside(player)) {
			if (!players.contains(player)) {
				onPlayerJoin(player);
				players.add(player);
			}
			onPlayerMove(player);
		} else {
			if (players.contains(player)) {
				onPlayerLeave(player);
				players.remove(player);
			}
		}
	}
	
	public void onPlayerJoin(Player player) {
		player.sendMessage("Welcome to zone " + name + "!");
	}
	
	public void onPlayerLeave(Player player) {
		player.sendMessage("Bye!");
	}

	public void onPlayerPlaceBlock(Player player, Block block) {
	}
	
	public void onPlayerMove(Player player) {
	}
	
	private boolean axisCollide(int x1, int w1, int x2, int w2) {
		return x1 < x2 + w2 && x2 < x1 + w1;
	}
	
	public boolean isCollideWith(AFArea area) {
		if (axisCollide(x, w, area.x, area.w) && axisCollide(y, h, area.y, area.h) && axisCollide(z, l, area.z, area.l)) {
			for (int j = 0; j < l; j++) {
				for (int i = 0; i < w; i++) {
					if (getBlockAt(i, j) && x + i < area.x + area.w && z + j < area.z + area.l && area.getBlockAt(x + i - area.x, z + j - area.z))
						return true;
				}
			}
		}
		return false;
	}
}

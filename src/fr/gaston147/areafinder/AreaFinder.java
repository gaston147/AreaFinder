package fr.gaston147.areafinder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AreaFinder extends JavaPlugin implements Listener {
	private static final Material END_BLOCK_ID = Material.STEP;
	private static final byte END_BLOCK_DATA = 0;
	private static final int MAX_BLOCKS = 100;
	
	private List<AFArea> areas;
	
	public void onEnable() {
		areas = new ArrayList<AFArea>();
		Bukkit.getPluginManager().registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Class<? extends AreaFinder> clazz = getClass();
		for (Method m : clazz.getDeclaredMethods()) {
			AFCommand ann = m.getAnnotation(AFCommand.class);
			if (ann != null && ann.name().equalsIgnoreCase(cmd.getName())) {
				if (args.length >= 1 && args[0].equalsIgnoreCase("usage")) {
					sender.sendMessage(ChatColor.YELLOW + "Usage: /" + ann.name() + " " + ann.usage());
					return true;
				}
				try {
					if (ann.sender() == AFCommandSender.PLAYER && !(sender instanceof Player))
						throw new AFCommandException("you must be a player");
					if (args.length < ann.min())
						throw new AFCommandException("not enough args, usage: " + ann.usage());
					if (args.length > ann.max())
						throw new AFCommandException("too many args, usage: " + ann.usage());
					try {
						m.setAccessible(true);
						m.invoke(this, sender, args);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						if (e.getCause() != null && e.getCause() instanceof AFCommandException) {
							throw (AFCommandException) e.getCause();
						} else {
							e.printStackTrace();
						}
					}
				} catch (AFCommandException e) {
					sender.sendMessage(ChatColor.RED + "Error: " + e.getMessage());
				}
				return true;
			}
		}
		return false;
	}
	
	@AFCommand(name = "findarea", sender = AFCommandSender.PLAYER, min = 2, max = 3, usage = "<s:name> <i:height>[ <b:include floor>]")
	private void findArea(Player player, String[] args) {
		String name = args[0];
		int h;
		try {
			h = Integer.valueOf(args[1]);
		} catch (NumberFormatException e) {
			throw new AFCommandException("'" + args[1] + "' is not a valid number");
		}
		boolean includeFloor = (args.length >= 3 ? Boolean.valueOf(args[2]) : true);
		h += includeFloor ? 1 : 0;
		
		List<Block> blocks = new ArrayList<Block>();
		Block b = player.getLocation().getBlock();
		blocks.add(b);
		findBlocks(blocks, b);
		
		int minX = b.getX(), minZ = b.getZ(), maxX = b.getX(), maxZ = b.getZ();
		for (Block block : blocks) {
			if (block.getX() < minX)
				minX = block.getX();
			if (block.getZ() < minZ)
				minZ = block.getZ();
			if (block.getX() > maxX)
				maxX = block.getX();
			if (block.getZ() > maxZ)
				maxZ = block.getZ();
		}
		
		int w = maxX - minX + 1, l = maxZ - minZ + 1;
		AFArea area = new AFArea(player.getWorld(), minX, b.getY() + (includeFloor ? -1 : 0), minZ, w, h, l, blocks, name);
		for (AFArea ar : areas)
			if (ar.isCollideWith(area))
				throw new AFCommandException("two areas at the same place");
		areas.add(area);
		player.sendMessage(ChatColor.GREEN + "Find " + blocks.size() + " blocks! (" + w + "*" + l + "*" + h + ") area '" + args[0] + "' created!");
		area.checkPlayers();
	}
	
	private static final BlockFace[] faces = new BlockFace[] { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	
	@SuppressWarnings("deprecation")
	private void findBlocks(List<Block> blocks, Block b) {
		if (blocks.size() >= MAX_BLOCKS)
			throw new AFCommandException("too many blocks");
		for (BlockFace face : faces) {
			Block b1 = b.getRelative(face);
			if (!blocks.contains(b1) && (b1.getType() != END_BLOCK_ID || b1.getData() != END_BLOCK_DATA)) {
				blocks.add(b1);
				findBlocks(blocks, b1);
			}
		}
	}
	
	@EventHandler
	private void onPlayerMove(PlayerMoveEvent event) {
		for (AFArea area : areas)
			area.checkPlayer(event.getPlayer());
	}
	
	@EventHandler
	private void onPlayerPlaceBlock(BlockPlaceEvent event) {
		for (AFArea area : areas)
			if (area.isInside(event.getBlock()))
				area.onPlayerPlaceBlock(event.getPlayer(), event.getBlock());
	}
}

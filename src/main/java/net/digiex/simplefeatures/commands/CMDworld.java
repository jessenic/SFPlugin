package net.digiex.simplefeatures.commands;

import net.digiex.simplefeatures.SFPlugin;
import net.digiex.simplefeatures.teleports.SFTeleportTask;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDworld implements CommandExecutor {

	SFPlugin plugin;

	public CMDworld(SFPlugin parent) {
		this.plugin = parent;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (SFTeleportTask.teleporters.containsKey(player.getName())) {
				player.sendMessage(ChatColor.GRAY
						+ "Teleport already in progress, use /abort to cancel.");
				return true;
			}
			if (args.length > 0) {
				World world = null;
				for (World w : plugin.getServer().getWorlds()) {
					String wname = w.getName();
					if (w.getName().contains("_nether")) {
						wname = "Nether";
					}
					if (wname.equalsIgnoreCase(args[0])) {
						world = w;
					} else if (wname.toLowerCase().indexOf(
							args[0].toLowerCase()) != -1) {
						world = w;
					}
				}
				if (world != null) {
					if (world.getName().contains("_nether")) {
						if (!((Player) sender).isOp()) {
							sender.sendMessage(ChatColor.RED
									+ "Use a Nether Portal in Survival to enter Nether");
							return true;
						} else {
							sender.sendMessage("Wait! You need to use nether portals!!! Oh you're an OP... Sorry, my mistake.");
						}
					}
					int taskId = plugin
							.getServer()
							.getScheduler()
							.scheduleAsyncDelayedTask(
									plugin,
									new SFTeleportTask(player, player, null,
											world.getSpawnLocation(), false,
											null, "Teleporting to "
													+ world.getName()));
					SFTeleportTask.teleporters.put(player.getName(), taskId);
					return true;

				}
				ListWorlds(sender, args[0]);
				return true;
			}
			ListWorlds(sender, "");
			return true;
		}
		return false;
	}

	private void ListWorlds(CommandSender sender, String tried) {
		if (tried.length() > 0) {
			sender.sendMessage(ChatColor.RED + "World \"" + tried
					+ "\" was not found. Check Spelling.");
		}
		sender.sendMessage(ChatColor.GREEN + "Available worlds:");
		for (World w : plugin.getServer().getWorlds()) {
			if (w.getName().contains("_nether")) {
				boolean allownether = false;
				if (sender instanceof Player) {
					allownether = ((Player) sender).isOp();
				} else {
					allownether = true;
				}
				if (allownether) {
					sender.sendMessage(ChatColor.GRAY + "Nether");
				}
			} else {
				sender.sendMessage(ChatColor.YELLOW + w.getName());
			}
		}
	}
}

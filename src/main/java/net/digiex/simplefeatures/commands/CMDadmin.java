package net.digiex.simplefeatures.commands;

import net.digiex.simplefeatures.SFPlayer;
import net.digiex.simplefeatures.SFPlugin;
import net.digiex.simplefeatures.listeners.PListener;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CMDadmin implements CommandExecutor {
	SFPlugin plugin;

	public CMDadmin(SFPlugin parent) {
		plugin = parent;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (sender instanceof Player) {
			Player p = (Player) sender;
			SFPlayer sfp = new SFPlayer(p, plugin);

			if (sfp.isAdmin()) {
				p.setOp(!p.isOp());
				PListener.updatePlayerNameColour(p, plugin);
				if (p.isOp()) {
					p.sendMessage(ChatColor.YELLOW + "Admin mode turned on!");
					plugin.getServer().broadcastMessage(
							p.getDisplayName() + ChatColor.GRAY
									+ " has enabled operator privileges!");
					p.getWorld().strikeLightningEffect(p.getLocation());
				} else {
					p.sendMessage(ChatColor.YELLOW + "Admin mode turned off!");
					plugin.getServer().broadcastMessage(
							p.getDisplayName() + ChatColor.GRAY
									+ " has disabled operator privileges!");
					p.getWorld().strikeLightningEffect(p.getLocation());
				}
				return true;
			} else {
				sender.sendMessage(ChatColor.RED
						+ "You are not allowed to do that, sir!");
				return true;
			}
		}
		return false;
	}
}

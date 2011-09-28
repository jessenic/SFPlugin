package net.digiex.simplefeatures.commands;

import net.digiex.simplefeatures.SFPlugin;
import net.digiex.simplefeatures.TeleportTask;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

public class CMDtpahere implements CommandExecutor {

    SFPlugin plugin;

    public CMDtpahere(SFPlugin parent) {
        this.plugin = parent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                Player to = plugin.getPlayer(sender, args[0]);
                if (plugin.teleporters.containsKey(to.getName())) {
                    if (to.hasPermission(new Permission("sf.tpoverride", PermissionDefault.OP))) {
                        TeleportTask task = plugin.teleporters.get(to.getName());
                        int id = task.getId();
                        plugin.getServer().getScheduler().cancelTask(id);
                    } else {
                        player.sendMessage(ChatColor.GRAY + to.getName() + " cannot teleport this quickly, he/she must learn to walk");
                        return true;
                    }
                }
                if (to != null) {
                    TeleportTask task = new TeleportTask(plugin, player, to, null, null, false, true, false, false);
                    int id = plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, task);
                    task.setId(id);
                    plugin.teleporters.put(to.getName(), task);
                    player.sendMessage(ChatColor.GRAY + "Requesting!");
                    return true;
                }
            }
        }
        return false;
    }
}
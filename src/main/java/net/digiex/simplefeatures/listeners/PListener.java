package net.digiex.simplefeatures.listeners;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import net.digiex.simplefeatures.SFHome;
import net.digiex.simplefeatures.SFInventory;
import net.digiex.simplefeatures.SFLocation;
import net.digiex.simplefeatures.SFMail;
import net.digiex.simplefeatures.SFPlugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitScheduler;

public class PListener extends PlayerListener {

	private class AskSetHomeTask implements Runnable {
		private final Player player;
		private final Location homeLoc;

		public AskSetHomeTask(Player player, Location homeLoc) {
			this.player = player;
			this.homeLoc = homeLoc;
		}

		@Override
		public void run() {
			String answer = SFPlugin.questioner.ask(player, ChatColor.YELLOW
					+ "Do you want to set your home to this bed?", "set",
					"cancel");
			if (answer == "set") {

				com.avaje.ebean.EbeanServer db = plugin.getDatabase();
				db.beginTransaction();

				try {
					SFHome home = db
							.find(SFHome.class)
							.where()
							.ieq("worldName",
									player.getLocation().getWorld().getName())
							.ieq("playerName", player.getName()).findUnique();
					boolean isUpdate = false;

					if (home == null) {
						player.sendMessage(ChatColor.YELLOW
								+ "Home for this world created!");

						home = new SFHome();
						home.setPlayer(player);
					} else {

						player.sendMessage(ChatColor.YELLOW
								+ "Home for this world updated!");

						isUpdate = true;
					}

					home.setLocation(homeLoc);

					if (isUpdate) {
						db.update(home, updateProps);
					}
					db.save(home);
					db.commitTransaction();
				} finally {
					db.endTransaction();
				}
			} else {
				player.sendMessage(ChatColor.GRAY
						+ "Setting home here cancelled.");
			}
			homeTasks.remove(player.getName());
		}
	}

	SFPlugin plugin;

	public BukkitScheduler tasks;

	public static HashMap<String, Integer> homeTasks = new HashMap<String, Integer>();

	private static final Set<String> updateProps;
	static {
		updateProps = new HashSet<String>();
		updateProps.add("x");
		updateProps.add("y");
		updateProps.add("z");
		updateProps.add("yaw");
		updateProps.add("pitch");
		updateProps.add("world_name");
	}

	public PListener(SFPlugin parent) {
		plugin = parent;
	}

	@Override
	public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
		if (e.isCancelled()) {
			return;
		}
		SFPlugin.log(Level.INFO, e.getPlayer().getName()
				+ "'s gamemode changed to " + e.getNewGameMode().toString());
		if (!(e.getPlayer().getHealth() > 0)) {
			e.getPlayer().getInventory().clear();
			e.getPlayer().setHealth(20);
			e.getPlayer().setFoodLevel(20);
		}

		SFInventory inv = new SFInventory();
		inv.setGameMode(e.getPlayer().getGameMode());
		inv.setPlayerName(e.getPlayer().getName());
		inv.setInventory(SFPlugin.itemStackToString(e.getPlayer()
				.getInventory().getContents()));
		inv.setArmor(SFPlugin.itemStackToString(e.getPlayer().getInventory()
				.getArmorContents()));
		inv.setHealth(e.getPlayer().getHealth());
		inv.setFood(e.getPlayer().getFoodLevel());
		inv.setExperience(e.getPlayer().getExperience());
		inv.setExhaustion(e.getPlayer().getExhaustion());
		inv.setFireTicks(e.getPlayer().getFireTicks());
		inv.setLevel(e.getPlayer().getLevel());
		inv.setRemainingAir(e.getPlayer().getRemainingAir());
		inv.setSaturation(e.getPlayer().getSaturation());
		inv.setTotalExperience(e.getPlayer().getTotalExperience());
		plugin.updateSFInventory(inv);

		e.getPlayer().getInventory().clear();
		try {
			if (!(inv.getHealth() > 0)) {
				e.getPlayer().setHealth(20);
				e.getPlayer().setFoodLevel(20);
			} else {
				inv = plugin.getSFInventory(e.getNewGameMode(), e.getPlayer()
						.getName());
				ItemStack[] contents = SFPlugin.stringToItemStack(inv
						.getInventory());
				if (contents != null) {
					e.getPlayer().getInventory().setContents(contents);
				}
				ItemStack[] armor = SFPlugin.stringToItemStack(inv.getArmor());
				if (armor != null) {
					e.getPlayer().getInventory().setArmorContents(armor);
				}
				e.getPlayer().setHealth(inv.getHealth());
				e.getPlayer().setFoodLevel(inv.getFood());
				e.getPlayer().setExperience(inv.getExperience());
				e.getPlayer().setExhaustion(inv.getExhaustion());
				e.getPlayer().setFireTicks(inv.getFireTicks());
				e.getPlayer().setLevel(inv.getLevel());
				e.getPlayer().setRemainingAir(inv.getRemainingAir());
				e.getPlayer().setSaturation(inv.getSaturation());
				e.getPlayer().setTotalExperience(inv.getTotalExperience());
			}
		} catch (NullPointerException ex) {
			SFPlugin.log(Level.INFO, "Some inventory contents were null for "
					+ e.getPlayer().getName());
			// ex.printStackTrace();
		}
	}

	@Override
	public void onPlayerInteract(final PlayerInteractEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if (event.getClickedBlock().getType() == Material.BED_BLOCK) {
			Player player = event.getPlayer();
			if (homeTasks.containsKey(player.getName())) {
				plugin.getServer().getScheduler()
						.cancelTask(homeTasks.get(player.getName()));
			}
			int taskId = plugin
					.getServer()
					.getScheduler()
					.scheduleAsyncDelayedTask(plugin,
							new AskSetHomeTask(player, player.getLocation()));
			homeTasks.put(player.getName(), taskId);
		}
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent e) {

		PermissionAttachment attachment = e.getPlayer().addAttachment(plugin);
		if (!e.getPlayer().isOp()) {
			attachment.setPermission("bukkit.command.plugins", false);
			attachment.setPermission("bukkit.command.version", false);
		}
		plugin.permissionAttachements.put(e.getPlayer().getName(), attachment);
		setGameMode(e.getPlayer(), e.getPlayer().getWorld());
		if (e.getPlayer().isOp()) {
			e.getPlayer().setDisplayName(
					ChatColor.AQUA + e.getPlayer().getName() + ChatColor.WHITE);
		} else {
			e.getPlayer()
					.setDisplayName(
							ChatColor.GREEN + e.getPlayer().getName()
									+ ChatColor.WHITE);
		}
		String plistname = e.getPlayer().getDisplayName();
		if (plistname.length() < 17) {
			e.getPlayer().setPlayerListName(plistname);
		}
		List<SFMail> msgs;
		msgs = plugin.getDatabase().find(SFMail.class).where()
				.ieq("toPlayer", e.getPlayer().getName()).findList();
		if (!msgs.isEmpty()) {
			e.getPlayer().sendMessage(
					ChatColor.AQUA + "You have " + msgs.size()
							+ " new mail! Type /read to view.");
		}
	}

	@Override
	public void onPlayerKick(PlayerKickEvent e) {
		System.out
				.println(e.getPlayer().getName() + " lost connection: kicked");
		if (!e.getPlayer().isWhitelisted()) {
			e.setLeaveMessage(null);
		}
		if (plugin.permissionAttachements.containsKey(e.getPlayer().getName())) {
			e.getPlayer().removeAttachment(
					plugin.permissionAttachements.get(e.getPlayer().getName()));
			plugin.permissionAttachements.remove(e.getPlayer().getName());
		}
	}

	@Override
	public void onPlayerPortal(PlayerPortalEvent e) {
		if (!(e.isCancelled()) && e.getTo() != null) {
			Teleported(e.getFrom().getWorld(), e.getTo().getWorld(),
					e.getPlayer());
		}
	}

	@Override
	public void onPlayerPreLogin(PlayerPreLoginEvent e) {
		if (!plugin.getServer().getOfflinePlayer(e.getName()).isWhitelisted()) {
			plugin.getServer().broadcastMessage(
					ChatColor.YELLOW + e.getName()
							+ " tried to join, but is not on whitelist!");
			e.disallow(Result.KICK_WHITELIST, ChatColor.RED
					+ "Not on whitelist, " + ChatColor.WHITE + " see "
					+ ChatColor.AQUA + "http://digiex.net/minecraft");
			return;
		}
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (plugin.permissionAttachements.containsKey(e.getPlayer().getName())) {
			e.getPlayer().removeAttachment(
					plugin.permissionAttachements.get(e.getPlayer().getName()));
			plugin.permissionAttachements.remove(e.getPlayer().getName());
		}
	}

	@Override
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		String wname = event.getPlayer().getLocation().getWorld().getName();
		if (wname.contains("_nether") || wname.contains("_skylands")) {
			wname = "Survival";
		}
		SFHome home = plugin.getDatabase().find(SFHome.class).where()
				.ieq("worldName", wname)
				.ieq("playerName", event.getPlayer().getName()).findUnique();
		if (home != null) {
			event.setRespawnLocation(home.getLocation());
		}
		Teleported(event.getPlayer().getWorld(), event.getRespawnLocation()
				.getWorld(), event.getPlayer());
	}

	@Override
	public void onPlayerTeleport(PlayerTeleportEvent e) {
		if (!(e.isCancelled()) && e.getTo() != null) {
			e.getPlayer().setNoDamageTicks(200);
			Teleported(e.getFrom().getWorld(), e.getTo().getWorld(),
					e.getPlayer());
		}
	}

	public void setGameMode(Player player, World world) {
		int gamemode = plugin.config.getInt("worlds." + world.getName()
				+ ".gamemode", 5);
		if (gamemode == 1) {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.setGameMode(GameMode.CREATIVE);
				SFPlugin.log(Level.INFO, "Gamemode set to creative for "
						+ player.getName());
			}
		} else if (gamemode == 0) {
			if (player.getGameMode() != GameMode.SURVIVAL) {
				player.setGameMode(org.bukkit.GameMode.SURVIVAL);

				SFPlugin.log(Level.INFO, "Gamemode set to survival for "
						+ player.getName());
			}
		} else {
			if (player.getGameMode() != plugin.getServer().getDefaultGameMode()) {
				player.setGameMode(plugin.getServer().getDefaultGameMode());
				SFPlugin.log(Level.INFO, "Gamemode set to default ("
						+ plugin.getServer().getDefaultGameMode().toString()
						+ ") for " + player.getName());
			}
		}
	}

	public void Teleported(World from, World to, Player player) {
		if (from != to) {
			setGameMode(player, to);
			SFPlugin.log(Level.INFO, player.getName() + " teleported from "
					+ from.getName() + " to " + to.getName());
			com.avaje.ebean.EbeanServer db = plugin.getDatabase();
			db.beginTransaction();

			try {
				SFLocation lastLoc = db.find(SFLocation.class).where()
						.ieq("worldName", from.getName())
						.ieq("playerName", player.getName()).findUnique();
				boolean isUpdate = false;

				if (lastLoc == null) {
					lastLoc = new SFLocation();
					lastLoc.setPlayerName(player.getName());
				} else {
					isUpdate = true;
				}
				Location loc = player.getLocation();
				lastLoc.setX(loc.getX());
				lastLoc.setY(loc.getY());
				lastLoc.setZ(loc.getZ());
				lastLoc.setYaw(loc.getYaw());
				lastLoc.setPitch(loc.getPitch());
				lastLoc.setWorldName(loc.getWorld().getName());
				if (isUpdate) {
					db.update(lastLoc, updateProps);
				}
				db.save(lastLoc);
				db.commitTransaction();
			} finally {
				db.endTransaction();
				SFPlugin.log(Level.INFO, "Last location for world "
						+ player.getWorld().getName() + " saved");
			}
		}
	}
}
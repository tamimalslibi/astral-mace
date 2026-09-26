package com.astralmace.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AstralMacePlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 15000; // 15 seconds
    private static final String ASTRAL_MACE_NAME = ChatColor.BLUE + "⚡ " + ChatColor.BOLD + "ASTRAL MACE";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Astral Mace Plugin has been enabled!");

        getCommand("astralmace").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return false;
            }

            Player player = (Player) sender;

            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You must be a server operator to use this command!");
                return false;
            }

            player.getInventory().addItem(createAstralMace());
            player.sendMessage(ChatColor.GREEN + "You received the Astral Mace!");
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("Astral Mace Plugin has been disabled!");
        cooldowns.clear();
    }

    private ItemStack createAstralMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ASTRAL_MACE_NAME);

            meta.addEnchant(Enchantment.DENSITY, 5, true);
            meta.addEnchant(Enchantment.WIND_BURST, 2, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);

            meta.setLore(Arrays.asList(
                ChatColor.GOLD + "A MACE THAT LAUNCHES PLAYERS SKYWARD.",
                "",
                ChatColor.BLUE + "⚔ " + ChatColor.BOLD + "Ability: " + ChatColor.WHITE + "Shift + Right Click",
                ChatColor.GRAY + "Launch the nearest player straight up,",
                ChatColor.GRAY + "then bring them back down.",
                "",
                ChatColor.BLUE + "⏱ " + ChatColor.BOLD + "Cooldown: " + ChatColor.WHITE + "15 Seconds",
                ChatColor.BLUE + "♦ " + ChatColor.BOLD + "UNBREAKABLE"
            ));

            mace.setItemMeta(meta);
        }

        return mace;
    }

    private boolean isAstralMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }

        return meta.getDisplayName().equals(ASTRAL_MACE_NAME);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isAstralMace(item)) {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (cooldowns.containsKey(playerId)) {
            long timeLeft = (cooldowns.get(playerId) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                player.sendMessage(ChatColor.RED + "Astral Mace is on cooldown! " + timeLeft + " seconds remaining.");
                return;
            }
            cooldowns.remove(playerId);
        }

        Player target = findNearestPlayer(player);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No players nearby!");
            return;
        }

        cooldowns.put(playerId, System.currentTimeMillis());
        player.sendMessage(ChatColor.YELLOW + "Astral Mace used! 15 second cooldown started.");

        launchPlayerVertically(target, player);
    }

    private Player findNearestPlayer(Player source) {
        Player nearest = null;
        double minDistance = 10.0;

        for (Player player : source.getWorld().getPlayers()) {
            if (player.equals(source) || !player.isValid()) {
                continue;
            }

            double distance = source.getLocation().distance(player.getLocation());
            if (distance < minDistance) {
                minDistance = distance;
                nearest = player;
            }
        }

        return nearest;
    }

    private void launchPlayerVertically(Player target, Player attacker) {
        final Location startLocation = target.getLocation().clone();
        final double lockedX = startLocation.getX();
        final double lockedZ = startLocation.getZ();

        Vector upwardVelocity = new Vector(0, 2.5, 0);
        target.setVelocity(upwardVelocity);

        target.sendMessage(ChatColor.BLUE + "You were launched by " + attacker.getName() + "'s Astral Mace!");
        attacker.sendMessage(ChatColor.GREEN + "You launched " + target.getName() + " into the sky!");

        new BukkitRunnable() {
            int ticksRun = 0;
            final int maxUpwardTicks = 30;
            final int totalTicks = 80;
            boolean descendPhase = false;

            @Override
            public void run() {
                if (!target.isOnline() || !target.isValid()) {
                    cancel();
                    return;
                }

                Location currentLoc = target.getLocation();
                if (Math.abs(currentLoc.getX() - lockedX) > 0.3 || Math.abs(currentLoc.getZ() - lockedZ) > 0.3) {
                    currentLoc.setX(lockedX);
                    currentLoc.setZ(lockedZ);
                    target.teleport(currentLoc);
                }

                if (ticksRun >= maxUpwardTicks && !descendPhase) {
                    descendPhase = true;
                    target.setVelocity(new Vector(0, -2.5, 0));
                }

                if (descendPhase) {
                    Vector currentVelocity = target.getVelocity();
                    target.setVelocity(new Vector(0, Math.min(currentVelocity.getY(), -1.5), 0));
                }

                if ((descendPhase && target.isOnGround()) || ticksRun >= totalTicks) {
                    Location finalLoc = target.getLocation();
                    finalLoc.setX(lockedX);
                    finalLoc.setZ(lockedZ);
                    target.teleport(finalLoc);

                    target.sendMessage(ChatColor.GRAY + "You returned to the ground.");
                    cancel();
                    return;
                }

                ticksRun++;
            }
        }.runTaskTimer(this, 1L, 1L);
    }
}

package com.astralmace.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
    private static final long COOLDOWN_TIME = 15000; // 15 seconds in milliseconds
    private static final String ASTRAL_MACE_NAME = ChatColor.BLUE + "⚡ " + ChatColor.BOLD + "ASTRAL MACE";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Astral Mace Plugin has been enabled!");

        // Register command to get the Astral Mace - ONLY FOR OPS
        getCommand("astralmace").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                return false;
            }

            Player player = (Player) sender;

            // Check if player is OP
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You must be a server operator to use this command!");
                return false;
            }

            // Give Astral Mace to OP
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

    /**
     * Creates the Astral Mace item with enchantments and lore
     */
    private ItemStack createAstralMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            // Set the blue bold name
            meta.setDisplayName(ASTRAL_MACE_NAME);

            // Add enchantments
            meta.addEnchant(Enchantment.DENSITY, 5, true);
            meta.addEnchant(Enchantment.WIND_BURST, 2, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);

            // Set lore
            meta.setLore(Arrays.asList(
                ChatColor.GOLD + "A MACE THAT LAUNCHES PLAYERS SKYWARD.",
                "",
                ChatColor.BLUE + "⚔ " + ChatColor.BOLD + "Ability: " + ChatColor.WHITE + "Strike",
                ChatColor.GRAY + "Launch hit players straight up,",
                ChatColor.GRAY + "then bring them back down.",
                "",
                ChatColor.BLUE + "⏱ " + ChatColor.BOLD + "Cooldown: " + ChatColor.WHITE + "15 Seconds",
                ChatColor.BLUE + "♦ " + ChatColor.BOLD + "UNBREAKABLE"
            ));

            mace.setItemMeta(meta);
        }

        return mace;
    }

    /**
     * Checks if an item is the Astral Mace
     */
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

    /**
     * Handles entity damage - for Astral Mace hits
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if the damaged entity is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Check if the damager is a player
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player target = (Player) event.getEntity();
        ItemStack itemInHand = attacker.getInventory().getItemInMainHand();

        // Check if attacking with Astral Mace
        if (!isAstralMace(itemInHand)) {
            return;
        }

        UUID attackerId = attacker.getUniqueId();

        // Check cooldown
        if (cooldowns.containsKey(attackerId)) {
            long timeLeft = (cooldowns.get(attackerId) + COOLDOWN_TIME - System.currentTimeMillis()) / 1000;
            if (timeLeft > 0) {
                attacker.sendMessage(ChatColor.RED + "Astral Mace is on cooldown! " + timeLeft + " seconds remaining.");
                return;
            }
            cooldowns.remove(attackerId);
        }

        // Don't launch yourself
        if (target.getUniqueId().equals(attackerId)) {
            return;
        }

        // Apply cooldown immediately
        cooldowns.put(attackerId, System.currentTimeMillis());
        attacker.sendMessage(ChatColor.YELLOW + "Astral Mace used! 15 second cooldown started.");

        // Start the vertical launch effect
        launchPlayerVertically(target, attacker);
    }

    /**
     * Launches the target player straight up, then brings them straight back down
     */
    private void launchPlayerVertically(Player target, Player attacker) {
        // Save the exact X and Z position to keep them locked
        final Location startLocation = target.getLocation().clone();
        final double lockedX = startLocation.getX();
        final double lockedZ = startLocation.getZ();

        // Launch phase: Send player straight up
        Vector upwardVelocity = new Vector(0, 2.5, 0); // Strong upward velocity
        target.setVelocity(upwardVelocity);

        target.sendMessage(ChatColor.BLUE + "You were launched by " + attacker.getName() + "'s Astral Mace!");
        attacker.sendMessage(ChatColor.GREEN + "You launched " + target.getName() + " into the sky!");

        // Task to keep player vertically aligned and bring them back down
        new BukkitRunnable() {
            int ticksRun = 0;
            final int maxUpwardTicks = 30; // ~1.5 seconds going up
            final int totalTicks = 80; // ~4 seconds total (up + down)
            boolean descendPhase = false;

            @Override
            public void run() {
                // Stop if player is offline or dead
                if (!target.isOnline() || !target.isValid()) {
                    cancel();
                    return;
                }

                // Lock horizontal position throughout the entire effect
                Location currentLoc = target.getLocation();
                if (Math.abs(currentLoc.getX() - lockedX) > 0.3 || Math.abs(currentLoc.getZ() - lockedZ) > 0.3) {
                    // Player drifted horizontally, teleport them back
                    currentLoc.setX(lockedX);
                    currentLoc.setZ(lockedZ);
                    target.teleport(currentLoc);
                }

                // Switch to descend phase after upward phase
                if (ticksRun >= maxUpwardTicks && !descendPhase) {
                    descendPhase = true;
                    // Apply strong downward velocity
                    target.setVelocity(new Vector(0, -2.5, 0));
                }

                // Continue applying downward force during descend phase
                if (descendPhase) {
                    Vector currentVelocity = target.getVelocity();
                    // Keep pushing them down, but lock horizontal movement
                    target.setVelocity(new Vector(0, Math.min(currentVelocity.getY(), -1.5), 0));
                }

                // Stop when they're back near ground or time runs out
                if ((descendPhase && target.isOnGround()) || ticksRun >= totalTicks) {
                    // Final position lock
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
        }.runTaskTimer(this, 1L, 1L); // Start after 1 tick, run every tick
    }
}

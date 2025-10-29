package org.akorpuzz.artifacts.listener;

import org.akorpuzz.artifacts.Features.TotemManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class TotemPlacementListener implements Listener {
    private final TotemManager manager;
    private final JavaPlugin plugin;
    private final Material totemMaterial;
    private final String pdcKey;

    public TotemPlacementListener(JavaPlugin plugin, TotemManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.totemMaterial = Material.valueOf(plugin.getConfig().getString("totem.material", "STRIPPED_OAK_LOG"));
        this.pdcKey = plugin.getConfig().getString("totem.pdc_key", "crop_totem_tier");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (b.getType() != totemMaterial) return;
        // Decide tier: if player is holding a specific item variant you can check its meta;
        // default to tier 1 or read from item meta:
        int tier = 1;
        ItemStack hand = e.getItemInHand();
        if (hand != null && hand.hasItemMeta()) {
            String s = hand.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, pdcKey), PersistentDataType.STRING);
            if (s != null) {
                try { tier = Integer.parseInt(s); } catch (Throwable ignored) {}
            }
        }
        manager.registerTotem(b.getLocation(), tier);
        // small particle
        b.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, b.getLocation().add(0.5,1,0.5), 6, 0.2, 0.2, 0.2, 0.02);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != totemMaterial) return;
        manager.unregisterTotem(b.getLocation());
        b.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, b.getLocation().add(0.5,1,0.5), 6, 0.2, 0.2, 0.2, 0.02);
    }
}
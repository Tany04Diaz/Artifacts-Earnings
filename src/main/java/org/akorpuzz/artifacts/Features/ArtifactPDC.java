package org.akorpuzz.artifacts.Features;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ArtifactPDC {
    private final JavaPlugin plugin;

    public ArtifactPDC(JavaPlugin plugin) { this.plugin = plugin; }

    public NamespacedKey keyUses() {
        return new NamespacedKey(plugin, "uses");
    }

    public int getUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        Integer u = item.getItemMeta().getPersistentDataContainer().get(keyUses(), PersistentDataType.INTEGER);
        return u == null ? 0 : u;
    }

    public void setUses(ItemStack item, int uses) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keyUses(), PersistentDataType.INTEGER, uses);
        item.setItemMeta(meta);
    }
}
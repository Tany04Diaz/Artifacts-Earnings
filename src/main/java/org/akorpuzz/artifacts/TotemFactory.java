package org.akorpuzz.artifacts;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class TotemFactory {
    public static final String PDC_KEY = "crop_totem_tier";

    public static ItemStack createTotem(JavaPlugin plugin, int tier) {
        Material mat = Material.valueOf(plugin.getConfig().getString("totem.material", "STRIPPED_OAK_LOG"));
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        String display = ChatColor.GOLD + "Crop Growth Totem " + ChatColor.DARK_RED + "(Tier " + tier + ")";
        meta.setDisplayName(display);

        meta.setLore(List.of(
                ChatColor.GRAY + "Aumenta la velocidad de crecimiento de cultivos en su radio.",
                ChatColor.GRAY + "Tier: " + ChatColor.YELLOW + tier
        ));

        // Guardar el tier en PDC para que TotemPlacementListener lo lea al colocar
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING, String.valueOf(tier));
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack createTotemTier1(JavaPlugin plugin) { return createTotem(plugin, 1); }
    public static ItemStack createTotemTier2(JavaPlugin plugin) { return createTotem(plugin, 2); }
    public static ItemStack createTotemTier3(JavaPlugin plugin) { return createTotem(plugin, 3); }
}
package org.akorpuzz.artifacts.Features;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class SellStickFactory {
    public static final String ID = "sellstick";
    public static final NamespacedKey KEY_ID(JavaPlugin plugin) { return new NamespacedKey(plugin, ID + "_id"); }
    public static final NamespacedKey KEY_USES(JavaPlugin plugin) { return new NamespacedKey(plugin, ID + "_uses"); }

    /**
     * Crea una SellStick con los usos indicados.
     * @param uses iniciales
     * @param plugin instancia del plugin
     * @param displaySuffix texto corto que identifique la variante (p.e. "10 usos")
     */
    public static ItemStack createSellStick(int uses, JavaPlugin plugin, String displaySuffix) {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta == null) return stick;

        // id en PDC para identificar el item
        meta.getPersistentDataContainer().set(KEY_ID(plugin), PersistentDataType.STRING, ID);

        // usos en PDC
        meta.getPersistentDataContainer().set(KEY_USES(plugin), PersistentDataType.INTEGER, uses);

        // displayName y lore desde config si existe, si no plantilla por defecto
        String template = plugin.getConfig().getString("sellstick.displayName", "&6SellStick &7- &e%uses% usos");
        String display = ChatColor.translateAlternateColorCodes('&', template).replace("%uses%", String.valueOf(uses));
        if (displaySuffix != null && !displaySuffix.isEmpty()) display = display + " " + ChatColor.GRAY + displaySuffix;
        meta.setDisplayName(display);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Usos restantes: " + ChatColor.YELLOW + uses);
        lore.add(ChatColor.GRAY + "Right-click chest to sell contents.");
        meta.setLore(lore);

        stick.setItemMeta(meta);
        return stick;
    }

    // Convenience: three predefined variants
    public static ItemStack createSellStick10(JavaPlugin plugin) {
        return createSellStick(10, plugin, "(10)");
    }
    public static ItemStack createSellStick25(JavaPlugin plugin) {
        return createSellStick(25, plugin, "(25)");
    }
    public static ItemStack createSellStick50(JavaPlugin plugin) {
        return createSellStick(50, plugin, "(50)");
    }
}
package org.akorpuzz.artifacts.Features;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum HarvesterMode {
    COLLECT,
    SELL;

    public static HarvesterMode fromString(@Nullable String s, HarvesterMode def) {
        if (s == null) return def;
        try { return HarvesterMode.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Throwable ignored) { return def; }
    }

    /**
     * Lee el modo desde PDC (keyMode) o deduce por nombre. Devuelve defaultMode si no hay dato.
     */
    public static HarvesterMode getMode(ItemStack item, NamespacedKey keyMode, HarvesterMode defaultMode) {
        if (item == null || !item.hasItemMeta()) return defaultMode;
        var meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(keyMode, PersistentDataType.STRING)) {
            String s = meta.getPersistentDataContainer().get(keyMode, PersistentDataType.STRING);
            return fromString(s, defaultMode);
        }
        String name = meta.getDisplayName();
        if (name != null && name.toLowerCase(Locale.ROOT).contains("sell")) return SELL;
        return defaultMode;
    }
}
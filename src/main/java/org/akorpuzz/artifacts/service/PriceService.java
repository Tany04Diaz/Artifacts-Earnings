package org.akorpuzz.artifacts.service;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class PriceService {
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);

    public PriceService(JavaPlugin plugin) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("prices");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase(Locale.ROOT));
                    double v = sec.getDouble(key, -1);
                    if (v >= 0) prices.put(mat, v);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public @Nullable Double getPrice(Material material) {
        if (material == null) return null;
        return prices.get(material);
    }
}
package org.akorpuzz.artifacts;

import org.akorpuzz.artifacts.Features.ArtifactPDC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ArtifactManager {
    //crea TestStick
    private static final ItemStack TEST_STICK;
    static {
        ItemStack stick = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Test Stick");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Item de Prueba");
            meta.setLore(lore);
            meta.addEnchant(Enchantment.SHARPNESS,999,true);
            stick.setItemMeta(meta);
        }
        TEST_STICK = stick;
    }

    private ArtifactManager() {}
    //devuelve un clon
    public static ItemStack getTestStick() {
        return TEST_STICK.clone();
    }
    //crea Harvester Hoe

    public static ItemStack HARVESTER_HOE(int initialUses, JavaPlugin plugin) {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            NamespacedKey keyUses = new NamespacedKey(plugin, "artifact_uses");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("harvester.displayName", "&6Harvester Hoe &7- &e%uses% usos")).replace("%uses%", String.valueOf(initialUses)));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Usos restantes: " + initialUses);
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING,5,true);
            meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, initialUses);
            hoe.setItemMeta(meta);
        }
        return hoe;
    }


    public static void updateDisplayNameUses(ItemStack item, int uses) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Harvester Hoe [" + ChatColor.GRAY + uses + ChatColor.YELLOW + "] usos restantes");
        item.setItemMeta(meta);
    }

    public static void updateLoreUses(ItemStack item, int uses) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        String usesLine = ChatColor.YELLOW + "Usos: " + ChatColor.GRAY + ChatColor.BOLD + uses;

        if (lore.size() >= 4) {
            lore.set(3, usesLine); // reemplaza la línea 4 si existe
        } else {
            while (lore.size() < 3) lore.add("");
            lore.add(usesLine);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static void updateHoeVisuals(ItemStack item, int uses) {
        updateDisplayNameUses(item, uses);
        updateLoreUses(item, uses);
    }


    //crea True Harvester Hoe
    private static final ItemStack TRUE_HARVESTER_HOE;
    static {
        ItemStack hoe = new ItemStack(Material.NETHERITE_HOE);
        ItemMeta meta = hoe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Harvester Hoe Verdadera");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Vende o recolecta tus cultivos con");
            lore.add(ChatColor.YELLOW +"lo mas nuevo en tecnoloia minecraftiana");
            lore.add(ChatColor.YELLOW + "Irrompible");
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING, 10, true);
            meta.setUnbreakable(true);
            hoe.setItemMeta(meta);
        }
        TRUE_HARVESTER_HOE=hoe;
    }
    public static final ItemStack getTrueHarvesterHoe(){
        return TRUE_HARVESTER_HOE.clone();
    }

}


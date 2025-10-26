package org.akorpuzz.artifacts;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ArtifactManager {
    //crea TestStick
    private static final ItemStack TEST_STICK;
    static {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "Test Stick");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Item de Prueba");
            meta.setLore(lore);
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
    private static final ItemStack HARVESTER_HOE;
    static {
        ItemStack hoe = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta= hoe.getItemMeta();
        if(meta != null){
            meta.setDisplayName(ChatColor.YELLOW+"Harvester Hoe");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW+"Vende o recolecta tus cultivos ");
            lore.add(ChatColor.YELLOW +"lo mas nuevo en tecnoloia minecraftiana");
            meta.setLore(lore);
            meta.addEnchant(Enchantment.UNBREAKING,5,true);
            hoe.setItemMeta(meta);
        }
        HARVESTER_HOE = hoe;
    }
    public static final ItemStack getHarvesterHoe(){
        return HARVESTER_HOE.clone();
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
            meta.isUnbreakable();
            hoe.setItemMeta(meta);
        }
        TRUE_HARVESTER_HOE=hoe;
    }
    public static final ItemStack getTrueHarvesterHoe(){
        return TRUE_HARVESTER_HOE.clone();
    }
}
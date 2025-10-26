package org.akorpuzz.artifacts.commands;

import org.akorpuzz.artifacts.ArtifactManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


import java.util.ArrayList;
import java.util.*;

public class GuiCommand implements CommandExecutor{
    @Override
    public boolean onCommand( CommandSender sender,  Command command,  String label,  String[] args) {

        if(sender instanceof Player player){
            // 9, 18, 27, 36, 45, 54
            Inventory Giver = Bukkit.createInventory(player,36,ChatColor.YELLOW+"Artefactos");

            //agregar artefactos al Gui desde ArtifactManager
            Giver.setItem(0, ArtifactManager.getTestStick());
            Giver.setItem(1,ArtifactManager.getHarvesterHoe());
            Giver.setItem(2,ArtifactManager.getTrueHarvesterHoe());

            //abrir inventario
            player.openInventory(Giver);

        }

        return true;
    }

}

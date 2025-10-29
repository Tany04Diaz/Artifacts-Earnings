package org.akorpuzz.artifacts.commands;

import org.akorpuzz.artifacts.ArtifactManager;
import org.akorpuzz.artifacts.Artifacts;
import org.akorpuzz.artifacts.Features.SellStickFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiCommand implements CommandExecutor{


    @Override
    public boolean onCommand( CommandSender sender,  Command command,  String label,  String[] args) {

        if(sender instanceof Player player){
            // 9, 18, 27, 36, 45, 54
            Inventory Giver = Bukkit.createInventory(player,36,ChatColor.YELLOW+"Artefactos");

            //agregar artefactos al Gui desde ArtifactManager
            Giver.setItem(0, ArtifactManager.getTestStick());
            ItemStack harvesterHoe = ArtifactManager.HARVESTER_HOE(plugin.getHarvesterUses(), plugin);
            Giver.setItem(1, harvesterHoe);
            Giver.setItem(2,ArtifactManager.getTrueHarvesterHoe());
            Giver.setItem(3, SellStickFactory.createSellStick10(this.plugin));
            Giver.setItem(4, SellStickFactory.createSellStick25(this.plugin));
            Giver.setItem(5, SellStickFactory.createSellStick50(this.plugin));
            Giver.setItem(6, org.akorpuzz.artifacts.TotemFactory.createTotemTier1(this.plugin));
            Giver.setItem(7, org.akorpuzz.artifacts.TotemFactory.createTotemTier2(this.plugin));
            Giver.setItem(8, org.akorpuzz.artifacts.TotemFactory.createTotemTier3(this.plugin));


            //abrir inventario
            player.openInventory(Giver);

        }

        return true;
    }
    //buen fragmento de codigo
    private Artifacts plugin;

    public GuiCommand(Artifacts plugin) {
        this.plugin = plugin;
    }

}

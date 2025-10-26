package org.akorpuzz.artifacts.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TestComand implements CommandExecutor {
    //metodo OnCommand
    @Override
    public boolean onCommand(CommandSender sender,Command command, String label, String[] args) {

        //confirma si lo envio un jugador
        if (sender instanceof Player Player){

            //obtener al jugador
            Player player = (Player) sender;

            //crear el item
            ItemStack Sword = new ItemStack(Material.DIAMOND_SWORD);

            //crear el meta: nombre, lore, carcteristicas
            ItemMeta im = Sword.getItemMeta();
            im.setDisplayName(ChatColor.GREEN+"FancySword");
            //im.setLore(Arrays.asList((ChatColor.RED.toString()+ChatColor.BOLD + "hola mundo")));

            //crear lore en array
            List<String> imlore = new ArrayList<>();
            imlore.add(ChatColor.RED.toString()+ChatColor.BOLD + "hola mundo");
            imlore.add(ChatColor.DARK_PURPLE+"me quede sin nada mas que decir");
            im.setLore(imlore);

            //encantamientos
            im.addEnchant(Enchantment.LOOTING,10,true);

            //añadir el meta
            Sword.setItemMeta(im);

            //añadir al inventario del jugador
            Player.getInventory().addItem(Sword);
            Player.sendMessage(ChatColor.GREEN+ "Se Entrego un objeto");
        }
        return true;
    }
}

package org.akorpuzz.artifacts.listener;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.akorpuzz.artifacts.Features.SellStickFactory;
import org.akorpuzz.artifacts.service.PriceService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SellStickListener implements Listener {
    private final JavaPlugin plugin;
    private final PriceService priceService;
    private final Economy economy;
    private final NamespacedKey keyId;
    private final NamespacedKey keyUses;

    public SellStickListener(JavaPlugin plugin, PriceService priceService, Economy economy) {
        this.plugin = Objects.requireNonNull(plugin);
        this.priceService = priceService;
        this.economy = economy;
        this.keyId = SellStickFactory.KEY_ID(plugin);
        this.keyUses = SellStickFactory.KEY_USES(plugin);
    }

    private boolean isSellStick(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keyId, PersistentDataType.STRING);
    }

    @EventHandler
    public void onPlayerUseSellStick(PlayerInteractEvent event) {
        // solo mano principal y right click block
        if (event.getHand() == null) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isSellStick(hand)) return;

        // evita abrir inventario del contenedor y procede a vender
        event.setCancelled(true);

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        BlockState state = clicked.getState();
        if (!(state instanceof Container)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("sellstick.messages.not_container", "&cThat block is not a container.")));
            return;
        }

        Container container = (Container) state;
        Inventory inv = container.getInventory();

        // Calcular total y qué slots/items vender
        Map<Integer, ItemStack> toRemove = new HashMap<>();
        double totalMoney = 0.0;
        int totalItemsSold = 0;

        // Iterate over inventory snapshots to avoid concurrency issues
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() == Material.AIR) continue;

            // Precio por unidad
            @Nullable Double unitPrice = priceService != null ? priceService.getPrice(is.getType()) : null;
            if (unitPrice == null) {
                // no hay precio: saltar ese stack (no se vende)
                continue;
            }

            int amount = is.getAmount();
            totalItemsSold += amount;
            totalMoney += unitPrice * amount;

            // marcar para eliminación completa del stack
            toRemove.put(i, is.clone());
        }

        if (toRemove.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("sellstick.messages.nothing_to_sell", "&eNo hay items vendibles en el cofre.")));
            return;
        }

        // Si economy no disponible, fallback: no vender, informar
        if (economy == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("sellstick.messages.no_economy", "&cEconomy provider not available. Sales disabled.")));
            return;
        }

        // Depositar dinero al usuario
        EconomyResponse resp = economy.depositPlayer(player, totalMoney);
        if (!resp.transactionSuccess()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("sellstick.messages.deposit_failed", "&cFailed to deposit money.")));
            return;
        }

        // Borrar los stacks vendidos del cofre (es seguro porque trabajamos sobre índice)
        for (Map.Entry<Integer, ItemStack> e : toRemove.entrySet()) {
            int slot = e.getKey();
            inv.setItem(slot, null);
        }
        // Actualizar estado del bloque (servidor lo hará automáticamente; si quieres, state.update(true))

        // Mensajes al jugador
        String soldMsgTemplate = plugin.getConfig().getString("sellstick.messages.sold",
                "&aVendiste %amount% items del cofre por %money%"); // %amount% %money%
        String moneyStr = String.format(Locale.ROOT, "%.2f", totalMoney);
        String soldMsg = ChatColor.translateAlternateColorCodes('&', soldMsgTemplate)
                .replace("%amount%", String.valueOf(totalItemsSold))
                .replace("%money%", moneyStr);
        player.sendMessage(soldMsg);
        // Sonido feedback
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1f);

        // Consumir 1 uso de la SellStick (PDC)
        Integer uses = getUsesFromItem(hand);
        int newUses = (uses == null) ? 0 : Math.max(0, uses - 1);

        // actualizar item en la mano (usos y display/lore)
        writeUsesToItemAndSync(hand, newUses, player);

        // Si newUses == 0, item fue eliminado por writeUsesToItemAndSync; enviar mensaje
        if (newUses <= 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("sellstick.messages.broken", "&cSe rompio tu SellStick.")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1f);
        }
    }

    @Nullable
    private Integer getUsesFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(keyUses, PersistentDataType.INTEGER);
    }

    private void writeUsesToItemAndSync(ItemStack item, int uses, @Nullable Player holder) {
        if (item == null) return;
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, uses);

        // Update displayName and lore using config template
        String template = plugin.getConfig().getString("sellstick.displayName", "&6SellStick &7- &e%uses% usos");
        String display = ChatColor.translateAlternateColorCodes('&', template).replace("%uses%", String.valueOf(uses));
        meta.setDisplayName(display);

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line != null && ChatColor.stripColor(line).toLowerCase().contains("usos"));
        lore.add(ChatColor.GRAY + "Usos: " + ChatColor.YELLOW + uses);
        copy.setItemMeta(meta);

        if (holder != null) {
            int slot = holder.getInventory().getHeldItemSlot();
            if (uses <= 0) {
                holder.getInventory().setItem(slot, null);
                holder.getWorld().playSound(holder.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1f);
            } else {
                holder.getInventory().setItem(slot, copy);
            }
        }
    }
}
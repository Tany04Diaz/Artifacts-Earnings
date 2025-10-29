package org.akorpuzz.artifacts.listener;

import net.milkbowl.vault.economy.Economy;
import org.akorpuzz.artifacts.Artifacts;
import org.akorpuzz.artifacts.Features.ArtifactPDC;
import org.akorpuzz.artifacts.Features.HarvesterMode;
import org.akorpuzz.artifacts.service.PriceService;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * HarvesterListener integrado con Vault (Economy), PriceService y ArtifactPDC.
 * - Maneja recolección/venta de cultivos (wheat, carrots, potatoes, beetroot, sugar cane).
 * - Replanta automáticamente y protege posiciones brevemente.
 * - Soporta modo COLLECT / SELL que se alterna con Shift + Right-Click (sneak + right click).
 * - Usa PDC keys: artifact_uses (INT), artifact_id (STRING opcional), artifact_mode (STRING).
 *
 * Requiere:
 *  - PriceService (puede ser null si no quieres precios).
 *  - Economy (Vault) para depositar dinero al vender (puede ser null para desactivar ventas).
 *  - ArtifactPDC para leer/escribir usos (se asume compatible con la lógica aquí).
 */
public class HarvesterListener implements Listener {
    private final Artifacts plugin;
    private final ArtifactPDC pdc;
    private final PriceService priceService;      // puede ser null
    private final Economy economy;                // Vault Economy, puede ser null
    private final Set<String> protectedPositions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final NamespacedKey keyUses;
    private final NamespacedKey keyId;
    private final NamespacedKey keyMode;

    public HarvesterListener(Artifacts plugin, ArtifactPDC pdc, PriceService priceService, Economy economy) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pdc = Objects.requireNonNull(pdc, "pdc");
        this.priceService = priceService;
        this.economy = economy;
        this.keyUses = new NamespacedKey(plugin, "artifact_uses");
        this.keyId = new NamespacedKey(plugin, "artifact_id");
        this.keyMode = new NamespacedKey(plugin, "artifact_mode");
    }

    /* ----------------------------- Utilities ----------------------------- */

    private String posKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getUID().toString() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void protectLocationForTicks(Location loc, int ticks) {
        if (loc == null || loc.getWorld() == null) return;
        String key = posKey(loc);
        protectedPositions.add(key);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> protectedPositions.remove(key), Math.max(1, ticks));
    }

    private boolean isProtected(Location loc) {
        if (loc == null) return false;
        return protectedPositions.contains(posKey(loc));
    }

    private boolean isUsingHarvesterHoe(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null) return false;
        Material m = hand.getType();
        if (m == Material.DIAMOND_HOE || m == Material.NETHERITE_HOE) return true;
        ItemMeta meta = hand.getItemMeta();
        if (meta != null) {
            String name = meta.getDisplayName();
            if (name != null && name.toLowerCase(Locale.ROOT).contains("harvester")) return true;
            if (meta.getPersistentDataContainer().has(keyId, PersistentDataType.STRING)) return true;
            if (meta.getPersistentDataContainer().has(keyUses, PersistentDataType.INTEGER)) return true;
        }
        return false;
    }

    private boolean isSupportedCropBlock(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOT || m == Material.SUGAR_CANE;
    }

    private boolean isStemCropBlock(Material m) {
        return m == Material.WHEAT || m == Material.CARROTS || m == Material.POTATOES || m == Material.BEETROOT;
    }

    private String beautifyMaterialName(Material m) {
        String s = m.name().toLowerCase().replace('_', ' ');
        String[] parts = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Nullable
    private Integer getUsesFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(keyUses, PersistentDataType.INTEGER);
    }

    private void updateUsesLore(ItemMeta meta, int uses, @Nullable Integer maxUses) {
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> {
            if (line == null) return false;
            String plain = ChatColor.stripColor(line).toLowerCase();
            return plain.startsWith("usos:") || plain.startsWith("uses:");
        });
        String usageText = (maxUses != null)
                ? (ChatColor.GRAY + "Usos: " + ChatColor.YELLOW + uses + ChatColor.GRAY + " / " + ChatColor.YELLOW + maxUses)
                : (ChatColor.GRAY + "Usos: " + ChatColor.YELLOW + uses);
        lore.add(usageText);
        meta.setLore(lore);
    }

    private String formatNameWithUses(String template, int uses, @Nullable Integer maxUses) {
        if (template == null) template = "§6Harvester Hoe §7- §e%uses% usos";
        String maxStr = (maxUses == null ? "-" : String.valueOf(maxUses));
        return template.replace("%uses%", String.valueOf(uses)).replace("%max%", maxStr);
    }

    /**
     * Escribe uses en PDC, actualiza displayName y lore, y sincroniza la ranura del holder si se pasa holder != null.
     */
    private void setUsesOnItemStackAndSync(ItemStack item, int uses, @Nullable Integer maxUses, @Nullable Player holder) {
        if (item == null) return;
        ItemStack copy = item.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, uses);

        String nameTemplate = plugin.getConfig().getString("harvester.displayName", "§6Harvester Hoe §7- §e%uses% usos");
        String formattedName = formatNameWithUses(nameTemplate, uses, maxUses);
        meta.setDisplayName(formattedName);

        updateUsesLore(meta, uses, maxUses);
        copy.setItemMeta(meta);

        if (holder != null) {
            int slot = holder.getInventory().getHeldItemSlot();
            if (uses <= 0) {
                holder.getInventory().setItem(slot, null);
                holder.getWorld().playSound(holder.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1f);
            } else {
                holder.getInventory().setItem(slot, copy);
            }
            holder.updateInventory();
        }
    }

    /* ----------------------------- Mode toggle (Shift + Right-Click) ----------------------------- */

    private final Set<UUID> modeToggleCooldown = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Procesar solo la mano principal para evitar duplicados
        if (event.getHand() != EquipmentSlot.HAND) return;

        // Solo clicks derecho
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        Player player = event.getPlayer();
        if (!player.isSneaking()) return; // requiere shift

        // Debounce: si el jugador acaba de togglear, ignorar
        UUID id = player.getUniqueId();
        if (modeToggleCooldown.contains(id)) {
            event.setCancelled(true);
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;
        if (!isUsingHarvesterHoe(player)) return;

        // Evitar que la interacción haga cosas de bloque (p.e. abrir puertas) mientras togglamos
        event.setCancelled(true);

        // Marcar cooldown breve (10 ticks = 0.5s)
        modeToggleCooldown.add(id);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> modeToggleCooldown.remove(id), 10L);

        // Obtener slot actual y la pila en ese slot (trabaja sobre clon para no causar reentradas)
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack current = player.getInventory().getItem(slot);
        if (current == null || current.getType().isAir()) return;

        // Leer modo y togglear
        HarvesterMode currentMode = HarvesterMode.getMode(current, keyMode, HarvesterMode.COLLECT);
        HarvesterMode next = (currentMode == HarvesterMode.COLLECT) ? HarvesterMode.SELL : HarvesterMode.COLLECT;

        // Clonar, actualizar PDC y meta (preservando uses en PDC si existe)
        ItemStack copy = current.clone();
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(keyMode, PersistentDataType.STRING, next.name());

        Integer uses = getUsesFromItem(current);
        if (uses != null) meta.getPersistentDataContainer().set(keyUses, PersistentDataType.INTEGER, uses);

        String nameTemplate = plugin.getConfig().getString("harvester.displayName", "§6Harvester Hoe §7- §e%uses% usos");
        String baseName = formatNameWithUses(nameTemplate, uses == null ? 0 : uses, null);
        String suffix = (next == HarvesterMode.SELL) ? " §4§lVenta" : " §4§lRecoleccion";
        meta.setDisplayName(baseName + suffix);

        copy.setItemMeta(meta);
        // Escribir la copia en la ranura (esto no debe provocar resending inmediato que togglee de nuevo gracias al debounce)
        player.getInventory().setItem(slot, copy);
        // Evita llamar player.updateInventory() aquí; puede forzar reenvío que dispare otro evento
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1f);
        String msgKey = (next == HarvesterMode.SELL) ? "messages.mode_sell" : "messages.mode_collect";
        String msg = plugin.getConfig().getString(msgKey, (next == HarvesterMode.SELL ? "&8&lSe a cambiado al modo : &4Venta" : "&8&lSe a cambiado al modo : &4Recoleccion"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }




    /* ----------------------------- Block break (harvest) ----------------------------- */

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (isProtected(loc)) return;

        Material mat = block.getType();
        if (!isSupportedCropBlock(mat)) return;

        if (!isUsingHarvesterHoe(player)) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;

        Integer usesLeft = getUsesFromItem(hand);
        if (usesLeft != null && usesLeft <= 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.harvester_consumed", "&cLa Harvester Hoe está desgastada.")));
            return;
        }

        if (!isFullyGrownForHarvest(block)) {
            boolean replanted = replantAsYoungAndProtect(block);
            if (replanted) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.replanted", "&7El cultivo se ha replantado en estado joven.")));
                event.setCancelled(true);
                return;
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.not_mature", "&7El cultivo no está listo para cosechar.")));
                return;
            }
        }

        // Gestionamos drops manualmente
        event.setCancelled(true);
        protectLocationForTicks(loc, 2);

        // Consumir herramienta inmediatamente (1 uso) y sincronizar la pila del jugador
        handleToolConsumptionImmediateAndSync(player);

        // Leer modo actual tras sincronización
        ItemStack currentHand = player.getInventory().getItemInMainHand();
        HarvesterMode mode = HarvesterMode.getMode(currentHand, keyMode, HarvesterMode.COLLECT);

        if (isStemCropBlock(mat)) {
            handleStemCrop(block, player, mat, mode);
        } else if (mat == Material.SUGAR_CANE) {
            handleSugarCane(block, player, mode);
        }
        // Reduction of uses per-block is handled inside handle* via setUsesOnItemStackAndSync when getUsesFromItem != null
    }

    /* ----------------------------- Harvest handlers ----------------------------- */

    private boolean isFullyGrownForHarvest(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        if (m == Material.SUGAR_CANE) {
            int height = sugarCaneColumnHeight(block);
            return height >= 2;
        }
        try {
            if (block.getBlockData() instanceof Ageable) {
                Ageable age = (Ageable) block.getBlockData();
                return age.getAge() == age.getMaximumAge();
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private int sugarCaneColumnHeight(Block block) {
        if (block == null || block.getType() != Material.SUGAR_CANE) return 0;
        int height = 1;
        Block up = block.getRelative(BlockFace.UP);
        while (up.getType() == Material.SUGAR_CANE) { height++; up = up.getRelative(BlockFace.UP); }
        Block down = block.getRelative(BlockFace.DOWN);
        while (down.getType() == Material.SUGAR_CANE) { height++; down = down.getRelative(BlockFace.DOWN); }
        return height;
    }

    private boolean replantAsYoungAndProtect(Block block) {
        if (block == null) return false;
        Material m = block.getType();

        try {
            if (block.getBlockData() instanceof Ageable) {
                Location loc = block.getLocation();
                protectLocationForTicks(loc, 2);
                block.setType(m);
                Ageable age = (Ageable) block.getBlockData();
                age.setAge(0);
                block.setBlockData(age);
                return true;
            }
        } catch (Throwable ignored) {}

        if (m == Material.SUGAR_CANE) {
            int height = sugarCaneColumnHeight(block);
            if (height <= 1) {
                Location loc = block.getLocation();
                protectLocationForTicks(loc, 2);
                block.setType(Material.SUGAR_CANE);
                return true;
            }
        }
        return false;
    }

    private void handleStemCrop(Block block, Player player, Material cropBlockType, HarvesterMode mode) {
        Material produceItem = produceItemForBlock(cropBlockType);
        Collection<ItemStack> rawDrops = block.getDrops(player.getInventory().getItemInMainHand());

        List<ItemStack> collectDrops = new ArrayList<>();
        int totalProduceAmount = 0;

        for (ItemStack d : rawDrops) {
            if (d == null || d.getAmount() <= 0) continue;
            collectDrops.add(new ItemStack(d.getType(), d.getAmount()));
            if (d.getType() == produceItem) totalProduceAmount += d.getAmount();
        }

        Location loc = block.getLocation();
        protectLocationForTicks(loc, 2);

        // Replant the crop at age 0
        block.setType(cropBlockType);
        try {
            if (block.getBlockData() instanceof Ageable) {
                Ageable age = (Ageable) block.getBlockData();
                age.setAge(0);
                block.setBlockData(age);
            }
        } catch (Throwable ignored) {}

        // Deliver or sell
        if (mode == HarvesterMode.COLLECT) {
            giveItemsToPlayerOrDrop(player, collectDrops, loc);
        } else {
            if (totalProduceAmount > 0) {
                Double unitPrice = priceService != null ? priceService.getPrice(produceItem) : null;
                if (unitPrice != null && economy != null) {
                    double total = unitPrice * totalProduceAmount;
                    economy.depositPlayer(player, total);
                    plugin.getLogger().fine("Sold " + totalProduceAmount + "x " + produceItem + " for " + total + " to " + player.getName());
                } else {
                    plugin.getLogger().log(Level.FINE, "Precio no encontrado o economy null for " + produceItem);
                    giveItemsToPlayerOrDrop(player, collectDrops, loc);
                }
            } else {
                giveItemsToPlayerOrDrop(player, collectDrops, loc);
            }
        }

        // Decrement uses by amount of produce harvested (if item uses exist)
        ItemStack hand = player.getInventory().getItemInMainHand();
        Integer uses = getUsesFromItem(hand);
        if (uses != null) {
            int newUses = Math.max(0, uses - totalProduceAmount);
            setUsesOnItemStackAndSync(hand, newUses, null, player);
            if (newUses <= 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.harvester_consumed", "&cLa Harvester Hoe está desgastada.")));
            }
        }
    }

    private void handleSugarCane(Block brokenBlock, Player player, HarvesterMode mode) {
        Block base = brokenBlock;
        while (base.getY() > 0 && base.getRelative(BlockFace.DOWN).getType() == Material.SUGAR_CANE) {
            base = base.getRelative(BlockFace.DOWN);
        }

        List<ItemStack> collected = new ArrayList<>();
        int sold = 0;

        Block cursor = base.getRelative(BlockFace.UP);
        while (cursor.getType() == Material.SUGAR_CANE) {
            Location curLoc = cursor.getLocation();
            protectLocationForTicks(curLoc, 2);
            collected.add(new ItemStack(Material.SUGAR_CANE, 1));
            cursor.setType(Material.AIR);
            sold++;
            cursor = cursor.getRelative(BlockFace.UP);
        }

        // Ensure base remains if broken block was base
        if (brokenBlock.equals(base)) {
            Location baseLoc = base.getLocation();
            protectLocationForTicks(baseLoc, 2);
            base.setType(Material.SUGAR_CANE);
        }

        if (mode == HarvesterMode.COLLECT) {
            giveItemsToPlayerOrDrop(player, collected, base.getLocation());
        } else {
            if (sold > 0) {
                Double unitPrice = priceService != null ? priceService.getPrice(Material.SUGAR_CANE) : null;
                if (unitPrice != null && economy != null) {
                    double total = unitPrice * sold;
                    economy.depositPlayer(player, total);
                    plugin.getLogger().fine("Sold " + sold + "x SUGAR_CANE for " + total + " to " + player.getName());
                } else {
                    plugin.getLogger().log(Level.FINE, "Precio no encontrado o economy null for SUGAR_CANE");
                    giveItemsToPlayerOrDrop(player, collected, base.getLocation());
                }
            } else {
                giveItemsToPlayerOrDrop(player, collected, base.getLocation());
            }
        }

        // Decrement uses by number of cane blocks harvested (if item uses exist)
        ItemStack hand = player.getInventory().getItemInMainHand();
        Integer uses = getUsesFromItem(hand);
        if (uses != null) {
            int newUses = Math.max(0, uses - sold);
            setUsesOnItemStackAndSync(hand, newUses, null, player);
            if (newUses <= 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.harvester_consumed", "&cLa Harvester Hoe está desgastada.")));
            }
        }
    }

    private void giveItemsToPlayerOrDrop(Player player, Collection<ItemStack> items, Location dropLocation) {
        if (items == null || items.isEmpty()) return;
        for (ItemStack is : items) {
            if (is == null || is.getAmount() <= 0) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(is);
            if (!leftover.isEmpty()) {
                for (ItemStack rem : leftover.values()) player.getWorld().dropItemNaturally(dropLocation, rem);
            }
        }
    }

    private Material produceItemForBlock(Material blockType) {
        switch (blockType) {
            case WHEAT: return Material.WHEAT;
            case CARROTS: return Material.CARROT;
            case POTATOES: return Material.POTATO;
            case BEETROOT: return Material.BEETROOT;
            default: return blockType;
        }
    }

    /* ----------------------------- Tool consumption fallback ----------------------------- */

    /**
     * Consume 1 uso if PDC exists, otherwise apply vanilla durability.
     * Synchronizes the player's held slot.
     */
    private void handleToolConsumptionImmediateAndSync(Player player) {
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack actual = player.getInventory().getItem(slot);
        if (actual == null || actual.getType().isAir()) return;

        Integer usesLeft = getUsesFromItem(actual);
        if (usesLeft != null) {
            int newUses = Math.max(0, usesLeft - 1);

            if (newUses <= 0) {
                if (actual.getAmount() > 1) {
                    ItemStack remaining = actual.clone();
                    remaining.setAmount(actual.getAmount() - 1);
                    player.getInventory().setItem(slot, remaining);
                } else {
                    player.getInventory().setItem(slot, null);
                }
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1f);
                player.updateInventory();
            } else {
                setUsesOnItemStackAndSync(actual, newUses, null, player);
            }
            return;
        }

        try {
            ItemMeta meta = actual.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable) {
                org.bukkit.inventory.meta.Damageable dmgMeta = (org.bukkit.inventory.meta.Damageable) meta;
                int current = dmgMeta.getDamage();
                dmgMeta.setDamage(current + 1);
                actual.setItemMeta((ItemMeta) dmgMeta);

                int maxDur = actual.getType().getMaxDurability();
                if (dmgMeta.getDamage() >= maxDur) {
                    if (actual.getAmount() > 1) {
                        actual.setAmount(actual.getAmount() - 1);
                        player.getInventory().setItem(slot, actual);
                    } else {
                        player.getInventory().setItem(slot, null);
                    }
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1f);
                } else {
                    player.getInventory().setItem(slot, actual);
                }
                player.updateInventory();
                return;
            }
        } catch (Throwable ignored) {}
    }
}
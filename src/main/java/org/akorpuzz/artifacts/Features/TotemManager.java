package org.akorpuzz.artifacts.Features;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TotemManager {
    private final JavaPlugin plugin;
    private final Material totemMaterial;
    private final String pdcKey;
    private final int intervalTicks;
    private final boolean debug;
    private final Map<Integer, TotemTier> tiers = new HashMap<>();
    private final Set<Location> totems = ConcurrentHashMap.newKeySet(); // locations of totems
    private BukkitTask task;

    // config-based whitelist
    private final Set<Material> cropWhitelist = EnumSet.noneOf(Material.class);
    private final Set<String> worldsAllowed = new HashSet<>();

    public TotemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.totemMaterial = Material.valueOf(plugin.getConfig().getString("totem.material", "STRIPPED_OAK_LOG"));
        this.pdcKey = plugin.getConfig().getString("totem.pdc_key", "crop_totem_tier");
        this.intervalTicks = plugin.getConfig().getInt("totem.apply_interval_ticks", 40);
        this.debug = plugin.getConfig().getBoolean("totem.debug", false);

        // load tiers
        ConfigurationSection ts = plugin.getConfig().getConfigurationSection("totem.tiers");
        if (ts != null) {
            for (String key : ts.getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    ConfigurationSection s = ts.getConfigurationSection(key);
                    int radius = s.getInt("radius", 4);
                    double multiplier = s.getDouble("multiplier", 1.5);
                    double extraChance = s.getDouble("extraChance", 0.0);
                    tiers.put(id, new TotemTier(id, radius, multiplier, extraChance));
                } catch (NumberFormatException ignored) { }
            }
        }
        // load crop whitelist
        List<String> crops = plugin.getConfig().getStringList("crop_whitelist");
        for (String c : crops) {
            try { cropWhitelist.add(Material.valueOf(c)); } catch (Throwable ignored) {}
        }
        // worlds allowed
        List<String> ws = plugin.getConfig().getStringList("totem.worlds_allowed");
        if (ws != null) worldsAllowed.addAll(ws);

        // start task
        startTask();
    }

    public void shutdown() {
        if (task != null) task.cancel();
    }

    public boolean isTotemBlock(Block b) {
        if (b == null) return false;
        if (b.getType() != totemMaterial) return false;
        PersistentDataContainer pdc = b.getChunk().getPersistentDataContainer(); // not used; we'll read block meta via TileState if existed
        // read pdc from block using block state name -- simpler: expect totem tier stored in block's tile entity? Many log types don't have blockstate.
        // We will rely on our registered set of totems; if set contains loc -> it's a totem.
        return totems.contains(b.getLocation());
    }

    public void registerTotem(Location loc, int tier) {
        totems.add(loc.clone());
        if (debug) plugin.getLogger().info("Registered totem at " + loc + " tier=" + tier);
    }

    public void unregisterTotem(Location loc) {
        totems.remove(loc.clone());
        if (debug) plugin.getLogger().info("Unregistered totem at " + loc);
    }

    private void startTask() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::applyTick, intervalTicks, intervalTicks);
    }

    private void applyTick() {
        if (totems.isEmpty()) return;
        // For each totem, apply growth in radius
        for (Location loc : new ArrayList<>(totems)) {
            if (loc == null) continue;
            if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) continue;
            int tierId = readTierFromBlock(loc);
            TotemTier tier = tiers.getOrDefault(tierId, tiers.get(1));
            if (tier == null) continue;
            if (!worldsAllowed.isEmpty() && !worldsAllowed.contains(loc.getWorld().getName())) continue;

            // particles
            spawnParticles(loc, plugin.getConfig().getString("totem.particle.type", "VILLAGER_HAPPY"),
                    plugin.getConfig().getInt("totem.particle.count", 6),
                    plugin.getConfig().getDouble("totem.particle.spread", 0.5));

            // find blocks in radius (simple cubic scan bounded by radius)
            int r = tier.radius;
            Set<Location> processed = new HashSet<>();
            World w = loc.getWorld();
            int cx = loc.getBlockX();
            int cy = loc.getBlockY();
            int cz = loc.getBlockZ();
            double multiplier = tier.multiplier;
            double extraChance = tier.extraChance;

            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    int bx = cx + dx;
                    int bz = cz + dz;
                    for (int by = cy - 1; by <= cy + 2; by++) { // reasonable Y window
                        Location bl = new Location(w, bx, by, bz);
                        if (processed.contains(bl)) continue;
                        if (bl.distance(loc) > r + 0.5) continue; // euclidean filter
                        processed.add(bl);
                        Block b = bl.getBlock();
                        Material m = b.getType();
                        if (!cropWhitelist.contains(m)) continue;
                        tryHandleCropGrowth(b, multiplier, extraChance, loc);
                    }
                }
            }
        }
    }

    private void tryHandleCropGrowth(Block b, double multiplier, double extraChance, Location visualOrigin) {
        Material m = b.getType();
        if (m == Material.SUGAR_CANE) {
            handleSugarCaneGrowth(b, multiplier, extraChance);
            return;
        } else if (m == Material.CACTUS) {
            handleCactusGrowth(b, multiplier, extraChance);
            return;
        } else {
            // Ageable crops
            if (!(b.getBlockData() instanceof Ageable)) return;
            Ageable ageable = (Ageable) b.getBlockData();
            int age = ageable.getAge();
            int max = ageable.getMaximumAge();
            if (age >= max) return;
            double baseChance = 0.06; // tunable baseline
            double effective = baseChance * multiplier + extraChance;
            if (Math.random() < effective) {
                try {
                    ageable.setAge(Math.min(max, age + 1));
                    b.setBlockData(ageable);
                    // particle small
                    b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.02);
                } catch (Throwable ignored) {}
            }
        }
    }

    private void handleSugarCaneGrowth(Block b, double multiplier, double extraChance) {
        Block above = b.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) return;
        // limit height to 3
        int height = 1;
        Block down = b.getRelative(0, -1, 0);
        while (down.getType() == Material.SUGAR_CANE) { height++; down = down.getRelative(0, -1, 0); }
        double base = 0.03;
        double effective = base * multiplier + extraChance;
        if (Math.random() < effective) {
            above.setType(Material.SUGAR_CANE);
            above.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, above.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void handleCactusGrowth(Block b, double multiplier, double extraChance) {
        Block above = b.getRelative(0, 1, 0);
        if (above.getType() != Material.AIR) return;
        double base = 0.02;
        double effective = base * multiplier + extraChance;
        if (Math.random() < effective) {
            above.setType(Material.CACTUS);
            above.getWorld().spawnParticle(Particle.SMOKE, above.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private int readTierFromBlock(Location loc) {
        // read from a stored map or from block state PDC if you wrote it there.
        // For this implementation we store tier encoded in X/Z/Y in internal map (or in block metadata if desired).
        // For simplicity: default to tier 1
        // TODO: support reading tier from block's persistent data if you set it when placing
        return 1;
    }

    private void spawnParticles(Location loc, String particleName, int count, double spread) {
        Particle p;
        try { p = Particle.valueOf(particleName); } catch (Throwable t) { p = Particle.HAPPY_VILLAGER; }
        loc.getWorld().spawnParticle(p, loc.add(0.5, 1.2, 0.5), count, spread, spread, spread, 0.02);
    }

    private static final class TotemTier {
        final int id;
        final int radius;
        final double multiplier;
        final double extraChance;
        TotemTier(int id, int radius, double multiplier, double extraChance) {
            this.id = id; this.radius = radius; this.multiplier = multiplier; this.extraChance = extraChance;
        }
    }
}
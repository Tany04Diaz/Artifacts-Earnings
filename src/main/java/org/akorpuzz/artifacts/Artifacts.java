package org.akorpuzz.artifacts;

import net.milkbowl.vault.economy.Economy;
import org.akorpuzz.artifacts.Features.ArtifactPDC;
import org.akorpuzz.artifacts.Features.TotemManager;
import org.akorpuzz.artifacts.commands.GuiCommand;
import org.akorpuzz.artifacts.commands.TestComand;
import org.akorpuzz.artifacts.listener.HarvesterListener;
import org.akorpuzz.artifacts.listener.SellStickListener;
import org.akorpuzz.artifacts.listener.TotemPlacementListener;
import org.akorpuzz.artifacts.service.PriceService;
import org.bukkit.Material;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Artifacts extends JavaPlugin {
    //pdc y usos de Harvester hoe
    private ArtifactPDC pdc;
    private int hhuses;
    private TotemManager totemManager;



    @Override
    public void onEnable() {

        // Plugin startup logic
        getCommand("artifactsearnings").setExecutor(new GuiCommand(this));
        getLogger().info("AArtifacts habilitado");
        saveDefaultConfig();
        //logica Harvesterhoe
        this.pdc = new ArtifactPDC(this);
        PriceService priceService = new PriceService(this);
        Economy economy = setupEconomy();
        this.totemManager = new TotemManager(this);
        this.hhuses = getConfig().getInt("harvester-hoe.uses",100);
        List<String> mats = getConfig().getStringList("harvester-hoe.harvest-blocks");
        List<Material> harvestables = mats.stream().map(Material::valueOf).toList();
        getServer().getPluginManager().registerEvents(new HarvesterListener(this, pdc, priceService, economy), this);
        getServer().getPluginManager().registerEvents(new SellStickListener(this, priceService, economy), this);
        getServer().getPluginManager().registerEvents(new TotemPlacementListener(this, totemManager), this);


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AArtifacts deshabilitado");
        if (totemManager != null) totemManager.shutdown();


    }
    // getters públicos usados por otras clases para Harvester hoe
    public ArtifactPDC getArtifactPDC() {
        return pdc;
    }

    public int getHarvesterUses() {
        return hhuses;
    }
    private Economy setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return null;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        return (rsp != null) ? rsp.getProvider() : null;
    }

}

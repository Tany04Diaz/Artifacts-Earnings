package org.akorpuzz.artifacts;

import org.akorpuzz.artifacts.commands.GuiCommand;
import org.akorpuzz.artifacts.commands.TestComand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Artifacts extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("sword").setExecutor(new TestComand());
        getCommand("artifacts").setExecutor(new GuiCommand());
        getLogger().info("AArtifacts habilitado");


    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("AArtifacts deshabilitado");
    }
}

package dev.distorteduniverse.immortal;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DistortedUniverseImmortalPlugin extends JavaPlugin {
    private ImmortalSettingsService settingsService;
    private ImmortalPlayerStore playerStore;

    @Override
    public void onEnable() {
        settingsService = new ImmortalSettingsService(this);
        settingsService.load();

        playerStore = new ImmortalPlayerStore(this);
        playerStore.load();

        getServer().getPluginManager().registerEvents(
            new ImmortalDamageListener(this, settingsService, playerStore),
            this
        );

        ImmortalCommand commandHandler = new ImmortalCommand(settingsService, playerStore);
        PluginCommand command = getCommand("duimmortal");
        if (command == null) {
            getLogger().severe("Command duimmortal is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getLogger().info("DistortedUniverseImmortal enabled.");
    }

    @Override
    public void onDisable() {
        if (settingsService != null && settingsService.settings() != null) {
            settingsService.save();
        }
        if (playerStore != null) {
            playerStore.save();
        }
        getLogger().info("DistortedUniverseImmortal disabled.");
    }
}

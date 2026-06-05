package dev.distorteduniverse.playerdisguise;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DistortedUniversePlayerDisguisePlugin extends JavaPlugin {
    private DisguiseSettingsService settingsService;
    private DisguiseStore disguiseStore;
    private NicknameStore nicknameStore;
    private DisguiseProtocolService protocolService;
    private DisguiseDisplayService displayService;

    @Override
    public void onEnable() {
        settingsService = new DisguiseSettingsService(this);
        settingsService.load();

        disguiseStore = new DisguiseStore(this);
        disguiseStore.load();

        nicknameStore = new NicknameStore(this);
        nicknameStore.load();

        if (!getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().severe("ProtocolLib is required and is not enabled. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        protocolService = new DisguiseProtocolService(this, settingsService, disguiseStore, nicknameStore);
        protocolService.start();

        ProfileLookupService profileLookupService = new ProfileLookupService(this);
        displayService = new DisguiseDisplayService(this, settingsService, disguiseStore, nicknameStore, protocolService);
        displayService.refreshAll();

        getServer().getPluginManager().registerEvents(
            new DisguiseListener(this, disguiseStore, nicknameStore, displayService), this);

        DisguiseCommand commandHandler = new DisguiseCommand(
            this,
            settingsService,
            disguiseStore,
            nicknameStore,
            profileLookupService,
            displayService
        );
        PluginCommand command = getCommand("dudisguise");
        if (command == null) {
            getLogger().severe("Command dudisguise is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getLogger().info("DistortedUniversePlayerDisguise enabled.");
    }

    @Override
    public void onDisable() {
        if (displayService != null) {
            displayService.clearAll();
        }
        if (protocolService != null) {
            protocolService.stop();
        }
        if (settingsService != null && settingsService.settings() != null) {
            settingsService.save();
        }
        if (disguiseStore != null) {
            disguiseStore.save();
        }
        if (nicknameStore != null) {
            nicknameStore.save();
        }
        getLogger().info("DistortedUniversePlayerDisguise disabled.");
    }
}

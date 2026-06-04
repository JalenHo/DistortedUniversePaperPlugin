package dev.distorteduniverse.playernickname;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DistortedUniversePlayerNicknamePlugin extends JavaPlugin {
    private NicknameSettingsService settingsService;
    private NicknameStore nicknameStore;
    private NicknameDisplayService displayService;

    @Override
    public void onEnable() {
        settingsService = new NicknameSettingsService(this);
        settingsService.load();

        nicknameStore = new NicknameStore(this);
        nicknameStore.load();

        NicknameFormatter nicknameFormatter = new NicknameFormatter();
        displayService = new NicknameDisplayService(this, settingsService, nicknameStore, nicknameFormatter);
        displayService.start();

        getServer().getPluginManager().registerEvents(new NicknameListener(this, nicknameStore, displayService), this);

        NicknameCommand commandHandler = new NicknameCommand(settingsService, nicknameStore, nicknameFormatter, displayService);
        PluginCommand command = getCommand("dunickname");
        if (command == null) {
            getLogger().severe("Command dunickname is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getLogger().info("DistortedUniversePlayerNickname enabled.");
    }

    @Override
    public void onDisable() {
        if (displayService != null) {
            displayService.stop();
        }
        if (settingsService != null && settingsService.settings() != null) {
            settingsService.save();
        }
        if (nicknameStore != null) {
            nicknameStore.save();
        }
        getLogger().info("DistortedUniversePlayerNickname disabled.");
    }
}


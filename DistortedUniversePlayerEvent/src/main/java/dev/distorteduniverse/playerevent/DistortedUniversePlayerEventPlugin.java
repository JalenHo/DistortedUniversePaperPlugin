package dev.distorteduniverse.playerevent;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DistortedUniversePlayerEventPlugin extends JavaPlugin {
    private SettingsService settingsService;
    private PlayerEventListener listener;

    @Override
    public void onEnable() {
        settingsService = new SettingsService(this);
        settingsService.load();

        MessageFormatter messageFormatter = new MessageFormatter();
        RecipientSelector recipientSelector = new RecipientSelector();
        listener = new PlayerEventListener(this, settingsService, messageFormatter, recipientSelector);
        getServer().getPluginManager().registerEvents(listener, this);

        PlayerEventCommand commandHandler = new PlayerEventCommand(settingsService, messageFormatter);
        PluginCommand command = getCommand("duplayerevent");
        if (command == null) {
            getLogger().severe("Command duplayerevent is missing from plugin.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getLogger().info("DistortedUniversePlayerEvent enabled.");
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            listener.clearPendingDeathKicks();
        }
        if (settingsService != null && settingsService.settings() != null) {
            settingsService.save();
        }
        getLogger().info("DistortedUniversePlayerEvent disabled.");
    }
}

package dev.distorteduniverse.team;

import org.bukkit.plugin.java.JavaPlugin;

public class TeamSettingsService {
    private final JavaPlugin plugin;
    private TeamSettings settings;

    public TeamSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = TeamSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = TeamSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public TeamSettings settings() {
        return settings;
    }
}

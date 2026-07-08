package dev.distorteduniverse.fakeplayer;

import org.bukkit.plugin.java.JavaPlugin;

public class FakePlayerSettingsService {
    private final JavaPlugin plugin;
    private FakePlayerSettings settings;

    public FakePlayerSettingsService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        settings = FakePlayerSettings.from(plugin.getConfig());
    }

    public void reload() {
        plugin.reloadConfig();
        settings = FakePlayerSettings.from(plugin.getConfig());
    }

    public void save() {
        settings.writeTo(plugin.getConfig());
        plugin.saveConfig();
    }

    public void update(FakePlayerSettings settings) {
        this.settings = settings;
        save();
    }

    public FakePlayerSettings settings() {
        return settings;
    }
}

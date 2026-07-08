package dev.distorteduniverse.fakeplayer;

import org.bukkit.configuration.file.FileConfiguration;
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
        migrateLegacyBehaviorDefaults(plugin.getConfig());
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

    public FakePlayerSettings settings() {
        return settings;
    }

    public void updateSettings(FakePlayerSettings newSettings) {
        this.settings = newSettings;
        save();
    }

    public void updateBehavior(FakePlayerSettings.BehaviorSettings behavior) {
        FakePlayerSettings current = settings;
        settings = new FakePlayerSettings(
            current.enabled(),
            current.movement(),
            behavior,
            current.display(),
            current.messages(),
            current.skins()
        );
        save();
    }

    public void updateMovement(FakePlayerSettings.MovementSettings movement) {
        FakePlayerSettings current = settings;
        settings = new FakePlayerSettings(
            current.enabled(),
            movement,
            current.behavior(),
            current.display(),
            current.messages(),
            current.skins()
        );
        save();
    }

    public void updateDisplay(FakePlayerSettings.DisplaySettings display) {
        FakePlayerSettings current = settings;
        settings = new FakePlayerSettings(
            current.enabled(),
            current.movement(),
            current.behavior(),
            display,
            current.messages(),
            current.skins()
        );
        save();
    }

    public void updateEnabled(boolean enabled) {
        FakePlayerSettings current = settings;
        settings = new FakePlayerSettings(
            enabled,
            current.movement(),
            current.behavior(),
            current.display(),
            current.messages(),
            current.skins()
        );
        save();
    }

    public void updateSkins(FakePlayerSettings.SkinsSettings skins) {
        FakePlayerSettings current = settings;
        settings = new FakePlayerSettings(
            current.enabled(),
            current.movement(),
            current.behavior(),
            current.display(),
            current.messages(),
            skins
        );
        save();
    }

    private void migrateLegacyBehaviorDefaults(FileConfiguration config) {
        int configVersion = config.getInt("config-version", 0);
        if (configVersion >= 5) {
            return;
        }

        boolean matchesLegacyBehaviorDefaults =
            config.getBoolean("behavior.invulnerable", true)
                && config.getBoolean("behavior.knockback-when-invulnerable", true)
                && config.getBoolean("behavior.gravity", true)
                && !config.getBoolean("behavior.immovable", false);

        if (matchesLegacyBehaviorDefaults) {
            plugin.getLogger().info("Migrating fake player behavior defaults to normal combat settings.");
            config.set("behavior.invulnerable", false);
        }

        config.set("config-version", 5);
        plugin.saveConfig();
    }
}

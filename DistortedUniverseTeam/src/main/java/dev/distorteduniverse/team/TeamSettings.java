package dev.distorteduniverse.team;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

public record TeamSettings(
    boolean enabled,
    boolean friendlyFire,
    GlowSettings glow,
    String defaultColor,
    int defaultMaxSize,
    AutoKitConfig autokit
) {
    public static TeamSettings from(ConfigurationSection config) {
        return new TeamSettings(
            getBoolean(config, "enabled", true),
            getBoolean(config, "friendly-fire", false),
            GlowSettings.from(config.getConfigurationSection("glow")),
            config.getString("default-color", "white"),
            getInt(config, "default-max-size", -1, -1, 100),
            AutoKitConfig.from(config.getConfigurationSection("autokit"))
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("enabled", enabled);
        config.set("friendly-fire", friendlyFire);
        glow.writeTo(config.createSection("glow"));
        config.set("default-color", defaultColor);
        config.set("default-max-size", defaultMaxSize);
        autokit.writeTo(config.createSection("autokit"));
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
        if (!config.isInt(path)) return fallback;
        int value = config.getInt(path);
        return Math.max(min, Math.min(max, value));
    }

    public record GlowSettings(
        boolean enabledByDefault,
        String defaultColor
    ) {
        public static GlowSettings from(ConfigurationSection config) {
            if (config == null) {
                return new GlowSettings(true, "white");
            }
            return new GlowSettings(
                config.isBoolean("enabled-by-default") ? config.getBoolean("enabled-by-default") : true,
                config.getString("default-color", "white")
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("enabled-by-default", enabledByDefault);
            config.set("default-color", defaultColor);
        }
    }

    public record AutoKitConfig(
        boolean enabled
    ) {
        public static AutoKitConfig from(ConfigurationSection config) {
            if (config == null) {
                return new AutoKitConfig(true);
            }
            return new AutoKitConfig(
                config.isBoolean("enabled") ? config.getBoolean("enabled") : true
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("enabled", enabled);
        }
    }
}

package dev.distorteduniverse.immortal;

import org.bukkit.configuration.ConfigurationSection;

public record ImmortalSettings(
    boolean enabled,
    double minimumHealth,
    boolean totemCompatibilityEnabled
) {
    public static final double MINIMUM_ALLOWED_HEALTH = 0.5D;
    public static final double MAXIMUM_ALLOWED_HEALTH = 1024.0D;

    public static ImmortalSettings from(ConfigurationSection config) {
        return new ImmortalSettings(
            getBoolean(config, "enabled", true),
            getDouble(config, "minimum-health", 1.0D, MINIMUM_ALLOWED_HEALTH, MAXIMUM_ALLOWED_HEALTH),
            getBoolean(config, "totem-compatibility.enabled", true)
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("enabled", enabled);
        config.set("minimum-health", minimumHealth);
        config.set("totem-compatibility.enabled", totemCompatibilityEnabled);
    }

    public String valueAsString(String path) {
        return switch (path) {
            case "enabled" -> Boolean.toString(enabled);
            case "minimum-health" -> Double.toString(minimumHealth);
            case "totem-compatibility.enabled" -> Boolean.toString(totemCompatibilityEnabled);
            default -> throw new IllegalArgumentException("Unknown setting path: " + path);
        };
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    private static double getDouble(ConfigurationSection config, String path, double fallback, double min, double max) {
        Object rawValue = config.get(path);
        if (!(rawValue instanceof Number number)) {
            return fallback;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}

package dev.distorteduniverse.fakeplayer;

import org.bukkit.configuration.ConfigurationSection;

public record FakePlayerSettings(
    boolean enabled,
    MovementSettings movement,
    BehaviorSettings behavior,
    SkinsSettings skins
) {
    public static FakePlayerSettings from(ConfigurationSection config) {
        return new FakePlayerSettings(
            getBoolean(config, "enabled", true),
            MovementSettings.from(config.getConfigurationSection("movement"), config.getConfigurationSection("wandering")),
            BehaviorSettings.from(config.getConfigurationSection("behavior")),
            SkinsSettings.from(config.getConfigurationSection("skins"))
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 2);
        config.set("enabled", enabled);
        movement.writeTo(config.createSection("movement"));
        behavior.writeTo(config.createSection("behavior"));
        skins.writeTo(config.createSection("skins"));
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    public record BehaviorSettings(
        boolean invulnerable,
        boolean gravity,
        boolean immovable
    ) {
        public static BehaviorSettings from(ConfigurationSection config) {
            if (config == null) {
                return new BehaviorSettings(false, true, false);
            }
            return new BehaviorSettings(
                getBoolean(config, "invulnerable", false),
                getBoolean(config, "gravity", true),
                getBoolean(config, "immovable", false)
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("invulnerable", invulnerable);
            config.set("gravity", gravity);
            config.set("immovable", immovable);
        }
    }

    public record MovementSettings(
        double speed,
        double arrivalDistance,
        int tickInterval,
        double wanderRadius
    ) {
        public static MovementSettings from(ConfigurationSection movement, ConfigurationSection legacyWandering) {
            if (movement != null) {
                return new MovementSettings(
                    getDouble(movement, "speed", 0.2, 0.05, 1.0),
                    getDouble(movement, "arrival-distance", 1.5, 0.5, 5.0),
                    getInt(movement, "tick-interval", 2, 1, 20),
                    getDouble(movement, "wander-radius", 10.0, 1.0, 100.0)
                );
            }

            if (legacyWandering != null) {
                return new MovementSettings(
                    0.2,
                    1.5,
                    getInt(legacyWandering, "tick-interval", 2, 1, 20),
                    getDouble(legacyWandering, "default-radius", 10.0, 1.0, 100.0)
                );
            }

            return new MovementSettings(0.2, 1.5, 2, 10.0);
        }

        public void writeTo(ConfigurationSection config) {
            config.set("speed", speed);
            config.set("arrival-distance", arrivalDistance);
            config.set("tick-interval", tickInterval);
            config.set("wander-radius", wanderRadius);
        }

        private static double getDouble(ConfigurationSection config, String path, double fallback, double min, double max) {
            if (!config.isSet(path)) {
                return fallback;
            }
            double value = config.getDouble(path);
            return Math.max(min, Math.min(max, value));
        }

        private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
            if (!config.isInt(path)) {
                return fallback;
            }
            int value = config.getInt(path);
            return Math.max(min, Math.min(max, value));
        }

        private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
            return config.isBoolean(path) ? config.getBoolean(path) : fallback;
        }
    }

    public record SkinsSettings(
        String folder,
        String defaultSkin
    ) {
        public static SkinsSettings from(ConfigurationSection config) {
            if (config == null) {
                return new SkinsSettings("skins", "default");
            }
            return new SkinsSettings(
                config.getString("folder", "skins"),
                config.getString("default", "default")
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("folder", folder);
            config.set("default", defaultSkin);
        }
    }
}

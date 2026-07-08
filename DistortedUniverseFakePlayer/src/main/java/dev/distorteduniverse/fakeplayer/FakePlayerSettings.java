package dev.distorteduniverse.fakeplayer;

import org.bukkit.configuration.ConfigurationSection;

public record FakePlayerSettings(
    boolean enabled,
    MovementSettings movement,
    BehaviorSettings behavior,
    DisplaySettings display,
    MessagesSettings messages,
    SkinsSettings skins
) {
    public static FakePlayerSettings from(ConfigurationSection config) {
        return new FakePlayerSettings(
            getBoolean(config, "enabled", true),
            MovementSettings.from(config.getConfigurationSection("movement"), config.getConfigurationSection("wandering")),
            BehaviorSettings.from(config.getConfigurationSection("behavior")),
            DisplaySettings.from(config.getConfigurationSection("display")),
            MessagesSettings.from(config.getConfigurationSection("messages")),
            SkinsSettings.from(config.getConfigurationSection("skins"))
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 5);
        config.set("enabled", enabled);
        movement.writeTo(config.createSection("movement"));
        behavior.writeTo(config.createSection("behavior"));
        display.writeTo(config.createSection("display"));
        messages.writeTo(config.createSection("messages"));
        skins.writeTo(config.createSection("skins"));
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    public record BehaviorSettings(
        boolean invulnerable,
        boolean knockbackWhenInvulnerable,
        boolean gravity,
        boolean immovable
    ) {
        public static BehaviorSettings from(ConfigurationSection config) {
            if (config == null) {
                return new BehaviorSettings(false, true, true, false);
            }
            return new BehaviorSettings(
                getBoolean(config, "invulnerable", false),
                getBoolean(config, "knockback-when-invulnerable", true),
                getBoolean(config, "gravity", true),
                getBoolean(config, "immovable", false)
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("invulnerable", invulnerable);
            config.set("knockback-when-invulnerable", knockbackWhenInvulnerable);
            config.set("gravity", gravity);
            config.set("immovable", immovable);
        }
    }

    public record DisplaySettings(
        boolean tabList
    ) {
        public static DisplaySettings from(ConfigurationSection config) {
            if (config == null) {
                return new DisplaySettings(false);
            }
            return new DisplaySettings(getBoolean(config, "tab-list", false));
        }

        public void writeTo(ConfigurationSection config) {
            config.set("tab-list", tabList);
        }
    }

    public record MessagesSettings(
        boolean usePlayerEventSettings,
        FakePlayerBroadcastService.MessageSettings join,
        FakePlayerBroadcastService.MessageSettings leave,
        FakePlayerBroadcastService.MessageSettings deathLeave,
        FakePlayerBroadcastService.MessageSettings death
    ) {
        public static MessagesSettings from(ConfigurationSection config) {
            if (config == null) {
                return defaults();
            }

            return new MessagesSettings(
                getBoolean(config, "use-player-event-settings", true),
                messageSettings(config.getConfigurationSection("join"), true, 64.0D, "<yellow><player_name> joined the game</yellow>"),
                messageSettings(config.getConfigurationSection("leave"), true, 64.0D, "<yellow><player_name> left the game</yellow>"),
                messageSettings(config.getConfigurationSection("death-leave"), true, 64.0D, "<yellow><player_name> left the game</yellow>"),
                messageSettings(config.getConfigurationSection("death"), true, 64.0D, "<player_name> died")
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("use-player-event-settings", usePlayerEventSettings);
            writeMessageSettings(config.createSection("join"), join);
            writeMessageSettings(config.createSection("leave"), leave);
            writeMessageSettings(config.createSection("death-leave"), deathLeave);
            writeMessageSettings(config.createSection("death"), death);
        }

        private static MessagesSettings defaults() {
            return new MessagesSettings(
                true,
                new FakePlayerBroadcastService.MessageSettings(true, 64.0D, "<yellow><player_name> joined the game</yellow>"),
                new FakePlayerBroadcastService.MessageSettings(true, 64.0D, "<yellow><player_name> left the game</yellow>"),
                new FakePlayerBroadcastService.MessageSettings(true, 64.0D, "<yellow><player_name> left the game</yellow>"),
                new FakePlayerBroadcastService.MessageSettings(true, 64.0D, "<player_name> died")
            );
        }

        private static FakePlayerBroadcastService.MessageSettings messageSettings(
            ConfigurationSection section,
            boolean enabledDefault,
            double radiusDefault,
            String templateDefault
        ) {
            if (section == null) {
                return new FakePlayerBroadcastService.MessageSettings(enabledDefault, radiusDefault, templateDefault);
            }
            return new FakePlayerBroadcastService.MessageSettings(
                getBoolean(section, "enabled", enabledDefault),
                getDouble(section, "radius", radiusDefault, 0.0D, 100000.0D),
                section.getString("template", templateDefault)
            );
        }

        private static void writeMessageSettings(ConfigurationSection section, FakePlayerBroadcastService.MessageSettings settings) {
            section.set("enabled", settings.enabled());
            section.set("radius", settings.radius());
            section.set("template", settings.template());
        }

        private static double getDouble(ConfigurationSection config, String path, double fallback, double min, double max) {
            if (!config.isSet(path)) {
                return fallback;
            }
            double value = config.getDouble(path);
            return Math.max(min, Math.min(max, value));
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

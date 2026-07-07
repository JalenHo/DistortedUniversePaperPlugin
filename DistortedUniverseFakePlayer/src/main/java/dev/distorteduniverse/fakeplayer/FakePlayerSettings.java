package dev.distorteduniverse.fakeplayer;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record FakePlayerSettings(
    boolean enabled,
    List<String> names,
    WanderingSettings wandering,
    BehaviorSettings behavior,
    MessagesSettings messages,
    ChatSettings chat,
    SkinsSettings skins
) {
    public static FakePlayerSettings from(ConfigurationSection config) {
        return new FakePlayerSettings(
            getBoolean(config, "enabled", true),
            getStringList(config, "names", List.of("Steve", "Alex", "Herobrine", "Notch")),
            WanderingSettings.from(config.getConfigurationSection("wandering")),
            BehaviorSettings.from(config.getConfigurationSection("behavior")),
            MessagesSettings.from(config.getConfigurationSection("messages")),
            ChatSettings.from(config.getConfigurationSection("chat")),
            SkinsSettings.from(config.getConfigurationSection("skins"))
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("enabled", enabled);
        config.set("names", names);
        wandering.writeTo(config.createSection("wandering"));
        behavior.writeTo(config.createSection("behavior"));
        messages.writeTo(config.createSection("messages"));
        chat.writeTo(config.createSection("chat"));
        skins.writeTo(config.createSection("skins"));
    }

    private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
        return config.isBoolean(path) ? config.getBoolean(path) : fallback;
    }

    private static List<String> getStringList(ConfigurationSection config, String path, List<String> fallback) {
        return config.isList(path) ? config.getStringList(path) : fallback;
    }

    public record BehaviorSettings(
        boolean invulnerable,
        boolean gravity,
        boolean immovable
    ) {
        public static BehaviorSettings from(ConfigurationSection config) {
            if (config == null) {
                return new BehaviorSettings(false, true, true);
            }
            return new BehaviorSettings(
                getBoolean(config, "invulnerable", false),
                getBoolean(config, "gravity", true),
                getBoolean(config, "immovable", true)
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("invulnerable", invulnerable);
            config.set("gravity", gravity);
            config.set("immovable", immovable);
        }
    }

    public record WanderingSettings(
        double defaultRadius,
        int tickInterval,
        boolean usePathfinding
    ) {
        public static WanderingSettings from(ConfigurationSection config) {
            if (config == null) {
                return new WanderingSettings(10.0, 40, true);
            }
            return new WanderingSettings(
                getDouble(config, "default-radius", 10.0, 1.0, 100.0),
                getInt(config, "tick-interval", 40, 5, 200),
                getBoolean(config, "use-pathfinding", true)
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("default-radius", defaultRadius);
            config.set("tick-interval", tickInterval);
            config.set("use-pathfinding", usePathfinding);
        }

        private static double getDouble(ConfigurationSection config, String path, double fallback, double min, double max) {
            if (!config.isDouble(path)) return fallback;
            double value = config.getDouble(path);
            return Math.max(min, Math.min(max, value));
        }

        private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
            if (!config.isInt(path)) return fallback;
            int value = config.getInt(path);
            return Math.max(min, Math.min(max, value));
        }

        private static boolean getBoolean(ConfigurationSection config, String path, boolean fallback) {
            return config.isBoolean(path) ? config.getBoolean(path) : fallback;
        }
    }

    public record MessagesSettings(
        MessageTemplate join,
        MessageTemplate leave,
        MessageTemplate death
    ) {
        public static MessagesSettings from(ConfigurationSection config) {
            if (config == null) {
                return new MessagesSettings(
                    new MessageTemplate(true, "<yellow>{player} joined</yellow>"),
                    new MessageTemplate(true, "<yellow>{player} left</yellow>"),
                    new MessageTemplate(true, "<red>{player} died</red>")
                );
            }
            return new MessagesSettings(
                MessageTemplate.from(config.getConfigurationSection("join")),
                MessageTemplate.from(config.getConfigurationSection("leave")),
                MessageTemplate.from(config.getConfigurationSection("death"))
            );
        }

        public void writeTo(ConfigurationSection config) {
            join.writeTo(config.createSection("join"));
            leave.writeTo(config.createSection("leave"));
            death.writeTo(config.createSection("death"));
        }
    }

    public record MessageTemplate(boolean enabled, String template) {
        public static MessageTemplate from(ConfigurationSection config) {
            if (config == null) {
                return new MessageTemplate(true, "");
            }
            return new MessageTemplate(
                config.isBoolean("enabled") ? config.getBoolean("enabled") : true,
                config.getString("template", "")
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("enabled", enabled);
            config.set("template", template);
        }
    }

    public record ChatSettings(
        boolean enabled,
        Map<String, String> responses
    ) {
        public static ChatSettings from(ConfigurationSection config) {
            if (config == null) {
                return new ChatSettings(true, Map.of());
            }
            Map<String, String> responses = new HashMap<>();
            if (config.isConfigurationSection("responses")) {
                for (Map.Entry<String, Object> entry : config.getConfigurationSection("responses").getValues(false).entrySet()) {
                    responses.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            return new ChatSettings(
                config.isBoolean("enabled") ? config.getBoolean("enabled") : true,
                responses
            );
        }

        public void writeTo(ConfigurationSection config) {
            config.set("enabled", enabled);
            for (Map.Entry<String, String> entry : responses.entrySet()) {
                config.set("responses." + entry.getKey(), entry.getValue());
            }
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

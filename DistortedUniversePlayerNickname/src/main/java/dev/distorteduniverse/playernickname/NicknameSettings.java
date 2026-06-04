package dev.distorteduniverse.playernickname;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;

public record NicknameSettings(
    boolean enabled,
    ApplySettings apply,
    AboveHeadSettings aboveHead,
    ScoreboardSettings scoreboard,
    ValidationSettings validation
) {
    public static NicknameSettings from(ConfigurationSection config) {
        return new NicknameSettings(
            getBoolean(config, "enabled", true),
            new ApplySettings(
                getBoolean(config, "apply.chat-display-name", true),
                getBoolean(config, "apply.tab-list-name", true)
            ),
            new AboveHeadSettings(
                getMode(config, "above-head.mode", AboveHeadMode.TEXT_DISPLAY),
                getBoolean(config, "above-head.hide-vanilla-name", true),
                getDouble(config, "above-head.y-offset", 2.25D, 0.0D, 8.0D),
                getInt(config, "above-head.update-interval-ticks", 2, 1, 200),
                getDouble(config, "above-head.view-range", 64.0D, 1.0D, 256.0D),
                getBoolean(config, "above-head.shadowed", true),
                getBoolean(config, "above-head.see-through", false),
                getBoolean(config, "above-head.default-background", false)
            ),
            new ScoreboardSettings(getBoolean(config, "scoreboard.override-existing-teams", false)),
            new ValidationSettings(
                getBoolean(config, "validation.allow-minimessage", true),
                getBoolean(config, "validation.allow-spaces", true),
                getInt(config, "validation.min-plain-length", 1, 1, 64),
                getInt(config, "validation.max-plain-length", 32, 1, 128)
            )
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("enabled", enabled);
        config.set("apply.chat-display-name", apply.chatDisplayName());
        config.set("apply.tab-list-name", apply.tabListName());
        config.set("above-head.mode", aboveHead.mode().configValue());
        config.set("above-head.hide-vanilla-name", aboveHead.hideVanillaName());
        config.set("above-head.y-offset", aboveHead.yOffset());
        config.set("above-head.update-interval-ticks", aboveHead.updateIntervalTicks());
        config.set("above-head.view-range", aboveHead.viewRange());
        config.set("above-head.shadowed", aboveHead.shadowed());
        config.set("above-head.see-through", aboveHead.seeThrough());
        config.set("above-head.default-background", aboveHead.defaultBackground());
        config.set("scoreboard.override-existing-teams", scoreboard.overrideExistingTeams());
        config.set("validation.allow-minimessage", validation.allowMiniMessage());
        config.set("validation.allow-spaces", validation.allowSpaces());
        config.set("validation.min-plain-length", validation.minPlainLength());
        config.set("validation.max-plain-length", validation.maxPlainLength());
    }

    public String valueAsString(String path) {
        return switch (path) {
            case "enabled" -> Boolean.toString(enabled);
            case "apply.chat-display-name" -> Boolean.toString(apply.chatDisplayName());
            case "apply.tab-list-name" -> Boolean.toString(apply.tabListName());
            case "above-head.mode" -> aboveHead.mode().configValue();
            case "above-head.hide-vanilla-name" -> Boolean.toString(aboveHead.hideVanillaName());
            case "above-head.y-offset" -> Double.toString(aboveHead.yOffset());
            case "above-head.update-interval-ticks" -> Integer.toString(aboveHead.updateIntervalTicks());
            case "above-head.view-range" -> Double.toString(aboveHead.viewRange());
            case "above-head.shadowed" -> Boolean.toString(aboveHead.shadowed());
            case "above-head.see-through" -> Boolean.toString(aboveHead.seeThrough());
            case "above-head.default-background" -> Boolean.toString(aboveHead.defaultBackground());
            case "scoreboard.override-existing-teams" -> Boolean.toString(scoreboard.overrideExistingTeams());
            case "validation.allow-minimessage" -> Boolean.toString(validation.allowMiniMessage());
            case "validation.allow-spaces" -> Boolean.toString(validation.allowSpaces());
            case "validation.min-plain-length" -> Integer.toString(validation.minPlainLength());
            case "validation.max-plain-length" -> Integer.toString(validation.maxPlainLength());
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

    private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
        if (!config.isInt(path)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, config.getInt(path)));
    }

    private static AboveHeadMode getMode(ConfigurationSection config, String path, AboveHeadMode fallback) {
        String value = config.getString(path);
        if (value == null) {
            return fallback;
        }
        try {
            return AboveHeadMode.fromConfigValue(value);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public record ApplySettings(boolean chatDisplayName, boolean tabListName) {
    }

    public record AboveHeadSettings(
        AboveHeadMode mode,
        boolean hideVanillaName,
        double yOffset,
        int updateIntervalTicks,
        double viewRange,
        boolean shadowed,
        boolean seeThrough,
        boolean defaultBackground
    ) {
    }

    public record ScoreboardSettings(boolean overrideExistingTeams) {
    }

    public record ValidationSettings(
        boolean allowMiniMessage,
        boolean allowSpaces,
        int minPlainLength,
        int maxPlainLength
    ) {
        public ValidationSettings {
            maxPlainLength = Math.max(minPlainLength, maxPlainLength);
        }
    }

    public enum AboveHeadMode {
        TEXT_DISPLAY("text-display"),
        SCOREBOARD_AFFIX("scoreboard-affix"),
        DISABLED("disabled");

        private final String configValue;

        AboveHeadMode(String configValue) {
            this.configValue = configValue;
        }

        public String configValue() {
            return configValue;
        }

        public static AboveHeadMode fromConfigValue(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (AboveHeadMode mode : values()) {
                if (mode.configValue.equals(normalized)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException("Unknown above-head mode: " + value);
        }
    }
}


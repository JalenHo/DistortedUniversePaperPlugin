package dev.distorteduniverse.playernickname;

import java.util.List;
import java.util.Locale;

public final class NicknameSettingParser {
    public static final List<String> PATHS = List.of(
        "enabled",
        "apply.chat-display-name",
        "apply.tab-list-name",
        "above-head.mode",
        "above-head.hide-vanilla-name",
        "above-head.y-offset",
        "above-head.update-interval-ticks",
        "above-head.view-range",
        "above-head.shadowed",
        "above-head.see-through",
        "above-head.default-background",
        "scoreboard.override-existing-teams",
        "validation.allow-minimessage",
        "validation.allow-spaces",
        "validation.min-plain-length",
        "validation.max-plain-length"
    );

    private NicknameSettingParser() {
    }

    public static String normalizePath(String path) {
        return path.trim().toLowerCase(Locale.ROOT);
    }

    public static Object parseSettingValue(String path, String rawValue) {
        String normalizedPath = normalizePath(path);
        if (!PATHS.contains(normalizedPath)) {
            throw new IllegalArgumentException("Unknown setting path: " + path);
        }

        String value = rawValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Value cannot be empty.");
        }

        return switch (normalizedPath) {
            case "enabled",
                "apply.chat-display-name",
                "apply.tab-list-name",
                "above-head.hide-vanilla-name",
                "above-head.shadowed",
                "above-head.see-through",
                "above-head.default-background",
                "scoreboard.override-existing-teams",
                "validation.allow-minimessage",
                "validation.allow-spaces" -> parseBoolean(value);
            case "above-head.mode" -> NicknameSettings.AboveHeadMode.fromConfigValue(value).configValue();
            case "above-head.y-offset" -> parseDouble(value, 0.0D, 8.0D, "Y offset");
            case "above-head.view-range" -> parseDouble(value, 1.0D, 256.0D, "View range");
            case "above-head.update-interval-ticks" -> parseInt(value, 1, 200, "Update interval ticks");
            case "validation.min-plain-length" -> parseInt(value, 1, 64, "Minimum plain length");
            case "validation.max-plain-length" -> parseInt(value, 1, 128, "Maximum plain length");
            default -> throw new IllegalArgumentException("Unsupported setting path: " + path);
        };
    }

    public static boolean parseBoolean(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes", "1", "enable", "enabled" -> true;
            case "false", "off", "no", "0", "disable", "disabled" -> false;
            default -> throw new IllegalArgumentException("Expected boolean value: true/false, on/off, yes/no.");
        };
    }

    private static double parseDouble(String value, double min, double max, String label) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    private static int parseInt(String value, int min, int max, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }
}


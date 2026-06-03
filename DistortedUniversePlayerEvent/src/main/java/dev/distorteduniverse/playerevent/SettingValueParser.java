package dev.distorteduniverse.playerevent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.SoundCategory;

public final class SettingValueParser {
    public static final List<String> MODULES = List.of(
        "death-sound",
        "death-message",
        "leave-message",
        "join-message",
        "death-kick"
    );

    public static final List<String> PATHS = List.of(
        "death-sound.enabled",
        "death-sound.radius",
        "death-sound.sound",
        "death-sound.category",
        "death-sound.volume",
        "death-sound.pitch",
        "death-sound.suppress-vanilla",
        "death-message.enabled",
        "death-message.radius",
        "death-message.respect-gamerule",
        "death-message.template",
        "leave-message.enabled",
        "leave-message.radius",
        "leave-message.template",
        "join-message.enabled",
        "join-message.radius",
        "join-message.template",
        "death-kick.enabled",
        "death-kick.delay-ticks",
        "death-kick.kick-message",
        "death-kick.show-leave-message",
        "death-kick.leave-radius",
        "death-kick.leave-template"
    );

    private static final Pattern SOUND_KEY_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private SettingValueParser() {
    }

    public static String normalizePath(String path) {
        return path.trim().toLowerCase(Locale.ROOT);
    }

    public static String enabledPathForModule(String module) {
        String normalized = normalizePath(module);
        if (!MODULES.contains(normalized)) {
            throw new IllegalArgumentException("Unknown module: " + module);
        }
        return normalized + ".enabled";
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
            case "death-sound.enabled",
                "death-sound.suppress-vanilla",
                "death-message.enabled",
                "death-message.respect-gamerule",
                "leave-message.enabled",
                "join-message.enabled",
                "death-kick.enabled",
                "death-kick.show-leave-message" -> parseBoolean(value);
            case "death-sound.radius",
                "death-message.radius",
                "leave-message.radius",
                "join-message.radius",
                "death-kick.leave-radius" -> parseDouble(value, 0.0D, 100000.0D, "Radius");
            case "death-sound.volume" -> parseDouble(value, 0.0D, 10.0D, "Volume");
            case "death-sound.pitch" -> parseDouble(value, 0.5D, 2.0D, "Pitch");
            case "death-kick.delay-ticks" -> parseInt(value, 0, 1200, "Delay ticks");
            case "death-sound.sound" -> normalizeSoundKey(value);
            case "death-sound.category" -> parseSoundCategory(value).name();
            case "death-message.template",
                "leave-message.template",
                "join-message.template",
                "death-kick.kick-message",
                "death-kick.leave-template" -> value;
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

    public static SoundCategory parseSoundCategory(String value) {
        try {
            return SoundCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown sound category: " + value);
        }
    }

    public static String normalizeSoundKey(String rawValue) {
        String value = rawValue.trim().toLowerCase(Locale.ROOT);
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        if (!SOUND_KEY_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Sound must be a namespaced key like minecraft:entity.wither.death.");
        }
        return value;
    }

    public static boolean isTemplatePath(String path) {
        String normalizedPath = normalizePath(path);
        return normalizedPath.equals("death-message.template")
            || normalizedPath.equals("leave-message.template")
            || normalizedPath.equals("join-message.template")
            || normalizedPath.equals("death-kick.kick-message")
            || normalizedPath.equals("death-kick.leave-template");
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

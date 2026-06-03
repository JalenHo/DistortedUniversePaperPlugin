package dev.distorteduniverse.playerevent;

import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;

public record PluginSettings(
    DeathSoundSettings deathSound,
    DeathMessageSettings deathMessage,
    LeaveMessageSettings leaveMessage,
    JoinMessageSettings joinMessage,
    DeathKickSettings deathKick
) {
    public static PluginSettings from(ConfigurationSection config) {
        return new PluginSettings(
            new DeathSoundSettings(
                getBoolean(config, "death-sound.enabled", true),
                getDouble(config, "death-sound.radius", 64.0D, 0.0D, 100000.0D),
                getSoundKey(config, "death-sound.sound", "minecraft:entity.wither.death"),
                getCategory(config, "death-sound.category", SoundCategory.PLAYERS),
                (float) getDouble(config, "death-sound.volume", 1.0D, 0.0D, 10.0D),
                (float) getDouble(config, "death-sound.pitch", 1.0D, 0.5D, 2.0D),
                getBoolean(config, "death-sound.suppress-vanilla", true)
            ),
            new DeathMessageSettings(
                getBoolean(config, "death-message.enabled", true),
                getDouble(config, "death-message.radius", 64.0D, 0.0D, 100000.0D),
                getBoolean(config, "death-message.respect-gamerule", true),
                getString(config, "death-message.template", "<death_message>")
            ),
            new LeaveMessageSettings(
                getBoolean(config, "leave-message.enabled", true),
                getDouble(config, "leave-message.radius", 64.0D, 0.0D, 100000.0D),
                getString(config, "leave-message.template", "<quit_message>")
            ),
            new JoinMessageSettings(
                getBoolean(config, "join-message.enabled", true),
                getDouble(config, "join-message.radius", 64.0D, 0.0D, 100000.0D),
                getString(config, "join-message.template", "<join_message>")
            ),
            new DeathKickSettings(
                getBoolean(config, "death-kick.enabled", true),
                getInt(config, "death-kick.delay-ticks", 0, 0, 1200),
                getString(config, "death-kick.kick-message", "You died."),
                getBoolean(config, "death-kick.show-leave-message", false),
                getDouble(config, "death-kick.leave-radius", 64.0D, 0.0D, 100000.0D),
                getString(config, "death-kick.leave-template", "<yellow><player_name> left the game</yellow>")
            )
        );
    }

    public void writeTo(ConfigurationSection config) {
        config.set("config-version", 1);
        config.set("death-sound.enabled", deathSound.enabled());
        config.set("death-sound.radius", deathSound.radius());
        config.set("death-sound.sound", deathSound.sound());
        config.set("death-sound.category", deathSound.category().name());
        config.set("death-sound.volume", deathSound.volume());
        config.set("death-sound.pitch", deathSound.pitch());
        config.set("death-sound.suppress-vanilla", deathSound.suppressVanilla());
        config.set("death-message.enabled", deathMessage.enabled());
        config.set("death-message.radius", deathMessage.radius());
        config.set("death-message.respect-gamerule", deathMessage.respectGamerule());
        config.set("death-message.template", deathMessage.template());
        config.set("leave-message.enabled", leaveMessage.enabled());
        config.set("leave-message.radius", leaveMessage.radius());
        config.set("leave-message.template", leaveMessage.template());
        config.set("join-message.enabled", joinMessage.enabled());
        config.set("join-message.radius", joinMessage.radius());
        config.set("join-message.template", joinMessage.template());
        config.set("death-kick.enabled", deathKick.enabled());
        config.set("death-kick.delay-ticks", deathKick.delayTicks());
        config.set("death-kick.kick-message", deathKick.kickMessage());
        config.set("death-kick.show-leave-message", deathKick.showLeaveMessage());
        config.set("death-kick.leave-radius", deathKick.leaveRadius());
        config.set("death-kick.leave-template", deathKick.leaveTemplate());
    }

    public String valueAsString(String path) {
        return switch (path) {
            case "death-sound.enabled" -> Boolean.toString(deathSound.enabled());
            case "death-sound.radius" -> Double.toString(deathSound.radius());
            case "death-sound.sound" -> deathSound.sound();
            case "death-sound.category" -> deathSound.category().name();
            case "death-sound.volume" -> Float.toString(deathSound.volume());
            case "death-sound.pitch" -> Float.toString(deathSound.pitch());
            case "death-sound.suppress-vanilla" -> Boolean.toString(deathSound.suppressVanilla());
            case "death-message.enabled" -> Boolean.toString(deathMessage.enabled());
            case "death-message.radius" -> Double.toString(deathMessage.radius());
            case "death-message.respect-gamerule" -> Boolean.toString(deathMessage.respectGamerule());
            case "death-message.template" -> deathMessage.template();
            case "leave-message.enabled" -> Boolean.toString(leaveMessage.enabled());
            case "leave-message.radius" -> Double.toString(leaveMessage.radius());
            case "leave-message.template" -> leaveMessage.template();
            case "join-message.enabled" -> Boolean.toString(joinMessage.enabled());
            case "join-message.radius" -> Double.toString(joinMessage.radius());
            case "join-message.template" -> joinMessage.template();
            case "death-kick.enabled" -> Boolean.toString(deathKick.enabled());
            case "death-kick.delay-ticks" -> Integer.toString(deathKick.delayTicks());
            case "death-kick.kick-message" -> deathKick.kickMessage();
            case "death-kick.show-leave-message" -> Boolean.toString(deathKick.showLeaveMessage());
            case "death-kick.leave-radius" -> Double.toString(deathKick.leaveRadius());
            case "death-kick.leave-template" -> deathKick.leaveTemplate();
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
        return Math.max(min, Math.min(max, value));
    }

    private static int getInt(ConfigurationSection config, String path, int fallback, int min, int max) {
        if (!config.isInt(path)) {
            return fallback;
        }
        int value = config.getInt(path);
        return Math.max(min, Math.min(max, value));
    }

    private static String getString(ConfigurationSection config, String path, String fallback) {
        String value = config.getString(path);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String getSoundKey(ConfigurationSection config, String path, String fallback) {
        try {
            return SettingValueParser.normalizeSoundKey(getString(config, path, fallback));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static SoundCategory getCategory(ConfigurationSection config, String path, SoundCategory fallback) {
        String value = config.getString(path);
        if (value == null) {
            return fallback;
        }
        try {
            return SoundCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public record DeathSoundSettings(
        boolean enabled,
        double radius,
        String sound,
        SoundCategory category,
        float volume,
        float pitch,
        boolean suppressVanilla
    ) {
    }

    public record DeathMessageSettings(
        boolean enabled,
        double radius,
        boolean respectGamerule,
        String template
    ) {
    }

    public record LeaveMessageSettings(
        boolean enabled,
        double radius,
        String template
    ) {
    }

    public record JoinMessageSettings(
        boolean enabled,
        double radius,
        String template
    ) {
    }

    public record DeathKickSettings(
        boolean enabled,
        int delayTicks,
        String kickMessage,
        boolean showLeaveMessage,
        double leaveRadius,
        String leaveTemplate
    ) {
    }
}

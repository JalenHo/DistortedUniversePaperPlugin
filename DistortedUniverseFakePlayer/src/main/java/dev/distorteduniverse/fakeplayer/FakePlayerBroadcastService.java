package dev.distorteduniverse.fakeplayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.text.DecimalFormat;

public class FakePlayerBroadcastService {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");

    private final DistortedUniverseFakePlayerPlugin plugin;
    private final FakePlayerSettingsService settingsService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    public FakePlayerBroadcastService(
        DistortedUniverseFakePlayerPlugin plugin,
        FakePlayerSettingsService settingsService
    ) {
        this.plugin = plugin;
        this.settingsService = settingsService;
    }

    public void broadcastJoin(String playerName, Location location) {
        MessageSettings settings = resolveJoinSettings();
        if (!settings.enabled()) {
            return;
        }

        Component vanilla = Component.text(playerName + " joined the game", NamedTextColor.YELLOW);
        Component message = format(settings.template(), playerName, location, Component.empty(), Component.empty(), vanilla);
        sendNearby(message, location, settings.radius());
    }

    public void broadcastLeave(String playerName, Location location, boolean afterDeath) {
        MessageSettings settings = resolveLeaveSettings(afterDeath);
        if (!settings.enabled()) {
            return;
        }

        Component vanilla = Component.text(playerName + " left the game", NamedTextColor.YELLOW);
        Component message = format(settings.template(), playerName, location, Component.empty(), vanilla, Component.empty());
        sendNearby(message, location, settings.radius());
    }

    public void broadcastDeath(String playerName, Location location) {
        MessageSettings settings = resolveDeathSettings();
        if (!settings.enabled()) {
            return;
        }

        Component vanilla = Component.text(playerName + " died");
        Component message = format(settings.template(), playerName, location, vanilla, Component.empty(), Component.empty());
        sendNearby(message, location, settings.radius());
    }

    private MessageSettings resolveJoinSettings() {
        FakePlayerSettings.MessagesSettings messages = settingsService.settings().messages();
        if (messages.usePlayerEventSettings()) {
            MessageSettings bridged = bridgeJoinSettings();
            if (bridged != null) {
                return bridged;
            }
        }
        return messages.join();
    }

    private MessageSettings resolveLeaveSettings(boolean afterDeath) {
        FakePlayerSettings.MessagesSettings messages = settingsService.settings().messages();
        if (messages.usePlayerEventSettings()) {
            if (afterDeath) {
                MessageSettings deathKickLeave = bridgeDeathKickLeaveSettings();
                if (deathKickLeave != null) {
                    return deathKickLeave;
                }
            }
            MessageSettings bridged = bridgeLeaveSettings();
            if (bridged != null) {
                return bridged;
            }
        }
        return afterDeath ? messages.deathLeave() : messages.leave();
    }

    private MessageSettings resolveDeathSettings() {
        FakePlayerSettings.MessagesSettings messages = settingsService.settings().messages();
        if (messages.usePlayerEventSettings()) {
            MessageSettings bridged = bridgeDeathSettings();
            if (bridged != null) {
                return bridged;
            }
        }
        return messages.death();
    }

    private MessageSettings bridgeJoinSettings() {
        return readPlayerEventMessage("join-message.enabled", "join-message.radius", "join-message.template");
    }

    private MessageSettings bridgeLeaveSettings() {
        return readPlayerEventMessage("leave-message.enabled", "leave-message.radius", "leave-message.template");
    }

    private MessageSettings bridgeDeathSettings() {
        return readPlayerEventMessage("death-message.enabled", "death-message.radius", "death-message.template");
    }

    private MessageSettings bridgeDeathKickLeaveSettings() {
        Plugin playerEvent = Bukkit.getPluginManager().getPlugin("DistortedUniversePlayerEvent");
        if (playerEvent == null || !playerEvent.isEnabled()) {
            return null;
        }

        PlayerEventConfigReader reader = new PlayerEventConfigReader(playerEvent);
        boolean deathKickEnabled = reader.getBoolean("death-kick.enabled", false);
        boolean showLeave = reader.getBoolean("death-kick.show-leave-message", false);
        if (!deathKickEnabled || !showLeave) {
            return bridgeLeaveSettings();
        }

        return new MessageSettings(
            true,
            reader.getDouble("death-kick.leave-radius", 64.0D),
            reader.getString("death-kick.leave-template", "<yellow><player_name> left the game</yellow>")
        );
    }

    private MessageSettings readPlayerEventMessage(String enabledPath, String radiusPath, String templatePath) {
        Plugin playerEvent = Bukkit.getPluginManager().getPlugin("DistortedUniversePlayerEvent");
        if (playerEvent == null || !playerEvent.isEnabled()) {
            return null;
        }

        PlayerEventConfigReader reader = new PlayerEventConfigReader(playerEvent);
        return new MessageSettings(
            reader.getBoolean(enabledPath, true),
            reader.getDouble(radiusPath, 64.0D),
            reader.getString(templatePath, "<player_name>")
        );
    }

    private Component format(
        String template,
        String playerName,
        Location location,
        Component deathMessage,
        Component quitMessage,
        Component joinMessage
    ) {
        return miniMessage.deserialize(
            template,
            Placeholder.unparsed("player_name", playerName),
            Placeholder.component("player", Component.text(playerName)),
            Placeholder.component("display_name", Component.text(playerName)),
            Placeholder.unparsed("world", worldName(location)),
            Placeholder.unparsed("x", COORDINATE_FORMAT.format(location.getX())),
            Placeholder.unparsed("y", COORDINATE_FORMAT.format(location.getY())),
            Placeholder.unparsed("z", COORDINATE_FORMAT.format(location.getZ())),
            Placeholder.component("death_message", deathMessage),
            Placeholder.component("quit_message", quitMessage),
            Placeholder.component("join_message", joinMessage)
        );
    }

    private void sendNearby(Component message, Location origin, double radius) {
        if (plainText.serialize(message).isBlank() || origin.getWorld() == null) {
            return;
        }

        double radiusSquared = radius * radius;
        for (Player recipient : origin.getWorld().getPlayers()) {
            if (recipient.getLocation().distanceSquared(origin) <= radiusSquared) {
                recipient.sendMessage(message);
            }
        }
    }

    private static String worldName(Location location) {
        return location.getWorld() == null ? "unknown" : location.getWorld().getName();
    }

    private static final class PlayerEventConfigReader {
        private final org.bukkit.configuration.file.FileConfiguration config;

        private PlayerEventConfigReader(Plugin plugin) {
            this.config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "config.yml")
            );
        }

        private boolean getBoolean(String path, boolean fallback) {
            return config.isBoolean(path) ? config.getBoolean(path) : fallback;
        }

        private double getDouble(String path, double fallback) {
            return config.isSet(path) ? config.getDouble(path) : fallback;
        }

        private String getString(String path, String fallback) {
            String value = config.getString(path);
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    public record MessageSettings(boolean enabled, double radius, String template) {}
}

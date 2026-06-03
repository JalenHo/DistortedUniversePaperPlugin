package dev.distorteduniverse.playerevent;

import java.text.DecimalFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class MessageFormatter {
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    public Component deathMessage(String template, Player player, Location location, Component vanillaDeathMessage) {
        return format(template, player, location, nonNull(vanillaDeathMessage), Component.empty(), Component.empty());
    }

    public Component quitMessage(String template, Player player, Location location, Component vanillaQuitMessage) {
        return format(template, player, location, Component.empty(), nonNull(vanillaQuitMessage), Component.empty());
    }

    public Component joinMessage(String template, Player player, Location location, Component vanillaJoinMessage) {
        return format(template, player, location, Component.empty(), Component.empty(), nonNull(vanillaJoinMessage));
    }

    public Component playerMessage(String template, Player player, Location location) {
        return format(template, player, location, Component.empty(), Component.empty(), Component.empty());
    }

    public boolean isVisiblyEmpty(Component component) {
        return plainText.serialize(component).isBlank();
    }

    public boolean isTemplateValid(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        try {
            miniMessage.deserialize(
                template,
                Placeholder.unparsed("player_name", "Steve"),
                Placeholder.component("player", Component.text("Steve")),
                Placeholder.component("display_name", Component.text("Steve")),
                Placeholder.unparsed("world", "world"),
                Placeholder.unparsed("x", "0"),
                Placeholder.unparsed("y", "64"),
                Placeholder.unparsed("z", "0"),
                Placeholder.component("death_message", Component.text("Steve died")),
                Placeholder.component("quit_message", Component.text("Steve left the game")),
                Placeholder.component("join_message", Component.text("Steve joined the game"))
            );
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Component format(
        String template,
        Player player,
        Location location,
        Component vanillaDeathMessage,
        Component vanillaQuitMessage,
        Component vanillaJoinMessage
    ) {
        TagResolver resolver = TagResolver.builder()
            .resolver(Placeholder.component("player", player.displayName()))
            .resolver(Placeholder.unparsed("player_name", player.getName()))
            .resolver(Placeholder.component("display_name", player.displayName()))
            .resolver(Placeholder.unparsed("world", worldName(location)))
            .resolver(Placeholder.unparsed("x", COORDINATE_FORMAT.format(location.getX())))
            .resolver(Placeholder.unparsed("y", COORDINATE_FORMAT.format(location.getY())))
            .resolver(Placeholder.unparsed("z", COORDINATE_FORMAT.format(location.getZ())))
            .resolver(Placeholder.component("death_message", vanillaDeathMessage))
            .resolver(Placeholder.component("quit_message", vanillaQuitMessage))
            .resolver(Placeholder.component("join_message", vanillaJoinMessage))
            .build();
        return miniMessage.deserialize(template, resolver);
    }

    private static Component nonNull(Component component) {
        return component == null ? Component.empty() : component;
    }

    private static String worldName(Location location) {
        return location.getWorld() == null ? "unknown" : location.getWorld().getName();
    }
}

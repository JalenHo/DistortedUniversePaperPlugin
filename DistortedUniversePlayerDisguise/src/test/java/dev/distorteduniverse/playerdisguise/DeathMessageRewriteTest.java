package dev.distorteduniverse.playerdisguise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

// Validates the core mechanism the kill-feed rewrite in DisguiseListener relies on: that
// Adventure's replaceText descends into a translatable death message's arguments (where the
// victim/killer names live), and only replaces whole-word matches.
class DeathMessageRewriteTest {
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static Component replaceName(Component message, String realName, String disguiseName) {
        return message.replaceText(builder -> builder
            .match(Pattern.compile("\\b" + Pattern.quote(realName) + "\\b"))
            .replacement(disguiseName));
    }

    @Test
    void replacesNameInTranslatableDeathMessageArguments() {
        // "GayAssRaccoon was slain by nessie2g"
        TranslatableComponent message = Component.translatable(
            "death.attack.player",
            Component.text("GayAssRaccoon"),
            Component.text("nessie2g")
        );

        Component victimRewritten = replaceName(message, "GayAssRaccoon", "Notch");
        Component bothRewritten = replaceName(victimRewritten, "nessie2g", "Herobrine");

        TranslatableComponent out = (TranslatableComponent) bothRewritten;
        assertEquals("Notch", PLAIN.serialize(out.args().get(0)));
        assertEquals("Herobrine", PLAIN.serialize(out.args().get(1)));
    }

    @Test
    void replacesNameInPlainTextDeathMessage() {
        Component message = Component.text("GayAssRaccoon fell from a high place");
        Component out = replaceName(message, "GayAssRaccoon", "Notch");
        assertEquals("Notch fell from a high place", PLAIN.serialize(out));
    }

    @Test
    void doesNotReplaceWhenRealNameIsSubstringOfAnotherWord() {
        Component message = Component.text("Steve was killed by SteveTheKiller");
        Component out = replaceName(message, "Steve", "Notch");
        // Whole-word match: the standalone "Steve" changes, "SteveTheKiller" stays intact.
        assertEquals("Notch was killed by SteveTheKiller", PLAIN.serialize(out));
    }

    @Test
    void leavesMessageUnchangedWhenNameAbsent() {
        Component message = Component.text("Steve drowned");
        Component out = replaceName(message, "Alex", "Notch");
        assertTrue(PLAIN.serialize(out).equals("Steve drowned"));
    }
}

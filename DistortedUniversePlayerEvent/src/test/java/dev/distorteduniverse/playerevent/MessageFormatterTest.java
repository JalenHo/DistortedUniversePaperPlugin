package dev.distorteduniverse.playerevent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageFormatterTest {
    @Test
    void acceptsMiniMessageTemplatesWithConfiguredPlaceholders() {
        MessageFormatter formatter = new MessageFormatter();

        assertTrue(formatter.isTemplateValid("<death_message>"));
        assertTrue(formatter.isTemplateValid("<join_message>"));
        assertTrue(formatter.isTemplateValid("<yellow><player_name> left the game</yellow>"));
        assertTrue(formatter.isTemplateValid("<red><player_name></red> died at <x> <y> <z>"));
    }

    @Test
    void rejectsBlankTemplates() {
        MessageFormatter formatter = new MessageFormatter();

        assertFalse(formatter.isTemplateValid(""));
        assertFalse(formatter.isTemplateValid("   "));
    }
}

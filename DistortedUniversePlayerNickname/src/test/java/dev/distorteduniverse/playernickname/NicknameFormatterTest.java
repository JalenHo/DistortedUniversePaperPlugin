package dev.distorteduniverse.playernickname;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NicknameFormatterTest {
    private final NicknameFormatter formatter = new NicknameFormatter();

    @Test
    void acceptsValidMiniMessageNicknameWithSpaces() {
        NicknameSettings.ValidationSettings validation = new NicknameSettings.ValidationSettings(true, true, 1, 32);

        NicknameFormatter.FormatResult result = formatter.format("<gold>Golden Traveler</gold>", validation);

        assertTrue(result.success());
        assertTrue(result.plainText().contains("Golden Traveler"));
    }

    @Test
    void rejectsBlankVisibleOverLengthAndDisallowedSpaces() {
        assertFalse(formatter.format("   ", new NicknameSettings.ValidationSettings(true, true, 1, 32)).success());
        assertFalse(formatter.format("Too Long", new NicknameSettings.ValidationSettings(true, false, 1, 32)).success());
        assertFalse(formatter.format("123456", new NicknameSettings.ValidationSettings(true, true, 1, 5)).success());
    }

    @Test
    void rejectsInvalidMiniMessageWhenEnabled() {
        NicknameSettings.ValidationSettings validation = new NicknameSettings.ValidationSettings(true, true, 1, 32);

        assertFalse(formatter.format("<gold>missing close", validation).success());
    }
}


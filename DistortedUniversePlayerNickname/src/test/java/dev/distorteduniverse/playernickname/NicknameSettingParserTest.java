package dev.distorteduniverse.playernickname;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NicknameSettingParserTest {
    @Test
    void parsesBooleanAliases() {
        assertTrue(NicknameSettingParser.parseBoolean("on"));
        assertTrue(NicknameSettingParser.parseBoolean("enabled"));
        assertFalse(NicknameSettingParser.parseBoolean("off"));
        assertFalse(NicknameSettingParser.parseBoolean("disabled"));
        assertThrows(IllegalArgumentException.class, () -> NicknameSettingParser.parseBoolean("maybe"));
    }

    @Test
    void parsesModesAndNumericBounds() {
        assertEquals("text-display", NicknameSettingParser.parseSettingValue("above-head.mode", "text-display"));
        assertEquals("scoreboard-affix", NicknameSettingParser.parseSettingValue("above-head.mode", "scoreboard-affix"));
        assertEquals("disabled", NicknameSettingParser.parseSettingValue("above-head.mode", "disabled"));
        assertEquals(2.5D, NicknameSettingParser.parseSettingValue("above-head.y-offset", "2.5"));
        assertEquals(20, NicknameSettingParser.parseSettingValue("above-head.update-interval-ticks", "20"));

        assertThrows(IllegalArgumentException.class, () -> NicknameSettingParser.parseSettingValue("above-head.mode", "packet"));
        assertThrows(IllegalArgumentException.class, () -> NicknameSettingParser.parseSettingValue("above-head.view-range", "0"));
        assertThrows(IllegalArgumentException.class, () -> NicknameSettingParser.parseSettingValue("unknown.path", "true"));
    }
}


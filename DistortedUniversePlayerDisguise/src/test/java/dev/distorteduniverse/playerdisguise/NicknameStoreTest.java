package dev.distorteduniverse.playerdisguise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NicknameStoreTest {
    @TempDir
    File dataFolder;

    @Test
    void savesAndLoadsNickname() {
        Plugin plugin = fakePlugin(dataFolder);
        NicknameStore store = new NicknameStore(plugin);
        UUID playerId = UUID.randomUUID();

        assertTrue(store.set(playerId, "RealPlayer", "KingBob"));
        store.save();

        NicknameStore loaded = new NicknameStore(plugin);
        loaded.load();

        NicknameStore.NicknameEntry entry = loaded.entry(playerId).orElseThrow();
        assertEquals("RealPlayer", entry.name());
        assertEquals("KingBob", entry.nickname());
        assertEquals("KingBob", loaded.nickname(playerId).orElseThrow());
    }

    @Test
    void clearRemovesNickname() {
        Plugin plugin = fakePlugin(dataFolder);
        NicknameStore store = new NicknameStore(plugin);
        UUID playerId = UUID.randomUUID();
        store.set(playerId, "RealPlayer", "KingBob");

        assertTrue(store.clear(playerId));
        assertFalse(store.clear(playerId));
        assertTrue(store.nickname(playerId).isEmpty());
    }

    @Test
    void findsByRealNameCaseInsensitively() {
        Plugin plugin = fakePlugin(dataFolder);
        NicknameStore store = new NicknameStore(plugin);
        UUID playerId = UUID.randomUUID();
        store.set(playerId, "RealPlayer", "KingBob");

        assertEquals(playerId, store.findByName("realplayer").orElseThrow().playerId());
    }

    @Test
    void setReportsWhetherItChanged() {
        Plugin plugin = fakePlugin(dataFolder);
        NicknameStore store = new NicknameStore(plugin);
        UUID playerId = UUID.randomUUID();

        assertTrue(store.set(playerId, "RealPlayer", "KingBob"));
        assertFalse(store.set(playerId, "RealPlayer", "KingBob"));
        assertTrue(store.set(playerId, "RealPlayer", "Other"));
    }

    @Test
    void validateNicknameAcceptsShortAndRejectsInvalid() {
        assertEquals("a", DisguiseSettingParser.validateNickname("a"));
        assertEquals("King_Bob9", DisguiseSettingParser.validateNickname("King_Bob9"));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.validateNickname(""));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.validateNickname("has space"));
        assertThrows(IllegalArgumentException.class, () -> DisguiseSettingParser.validateNickname("waytoolongnickname"));
    }

    private static Plugin fakePlugin(File dataFolder) {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getDataFolder" -> dataFolder;
                case "getLogger" -> Logger.getLogger("NicknameStoreTest");
                case "getName" -> "NicknameStoreTest";
                case "isEnabled" -> true;
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}

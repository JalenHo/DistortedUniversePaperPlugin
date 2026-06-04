package dev.distorteduniverse.playerdisguise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DisguiseStoreTest {
    @TempDir
    File dataFolder;

    @Test
    void savesAndLoadsTextureData() {
        Plugin plugin = fakePlugin(dataFolder);
        DisguiseStore store = new DisguiseStore(plugin);
        UUID playerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        DisguiseStore.DisguiseEntry entry = new DisguiseStore.DisguiseEntry(
            playerId,
            "RealPlayer",
            "Notch",
            sourceId,
            "Notch",
            "texture-value",
            "texture-signature",
            123456789L
        );

        assertTrue(store.setDisguise(entry));
        store.save();

        DisguiseStore loadedStore = new DisguiseStore(plugin);
        loadedStore.load();

        DisguiseStore.DisguiseEntry loaded = loadedStore.entry(playerId).orElseThrow();
        assertEquals("RealPlayer", loaded.name());
        assertEquals("Notch", loaded.profileName());
        assertEquals(sourceId, loaded.sourceId());
        assertEquals("texture-value", loaded.textureValue());
        assertEquals("texture-signature", loaded.textureSignature());
        assertEquals(123456789L, loaded.cachedAtEpochMillis());
    }

    private static Plugin fakePlugin(File dataFolder) {
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] {Plugin.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getDataFolder" -> dataFolder;
                case "getLogger" -> Logger.getLogger("DisguiseStoreTest");
                case "getName" -> "DisguiseStoreTest";
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
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0F;
        }
        if (type == double.class) {
            return 0.0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}


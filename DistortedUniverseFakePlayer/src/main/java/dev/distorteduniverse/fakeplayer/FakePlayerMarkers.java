package dev.distorteduniverse.fakeplayer;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class FakePlayerMarkers {
    private static NamespacedKey markerKey;

    private FakePlayerMarkers() {
    }

    public static void init(Plugin plugin) {
        markerKey = new NamespacedKey(plugin, "bot");
    }

    public static boolean isFakePlayer(Player player) {
        return markerKey != null
            && player.getPersistentDataContainer().has(markerKey, PersistentDataType.BYTE);
    }

    public static void mark(Player player) {
        if (markerKey != null) {
            player.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    public static void unmark(Player player) {
        if (markerKey != null) {
            player.getPersistentDataContainer().remove(markerKey);
        }
    }
}

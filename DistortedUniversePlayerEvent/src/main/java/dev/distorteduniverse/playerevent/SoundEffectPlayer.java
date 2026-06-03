package dev.distorteduniverse.playerevent;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

final class SoundEffectPlayer {
    private SoundEffectPlayer() {
    }

    static void play(
        Player recipient,
        Location location,
        String soundKey,
        SoundCategory category,
        float volume,
        float pitch
    ) {
        Sound registeredSound = registeredMinecraftSound(soundKey);
        if (registeredSound != null) {
            recipient.playSound(location, registeredSound, category, volume, pitch);
            return;
        }

        recipient.playSound(location, soundKey, category, volume, pitch);
    }

    static boolean isMinecraftSoundKey(String soundKey) {
        NamespacedKey key = NamespacedKey.fromString(soundKey);
        return key != null && NamespacedKey.MINECRAFT.equals(key.getNamespace());
    }

    static boolean isKnownMinecraftSound(String soundKey) {
        return registeredMinecraftSound(soundKey) != null;
    }

    private static Sound registeredMinecraftSound(String soundKey) {
        NamespacedKey key = NamespacedKey.fromString(soundKey);
        if (key == null || !NamespacedKey.MINECRAFT.equals(key.getNamespace())) {
            return null;
        }
        return Registry.SOUNDS.get(key);
    }
}

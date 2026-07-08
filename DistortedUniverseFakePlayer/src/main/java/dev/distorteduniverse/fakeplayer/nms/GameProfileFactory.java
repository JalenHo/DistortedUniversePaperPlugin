package dev.distorteduniverse.fakeplayer.nms;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.distorteduniverse.fakeplayer.SkinProperty;
import java.util.UUID;

public final class GameProfileFactory {
    private static final String TEXTURES = "textures";

    private GameProfileFactory() {
    }

    public static GameProfile create(UUID uuid, String name, SkinProperty skin) {
        if (skin == null || skin.value().isBlank()) {
            return new GameProfile(uuid, name);
        }

        Multimap<String, Property> propertyEntries = ArrayListMultimap.create();
        String signature = skin.signature() == null || skin.signature().isBlank() ? null : skin.signature();
        propertyEntries.put(TEXTURES, new Property(TEXTURES, skin.value(), signature));
        return new GameProfile(uuid, name, new PropertyMap(propertyEntries));
    }
}

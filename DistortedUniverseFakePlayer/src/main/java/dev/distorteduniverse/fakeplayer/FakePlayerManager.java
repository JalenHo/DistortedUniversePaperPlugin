package dev.distorteduniverse.fakeplayer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class FakePlayerManager {
    private final ProtocolManager protocolManager;
    private final FakePlayerSkinLoader skinLoader;
    private final FakePlayerStore store;
    private final Set<UUID> spawnedEntities = new HashSet<>();
    private final Map<UUID, Integer> entityIds = new HashMap<>();
    private int nextEntityId = 1;

    public FakePlayerManager(ProtocolManager protocolManager, FakePlayerSkinLoader skinLoader, FakePlayerStore store) {
        this.protocolManager = protocolManager;
        this.skinLoader = skinLoader;
        this.store = store;
    }

    public boolean spawnFakePlayer(FakePlayer fakePlayer) {
        UUID uuid = fakePlayer.uuid();

        if (spawnedEntities.contains(uuid)) {
            return false;
        }

        int entityId = nextEntityId++;
        entityIds.put(uuid, entityId);

        Optional<WrappedSignedProperty> skinProperty = skinLoader.getSkin(fakePlayer.skin());
        if (skinProperty.isEmpty()) {
            skinProperty = skinLoader.getSkin("default");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            skinProperty.ifPresent(skin -> sendPlayerInfoPacket(player, fakePlayer, skin));
            sendSpawnEntityPacket(player, entityId, fakePlayer);
            sendEntityMetadataPacket(player, entityId, fakePlayer);
        }

        spawnedEntities.add(uuid);
        return true;
    }

    public boolean despawnFakePlayer(UUID uuid) {
        if (!spawnedEntities.contains(uuid)) {
            return false;
        }

        int entityId = entityIds.remove(uuid);

        for (Player player : Bukkit.getOnlinePlayers()) {
            sendPlayerInfoRemovePacket(player, uuid);
            sendRemoveEntityPacket(player, entityId);
        }

        spawnedEntities.remove(uuid);
        return true;
    }

    public void teleportFakePlayer(UUID uuid, Location newLocation) {
        if (!spawnedEntities.contains(uuid)) {
            return;
        }

        Integer entityId = entityIds.get(uuid);
        if (entityId == null) {
            return;
        }

        store.get(uuid.toString()).ifPresent(fp -> {
            FakePlayer updated = fp.withLocation(newLocation);
            store.update(uuid.toString(), updated);

            for (Player player : Bukkit.getOnlinePlayers()) {
                sendEntityTeleportPacket(player, entityId, newLocation);
            }
        });
    }

    public void updateSkin(UUID uuid, String skinName) {
        if (!spawnedEntities.contains(uuid)) {
            return;
        }

        store.get(uuid.toString()).ifPresent(fp -> {
            FakePlayer updated = fp.withSkin(skinName);
            store.update(uuid.toString(), updated);

            Optional<WrappedSignedProperty> skinProperty = skinLoader.getSkin(skinName);
            skinProperty.ifPresent(prop -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendPlayerInfoRemovePacket(player, uuid);
                    sendPlayerInfoPacket(player, updated, prop);
                }
            });
        });
    }

    public void onPlayerJoin(Player player) {
        for (FakePlayer fakePlayer : store.getAll()) {
            if (!spawnedEntities.contains(fakePlayer.uuid())) {
                continue;
            }

            int entityId = entityIds.get(fakePlayer.uuid());
            Optional<WrappedSignedProperty> skinProperty = skinLoader.getSkin(fakePlayer.skin());
            skinProperty.or(() -> skinLoader.getSkin("default")).ifPresent(prop -> {
                sendPlayerInfoPacket(player, fakePlayer, prop);
                sendSpawnEntityPacket(player, entityId, fakePlayer);
                sendEntityMetadataPacket(player, entityId, fakePlayer);
            });
        }
    }

    private void sendPlayerInfoPacket(Player receiver, FakePlayer fakePlayer, WrappedSignedProperty skin) {
        WrappedGameProfile profile = new WrappedGameProfile(fakePlayer.uuid(), fakePlayer.name());
        profile.getProperties().put("textures", skin);

        PlayerInfoData data = new PlayerInfoData(
            fakePlayer.uuid(),
            0,
            true,
            EnumWrappers.NativeGameMode.SURVIVAL,
            profile,
            WrappedChatComponent.fromText(fakePlayer.name())
        );

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, EnumSet.of(
            EnumWrappers.PlayerInfoAction.ADD_PLAYER,
            EnumWrappers.PlayerInfoAction.UPDATE_LISTED
        ));
        // Since 1.19.3, actions and entry lists share field indices in ProtocolLib; use index 1 for data.
        packet.getPlayerInfoDataLists().write(1, List.of(data));

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send player info packet", e);
        }
    }

    private void sendPlayerInfoRemovePacket(Player receiver, UUID uuid) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, List.of(uuid));

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send player info remove packet", e);
        }
    }

    private void sendSpawnEntityPacket(Player receiver, int entityId, FakePlayer fakePlayer) {
        Location location = fakePlayer.location();
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, fakePlayer.uuid());
        packet.getEntityTypeModifier().write(0, EntityType.PLAYER);
        packet.getDoubles()
            .write(0, location.getX())
            .write(1, location.getY())
            .write(2, location.getZ());

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send spawn entity packet", e);
        }
    }

    private void sendRemoveEntityPacket(Player receiver, int entityId) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntegers().write(0, entityId);

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send entity destroy packet", e);
        }
    }

    private void sendEntityTeleportPacket(Player receiver, int entityId, Location location) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        packet.getIntegers().write(0, entityId);
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        packet.getBytes().write(0, (byte) ((location.getYaw() * 256.0f) / 360.0f));
        packet.getBytes().write(1, (byte) ((location.getPitch() * 256.0f) / 360.0f));

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send teleport packet", e);
        }
    }

    private void sendEntityMetadataPacket(Player receiver, int entityId, FakePlayer fakePlayer) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entityId);

        try {
            protocolManager.sendServerPacket(receiver, packet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send entity metadata packet", e);
        }
    }

    public boolean isSpawned(UUID uuid) {
        return spawnedEntities.contains(uuid);
    }

    public Collection<UUID> getSpawnedUuids() {
        return Collections.unmodifiableCollection(spawnedEntities);
    }

    public int getEntityId(UUID uuid) {
        return entityIds.getOrDefault(uuid, -1);
    }
}

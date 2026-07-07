package dev.distorteduniverse.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FakePlayerManager {
    private final JavaPlugin plugin;
    private final FakePlayerSkinLoader skinLoader;
    private final FakePlayerStore store;
    private FakePlayerSettings.BehaviorSettings behavior;
    private final Map<UUID, UUID> fakeToEntity = new HashMap<>();
    private final Set<UUID> spawnedEntities = new HashSet<>();

    public FakePlayerManager(
        JavaPlugin plugin,
        FakePlayerSkinLoader skinLoader,
        FakePlayerStore store,
        FakePlayerSettings.BehaviorSettings behavior
    ) {
        this.plugin = plugin;
        this.skinLoader = skinLoader;
        this.store = store;
        this.behavior = behavior;
    }

    public boolean spawnFakePlayer(FakePlayer fakePlayer) {
        UUID uuid = fakePlayer.uuid();
        if (spawnedEntities.contains(uuid)) {
            return false;
        }

        Location location = fakePlayer.location();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        Mannequin mannequin = (Mannequin) world.spawnEntity(location, EntityType.MANNEQUIN);
        applyAppearance(mannequin, fakePlayer);
        configureBehavior(mannequin);

        fakeToEntity.put(uuid, mannequin.getUniqueId());
        spawnedEntities.add(uuid);
        return true;
    }

    public boolean despawnFakePlayer(UUID uuid) {
        spawnedEntities.remove(uuid);
        UUID entityUuid = fakeToEntity.remove(uuid);
        if (entityUuid == null) {
            return false;
        }

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity != null) {
            entity.remove();
        }
        return true;
    }

    public void teleportFakePlayer(UUID uuid, Location newLocation) {
        getEntity(uuid).ifPresent(entity -> {
            entity.teleport(newLocation);
            store.findKeyByUuid(uuid).ifPresent(key ->
                store.get(key).ifPresent(fp -> store.update(key, fp.withLocation(newLocation)))
            );
        });
    }

    public Optional<Mannequin> getMannequin(UUID fakePlayerUuid) {
        return getEntity(fakePlayerUuid)
            .filter(entity -> entity instanceof Mannequin)
            .map(entity -> (Mannequin) entity);
    }

    public Optional<Entity> getEntity(UUID fakePlayerUuid) {
        UUID entityUuid = fakeToEntity.get(fakePlayerUuid);
        if (entityUuid == null) {
            return Optional.empty();
        }

        Entity entity = Bukkit.getEntity(entityUuid);
        if (entity == null || !entity.isValid()) {
            fakeToEntity.remove(fakePlayerUuid);
            spawnedEntities.remove(fakePlayerUuid);
            return Optional.empty();
        }
        return Optional.of(entity);
    }

    private void applyAppearance(Mannequin mannequin, FakePlayer fakePlayer) {
        mannequin.customName(null);
        mannequin.setCustomNameVisible(false);
        mannequin.setDescription(null);

        Optional<SkinProperty> skinProperty = skinLoader.getSkin(fakePlayer.skin());
        if (skinProperty.isEmpty()) {
            skinProperty = skinLoader.getSkin("default");
        }

        PlayerProfile profile = skinLoader.createProfile(fakePlayer.uuid(), fakePlayer.name(), skinProperty.orElse(null));
        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
    }

    public void setMovementActive(UUID uuid, boolean active) {
        getMannequin(uuid).ifPresent(mannequin ->
            mannequin.setImmovable(active ? false : behavior.immovable())
        );
    }

    private void configureBehavior(Mannequin mannequin) {
        mannequin.setGravity(behavior.gravity());
        mannequin.setImmovable(behavior.immovable());
        mannequin.setInvulnerable(behavior.invulnerable());
    }

    public boolean isSpawned(UUID uuid) {
        return spawnedEntities.contains(uuid) && getEntity(uuid).isPresent();
    }

    public Collection<UUID> getSpawnedUuids() {
        return Collections.unmodifiableCollection(new HashSet<>(spawnedEntities));
    }

    public boolean isManagedEntity(UUID entityUuid) {
        return fakeToEntity.containsValue(entityUuid);
    }

    public Optional<UUID> getFakePlayerUuid(UUID entityUuid) {
        return fakeToEntity.entrySet().stream()
            .filter(entry -> entry.getValue().equals(entityUuid))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public void updateBehavior(FakePlayerSettings.BehaviorSettings newBehavior) {
        this.behavior = newBehavior;
        for (UUID fakeUuid : new ArrayList<>(spawnedEntities)) {
            getEntity(fakeUuid).ifPresent(entity -> {
                if (entity instanceof Mannequin mannequin) {
                    mannequin.setGravity(newBehavior.gravity());
                    mannequin.setImmovable(newBehavior.immovable());
                    mannequin.setInvulnerable(newBehavior.invulnerable());
                }
            });
        }
    }
}

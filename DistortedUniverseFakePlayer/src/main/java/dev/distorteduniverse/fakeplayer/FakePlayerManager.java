package dev.distorteduniverse.fakeplayer;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;

import java.util.*;

public class FakePlayerManager {
    private final FakePlayerSkinLoader skinLoader;
    private final FakePlayerStore store;
    private FakePlayerSettings.BehaviorSettings behavior;
    private final Map<UUID, Mannequin> activeMannequins = new HashMap<>();

    public FakePlayerManager(
        FakePlayerSkinLoader skinLoader,
        FakePlayerStore store,
        FakePlayerSettings.BehaviorSettings behavior
    ) {
        this.skinLoader = skinLoader;
        this.store = store;
        this.behavior = behavior;
    }

    public boolean spawnFakePlayer(FakePlayer fakePlayer) {
        UUID uuid = fakePlayer.uuid();
        if (activeMannequins.containsKey(uuid)) {
            return false;
        }

        Location location = snapSpawnLocation(fakePlayer.location());
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        world.getChunkAt(location).load(true);
        world.setChunkForceLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4, true);

        Mannequin mannequin = (Mannequin) world.spawnEntity(location, EntityType.MANNEQUIN);
        if (mannequin == null || mannequin.isDead()) {
            return false;
        }

        configureBehavior(mannequin);
        applyAppearance(mannequin, fakePlayer);
        activeMannequins.put(uuid, mannequin);
        return true;
    }

    public boolean despawnFakePlayer(UUID uuid) {
        Mannequin mannequin = activeMannequins.remove(uuid);
        if (mannequin == null) {
            return false;
        }

        World world = mannequin.getWorld();
        if (world != null) {
            Location location = mannequin.getLocation();
            world.setChunkForceLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4, false);
        }

        if (mannequin.isValid()) {
            mannequin.remove();
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
        return getEntity(fakePlayerUuid).map(entity -> (Mannequin) entity);
    }

    public Optional<Entity> getEntity(UUID fakePlayerUuid) {
        Mannequin mannequin = activeMannequins.get(fakePlayerUuid);
        if (mannequin == null || mannequin.isDead()) {
            if (mannequin != null) {
                activeMannequins.remove(fakePlayerUuid);
            }
            return Optional.empty();
        }
        return Optional.of(mannequin);
    }

    public Location snapSpawnLocation(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return location;
        }

        Location snapped = location.clone();
        int groundY = world.getHighestBlockYAt(snapped);
        snapped.setY(groundY + 1.0);
        return snapped;
    }

    private void applyAppearance(Mannequin mannequin, FakePlayer fakePlayer) {
        mannequin.customName(null);
        mannequin.setCustomNameVisible(false);
        mannequin.setDescription(null);

        Optional<SkinProperty> skinProperty = skinLoader.getSkin(fakePlayer.skin());
        if (skinProperty.isEmpty()) {
            skinProperty = skinLoader.getSkin("default");
        }

        PlayerProfile profile = skinLoader.createProfile(mannequin.getUniqueId(), fakePlayer.name(), skinProperty.orElse(null));
        mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
    }

    public void setMovementActive(UUID uuid, boolean active) {
        getMannequin(uuid).ifPresent(mannequin ->
            mannequin.setImmovable(active ? false : behavior.immovable())
        );
    }

    private void configureBehavior(Mannequin mannequin) {
        mannequin.setInvulnerable(behavior.invulnerable());
        mannequin.setGravity(behavior.gravity());
        mannequin.setImmovable(behavior.immovable());
    }

    public boolean isSpawned(UUID uuid) {
        return getEntity(uuid).isPresent();
    }

    public Collection<UUID> getSpawnedUuids() {
        return Collections.unmodifiableCollection(new HashSet<>(activeMannequins.keySet()));
    }

    public boolean isManagedEntity(UUID entityUuid) {
        return activeMannequins.values().stream()
            .anyMatch(mannequin -> mannequin.getUniqueId().equals(entityUuid));
    }

    public Optional<UUID> getFakePlayerUuid(UUID entityUuid) {
        return activeMannequins.entrySet().stream()
            .filter(entry -> entry.getValue().getUniqueId().equals(entityUuid))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public void updateBehavior(FakePlayerSettings.BehaviorSettings newBehavior) {
        this.behavior = newBehavior;
        for (UUID fakeUuid : new ArrayList<>(activeMannequins.keySet())) {
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

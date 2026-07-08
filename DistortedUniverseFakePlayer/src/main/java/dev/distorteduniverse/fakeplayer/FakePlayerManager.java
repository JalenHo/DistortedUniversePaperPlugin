package dev.distorteduniverse.fakeplayer;

import dev.distorteduniverse.fakeplayer.nms.NmsFakePlayerSpawner;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FakePlayerManager {
    private final JavaPlugin plugin;
    private final FakePlayerSkinLoader skinLoader;
    private final FakePlayerStore store;
    private final NmsFakePlayerSpawner spawner;
    private FakePlayerSettings.BehaviorSettings behavior;
    private FakePlayerSettings.DisplaySettings display;
    private double movementSpeed = 0.2D;
    private final Map<UUID, Player> activePlayers = new HashMap<>();

    public FakePlayerManager(
        JavaPlugin plugin,
        FakePlayerSkinLoader skinLoader,
        FakePlayerStore store,
        NmsFakePlayerSpawner spawner,
        FakePlayerSettings.BehaviorSettings behavior,
        FakePlayerSettings.DisplaySettings display
    ) {
        this.plugin = plugin;
        this.skinLoader = skinLoader;
        this.store = store;
        this.spawner = spawner;
        this.behavior = behavior;
        this.display = display;
    }

    public void updateMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public int cleanupOrphanedFakePlayers() {
        int removed = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!FakePlayerMarkers.isFakePlayer(player)) {
                continue;
            }

            boolean tracked = getTrackedPlayer(player.getUniqueId())
                .map(candidate -> candidate.getUniqueId().equals(player.getUniqueId()))
                .orElse(false);
            boolean stored = store.findKeyByUuid(player.getUniqueId()).isPresent();
            if (tracked || stored) {
                continue;
            }

            plugin.getLogger().warning("Removing orphaned fake player entity " + player.getName());
            FakePlayerMarkers.unmark(player);
            spawner.remove(player);
            removed++;
        }
        return removed;
    }

    public boolean spawnFakePlayer(FakePlayer fakePlayer) {
        UUID uuid = fakePlayer.uuid();
        if (activePlayers.containsKey(uuid)) {
            return false;
        }

        if (!spawner.isAvailable()) {
            plugin.getLogger().severe("NMS fake player spawner is unavailable on this server build.");
            return false;
        }

        Location location = snapSpawnLocation(fakePlayer.location());
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        world.getChunkAt(location).load(true);
        world.setChunkForceLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4, true);

        Optional<SkinProperty> skinProperty = skinLoader.getSkin(fakePlayer.skin());
        if (skinProperty.isEmpty()) {
            skinProperty = skinLoader.getSkin("default");
        }

        Player player = spawner.spawn(
            uuid,
            fakePlayer.name(),
            skinProperty.orElse(null),
            location
        );

        if (player == null || !player.isOnline()) {
            world.setChunkForceLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4, false);
            return false;
        }

        FakePlayerMarkers.mark(player);
        configureBehavior(player);
        applyAppearance(player, fakePlayer.name(), skinProperty);
        activePlayers.put(uuid, player);
        return true;
    }

    public boolean despawnFakePlayer(UUID uuid) {
        Player player = activePlayers.remove(uuid);
        if (player == null) {
            return false;
        }

        World world = player.getWorld();
        if (world != null) {
            Location location = player.getLocation();
            world.setChunkForceLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4, false);
        }

        FakePlayerMarkers.unmark(player);
        spawner.remove(player);
        return true;
    }

    public boolean forceDespawnFakePlayer(UUID uuid) {
        Player player = activePlayers.remove(uuid);
        if (player == null) {
            return false;
        }

        FakePlayerMarkers.unmark(player);
        spawner.remove(player);
        return true;
    }

    public void teleportFakePlayer(UUID uuid, Location newLocation) {
        getPlayer(uuid).ifPresent(player -> {
            player.teleport(newLocation);
            store.findKeyByUuid(uuid).ifPresent(key ->
                store.get(key).ifPresent(fp -> store.update(key, fp.withLocation(newLocation)))
            );
        });
    }

    public Optional<Player> getPlayer(UUID fakePlayerUuid) {
        Player player = activePlayers.get(fakePlayerUuid);
        if (player == null || !player.isOnline() || player.isDead()) {
            if (player != null) {
                activePlayers.remove(fakePlayerUuid);
            }
            return Optional.empty();
        }
        return Optional.of(player);
    }

    public Optional<Player> getTrackedPlayer(UUID fakePlayerUuid) {
        return Optional.ofNullable(activePlayers.get(fakePlayerUuid));
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

    private void applyAppearance(Player player, String name, Optional<SkinProperty> skinProperty) {
        player.customName(null);
        player.setCustomNameVisible(false);
        player.displayName(net.kyori.adventure.text.Component.text(name));
        player.playerListName(net.kyori.adventure.text.Component.text(name));

        com.destroystokyo.paper.profile.PlayerProfile profile = skinLoader.createProfile(
            player.getUniqueId(),
            name,
            skinProperty.orElse(null)
        );
        player.setPlayerProfile(profile);
    }

    public void setMovementActive(UUID uuid, boolean active) {
        getPlayer(uuid).ifPresent(player -> {
            if (active) {
                player.setWalkSpeed((float) Math.max(0.01F, movementSpeed));
            } else if (behavior.immovable()) {
                player.setWalkSpeed(0.0F);
            } else {
                player.setWalkSpeed(0.2F);
            }
        });
    }

    private void configureBehavior(Player player) {
        player.setInvulnerable(behavior.invulnerable());
        player.setGravity(behavior.gravity());
        if (behavior.immovable()) {
            player.setWalkSpeed(0.0F);
        }
    }

    public boolean isSpawned(UUID uuid) {
        return getPlayer(uuid).isPresent();
    }

    public Collection<UUID> getSpawnedUuids() {
        return Collections.unmodifiableCollection(new HashSet<>(activePlayers.keySet()));
    }

    public boolean isManagedEntity(UUID entityUuid) {
        Player player = activePlayers.get(entityUuid);
        if (player != null && player.getUniqueId().equals(entityUuid)) {
            return true;
        }
        return activePlayers.values().stream()
            .anyMatch(candidate -> candidate.getUniqueId().equals(entityUuid));
    }

    public Optional<UUID> getFakePlayerUuid(UUID entityUuid) {
        if (activePlayers.containsKey(entityUuid)) {
            return Optional.of(entityUuid);
        }
        return activePlayers.entrySet().stream()
            .filter(entry -> entry.getValue().getUniqueId().equals(entityUuid))
            .map(Map.Entry::getKey)
            .findFirst();
    }

    public void updateBehavior(FakePlayerSettings.BehaviorSettings newBehavior) {
        this.behavior = newBehavior;
        for (UUID fakeUuid : new ArrayList<>(activePlayers.keySet())) {
            getPlayer(fakeUuid).ifPresent(this::configureBehavior);
        }
    }

    public void updateDisplay(FakePlayerSettings.DisplaySettings newDisplay) {
        this.display = newDisplay;
    }
}

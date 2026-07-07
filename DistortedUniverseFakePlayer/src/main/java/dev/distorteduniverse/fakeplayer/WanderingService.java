package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class WanderingService {
    private final JavaPlugin plugin;
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private FakePlayerSettings.WanderingSettings settings;
    private final Map<UUID, WanderingTask> activeTasks = new HashMap<>();
    private final Random random = new Random();

    public WanderingService(
        JavaPlugin plugin,
        FakePlayerManager manager,
        FakePlayerStore store,
        FakePlayerSettings.WanderingSettings settings
    ) {
        this.plugin = plugin;
        this.manager = manager;
        this.store = store;
        this.settings = settings;
    }

    public void startWandering(String key, FakePlayer fakePlayer) {
        if (activeTasks.containsKey(fakePlayer.uuid())) {
            return;
        }

        FakePlayer updated = fakePlayer.withWandering(true);
        store.update(key, updated);

        WanderingTask task = new WanderingTask(key, updated.uuid(), this);
        activeTasks.put(updated.uuid(), task);
        task.runTaskTimer(plugin, 0L, settings.tickInterval());
    }

    public void startWanderingAll(double radius) {
        for (String key : store.getKeys()) {
            store.get(key).ifPresent(fp -> {
                if (!manager.isSpawned(fp.uuid())) {
                    return;
                }
                startWandering(key, fp);
            });
        }
    }

    public void stopWandering(UUID uuid) {
        WanderingTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        store.findKeyByUuid(uuid).ifPresent(key ->
            store.get(key).ifPresent(fp -> store.update(key, fp.withWandering(false)))
        );
    }

    public void stopAllWandering() {
        for (WanderingTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();

        for (String key : store.getKeys()) {
            store.get(key).ifPresent(fp -> store.update(key, fp.withWandering(false)));
        }
    }

    public void onTick(String storeKey) {
        store.get(storeKey).ifPresent(fp -> {
            Optional<Entity> entity = manager.getEntity(fp.uuid());
            if (entity.isEmpty()) {
                return;
            }

            Location current = entity.get().getLocation();
            double radius = settings.defaultRadius();
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;

            Location newLocation = current.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            newLocation.setYaw(random.nextFloat() * 360);
            newLocation.setPitch((random.nextFloat() - 0.5f) * 60);

            if (settings.usePathfinding()) {
                newLocation = findValidLocation(newLocation);
            }

            manager.teleportFakePlayer(fp.uuid(), newLocation);
        });
    }

    public void updateSettings(FakePlayerSettings.WanderingSettings newSettings) {
        if (!activeTasks.isEmpty()) {
            stopAllWandering();
        }
        this.settings = newSettings;
    }

    private Location findValidLocation(Location target) {
        Location check = target.clone();
        check.setY(target.getWorld().getHighestBlockYAt(target) + 1);

        if (check.getY() < target.getWorld().getMinHeight()) {
            check.setY(target.getWorld().getMinHeight() + 1);
        }

        return check;
    }

    public boolean isWandering(UUID uuid) {
        return activeTasks.containsKey(uuid);
    }

    private static class WanderingTask extends BukkitRunnable {
        private final String key;
        private final UUID fakePlayerUuid;
        private final WanderingService service;

        WanderingTask(String key, UUID fakePlayerUuid, WanderingService service) {
            this.key = key;
            this.fakePlayerUuid = fakePlayerUuid;
            this.service = service;
        }

        @Override
        public void run() {
            service.onTick(key);
        }
    }
}

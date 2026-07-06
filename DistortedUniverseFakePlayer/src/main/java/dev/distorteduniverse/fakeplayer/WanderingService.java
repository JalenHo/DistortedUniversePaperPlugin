package dev.distorteduniverse.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class WanderingService {
    private final FakePlayerManager manager;
    private final FakePlayerStore store;
    private final FakePlayerSettings.WanderingSettings settings;
    private final Map<UUID, WanderingTask> activeTasks = new HashMap<>();
    private final Random random = new Random();

    public WanderingService(FakePlayerManager manager, FakePlayerStore store, FakePlayerSettings.WanderingSettings settings) {
        this.manager = manager;
        this.store = store;
        this.settings = settings;
    }

    public void startWandering(String key, FakePlayer fakePlayer) {
        if (activeTasks.containsKey(fakePlayer.uuid())) {
            return;
        }

        WanderingTask task = new WanderingTask(key, fakePlayer, this);
        activeTasks.put(fakePlayer.uuid(), task);
        task.runTaskTimerAsynchronously(Bukkit.getPluginManager().getPlugin("DistortedUniverseFakePlayer"),
            0L, settings.tickInterval());
    }

    public void startWanderingAll(double radius) {
        for (FakePlayer fp : store.getAll()) {
            if (!manager.isSpawned(fp.uuid())) {
                continue;
            }
            FakePlayer updated = fp.withWandering(true);
            store.update(fp.name().toLowerCase(), updated);
            startWandering(fp.name().toLowerCase(), updated);
        }
    }

    public void stopWandering(UUID uuid) {
        WanderingTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        store.get(uuid.toString()).ifPresent(fp -> {
            FakePlayer updated = fp.withWandering(false);
            store.update(fp.name().toLowerCase(), updated);
        });
    }

    public void stopAllWandering() {
        for (WanderingTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();

        for (String key : store.getKeys()) {
            store.get(key).ifPresent(fp -> {
                FakePlayer updated = fp.withWandering(false);
                store.update(key, updated);
            });
        }
    }

    public void onTick(UUID uuid) {
        store.get(uuid.toString()).ifPresent(fp -> {
            Location current = fp.location();
            double radius = settings.defaultRadius();

            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;

            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            Location newLocation = current.clone();
            newLocation.add(offsetX, 0, offsetZ);

            newLocation.setYaw(random.nextFloat() * 360);
            newLocation.setPitch((random.nextFloat() - 0.5f) * 60);

            if (settings.usePathfinding()) {
                Location validLocation = findValidLocation(newLocation);
                if (validLocation != null) {
                    newLocation = validLocation;
                }
            }

            manager.teleportFakePlayer(uuid, newLocation);
        });
    }

    public void updateSettings(FakePlayerSettings.WanderingSettings newSettings) {
        if (!activeTasks.isEmpty()) {
            stopAllWandering();
        }
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
        private final FakePlayer fakePlayer;
        private final WanderingService service;

        WanderingTask(String key, FakePlayer fakePlayer, WanderingService service) {
            this.key = key;
            this.fakePlayer = fakePlayer;
            this.service = service;
        }

        @Override
        public void run() {
            service.onTick(fakePlayer.uuid());
        }
    }
}

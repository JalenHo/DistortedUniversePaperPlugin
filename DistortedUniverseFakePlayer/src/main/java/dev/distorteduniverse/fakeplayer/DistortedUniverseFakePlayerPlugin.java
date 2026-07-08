package dev.distorteduniverse.fakeplayer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Optional;

public class DistortedUniverseFakePlayerPlugin extends JavaPlugin {
    private FakePlayerSettingsService settingsService;
    private FakePlayerSkinLoader skinLoader;
    private FakePlayerStore store;
    private FakePlayerManager manager;
    private BotMovementService movementService;
    private FakePlayerListener listener;

    @Override
    public void onEnable() {
        settingsService = new FakePlayerSettingsService(this);
        settingsService.load();

        store = new FakePlayerStore(getDataFolder());
        store.load();

        skinLoader = new FakePlayerSkinLoader(this);
        skinLoader.load(settingsService.settings().skins());

        manager = new FakePlayerManager(
            skinLoader,
            store,
            settingsService.settings().behavior()
        );

        movementService = new BotMovementService(
            this,
            manager,
            store,
            settingsService.settings().movement()
        );

        listener = new FakePlayerListener(this, manager, movementService, settingsService);
        getServer().getPluginManager().registerEvents(listener, this);

        FakePlayerCommand command = new FakePlayerCommand(this);
        getCommand("dfp").setExecutor(command);
        getCommand("dfp").setTabCompleter(command);

        applyRuntimeSettings();
        movementService.start();

        getLogger().info("DistortedUniverseFakePlayer enabled!");
    }

    @Override
    public void onDisable() {
        if (movementService != null) {
            movementService.shutdown();
        }

        if (store != null && manager != null) {
            for (var uuid : new ArrayList<>(manager.getSpawnedUuids())) {
                manager.despawnFakePlayer(uuid);
            }
            store.save();
        }

        getLogger().info("DistortedUniverseFakePlayer disabled!");
    }

    public void applyRuntimeSettings() {
        FakePlayerSettings settings = settingsService.settings();
        skinLoader.load(settings.skins());
        movementService.updateSettings(settings.movement());
        manager.updateBehavior(settings.behavior());

        if (!settings.enabled()) {
            movementService.stopAll(false);
            for (var uuid : new ArrayList<>(manager.getSpawnedUuids())) {
                manager.despawnFakePlayer(uuid);
            }
            getLogger().warning("Plugin is disabled in config!");
            return;
        }

        int spawned = 0;
        for (String key : store.getKeys()) {
            Optional<FakePlayer> opt = store.get(key);
            if (opt.isEmpty()) {
                continue;
            }
            FakePlayer fp = opt.get();
            if (!manager.isSpawned(fp.uuid()) && manager.spawnFakePlayer(fp)) {
                spawned++;
            }
            manager.refreshAppearance(fp);
            if (fp.isWandering() && manager.isSpawned(fp.uuid()) && !movementService.isMoving(fp.uuid())) {
                movementService.startWandering(key, fp, settings.movement().wanderRadius());
            }
        }
        getLogger().info("Synced fake players; spawned " + spawned + " missing entities");
    }

    public FakePlayerSettingsService getSettingsService() {
        return settingsService;
    }

    public FakePlayerSkinLoader getSkinLoader() {
        return skinLoader;
    }

    public FakePlayerStore getStore() {
        return store;
    }

    public FakePlayerManager getManager() {
        return manager;
    }

    public BotMovementService getMovementService() {
        return movementService;
    }
}

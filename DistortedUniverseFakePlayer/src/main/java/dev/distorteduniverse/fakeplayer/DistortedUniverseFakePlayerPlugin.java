package dev.distorteduniverse.fakeplayer;

import dev.distorteduniverse.fakeplayer.nms.NmsFakePlayerSpawner;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Optional;

public class DistortedUniverseFakePlayerPlugin extends JavaPlugin {
    private FakePlayerSettingsService settingsService;
    private FakePlayerSkinLoader skinLoader;
    private FakePlayerStore store;
    private NmsFakePlayerSpawner nmsSpawner;
    private FakePlayerManager manager;
    private BotMovementService movementService;
    private FakePlayerBroadcastService broadcastService;
    private FakePlayerLifecycleService lifecycleService;

    @Override
    public void onEnable() {
        FakePlayerMarkers.init(this);

        settingsService = new FakePlayerSettingsService(this);
        settingsService.load();

        store = new FakePlayerStore(getDataFolder());
        store.load();

        skinLoader = new FakePlayerSkinLoader(this);
        skinLoader.load(settingsService.settings().skins());

        nmsSpawner = new NmsFakePlayerSpawner(this);
        if (!nmsSpawner.isAvailable()) {
            getLogger().severe("NMS fake player spawner failed to initialize. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        FakePlayerSettings settings = settingsService.settings();
        manager = new FakePlayerManager(
            this,
            skinLoader,
            store,
            nmsSpawner,
            settings.behavior(),
            settings.display()
        );
        manager.updateMovementSpeed(settings.movement().speed());

        movementService = new BotMovementService(
            this,
            manager,
            store,
            settings.movement()
        );

        broadcastService = new FakePlayerBroadcastService(this, settingsService);
        lifecycleService = new FakePlayerLifecycleService(store, manager, movementService, broadcastService);

        getServer().getPluginManager().registerEvents(
            new FakePlayerListener(this, manager, store, settingsService, lifecycleService, movementService),
            this
        );
        getServer().getPluginManager().registerEvents(new FakePlayerJoinGuard(), this);

        FakePlayerCommand command = new FakePlayerCommand(this);
        getCommand("dfp").setExecutor(command);
        getCommand("dfp").setTabCompleter(command);

        int orphaned = manager.cleanupOrphanedFakePlayers();
        if (orphaned > 0) {
            getLogger().warning("Cleaned up " + orphaned + " orphaned fake player entities during startup.");
        }

        respawnAllFakePlayers();

        getLogger().info("DistortedUniverseFakePlayer enabled (NMS fake players).");
    }

    @Override
    public void onDisable() {
        if (store != null && manager != null) {
            for (var uuid : new ArrayList<>(manager.getSpawnedUuids())) {
                manager.despawnFakePlayer(uuid);
            }
            store.save();
        }

        if (movementService != null) {
            movementService.shutdown();
        }

        getLogger().info("DistortedUniverseFakePlayer disabled!");
    }

    private void respawnAllFakePlayers() {
        FakePlayerSettings settings = settingsService.settings();
        if (!settings.enabled()) {
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
            if (manager.spawnFakePlayer(fp)) {
                if (fp.isWandering()) {
                    movementService.startWandering(key, fp, settings.movement().wanderRadius());
                }
                spawned++;
            }
        }
        getLogger().info("Respawned " + spawned + " fake players");
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

    public FakePlayerLifecycleService getLifecycleService() {
        return lifecycleService;
    }
}

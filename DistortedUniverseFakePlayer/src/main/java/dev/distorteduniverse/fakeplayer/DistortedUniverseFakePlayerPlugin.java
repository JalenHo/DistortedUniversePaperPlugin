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

        listener = new FakePlayerListener(manager, settingsService);
        getServer().getPluginManager().registerEvents(listener, this);

        FakePlayerCommand command = new FakePlayerCommand(this);
        getCommand("dfp").setExecutor(command);
        getCommand("dfp").setTabCompleter(command);

        respawnAllFakePlayers();

        getLogger().info("DistortedUniverseFakePlayer enabled!");
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
            movementService.stopAll();
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
}

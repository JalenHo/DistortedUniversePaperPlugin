package dev.distorteduniverse.fakeplayer;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DistortedUniverseFakePlayerPlugin extends JavaPlugin {
    private FakePlayerSettingsService settingsService;
    private FakePlayerSkinLoader skinLoader;
    private FakePlayerStore store;
    private FakePlayerManager manager;
    private WanderingService wanderingService;
    private FakePlayerListener listener;
    private final Set<UUID> protectedEntities = new HashSet<>();

    @Override
    public void onEnable() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        settingsService = new FakePlayerSettingsService(this);
        settingsService.load();

        store = new FakePlayerStore(getDataFolder());
        store.load();

        skinLoader = new FakePlayerSkinLoader(this);
        skinLoader.load(settingsService.settings().skins());

        manager = new FakePlayerManager(protocolManager, skinLoader, store);

        wanderingService = new WanderingService(manager, store, settingsService.settings().wandering());

        listener = new FakePlayerListener(manager, store, settingsService.settings(), protectedEntities);
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
            for (UUID uuid : new ArrayList<>(manager.getSpawnedUuids())) {
                manager.despawnFakePlayer(uuid);
            }
            store.save();
        }

        if (wanderingService != null) {
            wanderingService.stopAllWandering();
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
        for (FakePlayer fp : store.getAll()) {
            if (manager.spawnFakePlayer(fp)) {
                protectedEntities.add(fp.uuid());
                if (fp.isWandering()) {
                    wanderingService.startWandering(fp.name().toLowerCase(), fp);
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

    public WanderingService getWanderingService() {
        return wanderingService;
    }

    public Set<UUID> getProtectedEntities() {
        return protectedEntities;
    }
}

package dev.distorteduniverse.playerdisguise;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public final class DisguiseListener implements Listener {
    private final Plugin plugin;
    private final DisguiseStore disguiseStore;
    private final DisguiseDisplayService displayService;

    public DisguiseListener(Plugin plugin, DisguiseStore disguiseStore, DisguiseDisplayService displayService) {
        this.plugin = plugin;
        this.disguiseStore = disguiseStore;
        this.displayService = displayService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (disguiseStore.updateKnownName(player.getUniqueId(), player.getName())) {
            disguiseStore.save();
        }
        Bukkit.getScheduler().runTask(plugin, () -> displayService.apply(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.getPlayer().removeMetadata(DisguiseDisplayService.DISGUISE_METADATA_KEY, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> displayService.apply(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> displayService.apply(event.getPlayer()));
    }
}


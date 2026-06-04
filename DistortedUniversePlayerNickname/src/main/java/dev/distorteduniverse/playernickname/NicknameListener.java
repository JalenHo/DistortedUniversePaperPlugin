package dev.distorteduniverse.playernickname;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

public final class NicknameListener implements Listener {
    private final Plugin plugin;
    private final NicknameStore nicknameStore;
    private final NicknameDisplayService displayService;

    public NicknameListener(Plugin plugin, NicknameStore nicknameStore, NicknameDisplayService displayService) {
        this.plugin = plugin;
        this.nicknameStore = nicknameStore;
        this.displayService = displayService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (nicknameStore.updateKnownName(player.getUniqueId(), player.getName())) {
            nicknameStore.save();
        }
        Bukkit.getScheduler().runTask(plugin, () -> displayService.apply(player));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        displayService.clear(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        displayService.removeLabel(event.getPlayer());
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


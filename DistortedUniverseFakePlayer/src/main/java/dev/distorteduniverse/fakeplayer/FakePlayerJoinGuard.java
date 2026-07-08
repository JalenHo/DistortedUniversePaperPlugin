package dev.distorteduniverse.fakeplayer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FakePlayerJoinGuard implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (FakePlayerMarkers.isFakePlayer(event.getPlayer())) {
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (FakePlayerMarkers.isFakePlayer(event.getPlayer())) {
            event.quitMessage(null);
        }
    }
}

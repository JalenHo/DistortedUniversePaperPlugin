package dev.distorteduniverse.playerevent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class PlayerEventListener implements Listener {
    private final Plugin plugin;
    private final SettingsService settingsService;
    private final MessageFormatter messageFormatter;
    private final RecipientSelector recipientSelector;
    private final Map<UUID, DeathKickContext> pendingDeathKicks = new HashMap<>();

    public PlayerEventListener(
        Plugin plugin,
        SettingsService settingsService,
        MessageFormatter messageFormatter,
        RecipientSelector recipientSelector
    ) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.messageFormatter = messageFormatter;
        this.recipientSelector = recipientSelector;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PluginSettings settings = settingsService.settings();
        Player player = event.getPlayer();
        Location deathLocation = player.getLocation().clone();
        Component vanillaDeathMessage = event.deathMessage();
        boolean shouldShowDeathMessage = !settings.deathMessage().respectGamerule() || event.getShowDeathMessages();

        if (settings.deathMessage().enabled()) {
            event.deathMessage(null);
            event.setShowDeathMessages(false);
            if (shouldShowDeathMessage) {
                Component message = messageFormatter.deathMessage(
                    settings.deathMessage().template(),
                    player,
                    deathLocation,
                    vanillaDeathMessage == null ? defaultDeathMessage(player) : vanillaDeathMessage
                );
                sendNearby(message, deathLocation, settings.deathMessage().radius(), player, true);
            }
        }

        if (settings.deathSound().enabled()) {
            if (settings.deathSound().suppressVanilla()) {
                event.setShouldPlayDeathSound(false);
            }
            playNearbyDeathSound(settings.deathSound(), deathLocation, player);
        }

        if (settings.deathKick().enabled()) {
            scheduleDeathKick(player, deathLocation, settings.deathKick());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKick(PlayerKickEvent event) {
        if (pendingDeathKicks.containsKey(event.getPlayer().getUniqueId())) {
            event.leaveMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PluginSettings settings = settingsService.settings();
        if (!settings.joinMessage().enabled()) {
            return;
        }

        Player player = event.getPlayer();
        Location joinLocation = player.getLocation().clone();
        Component vanillaJoinMessage = event.joinMessage();
        event.joinMessage(null);
        Component message = messageFormatter.joinMessage(
            settings.joinMessage().template(),
            player,
            joinLocation,
            vanillaJoinMessage == null ? defaultJoinMessage(player) : vanillaJoinMessage
        );
        sendNearby(message, joinLocation, settings.joinMessage().radius(), player, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PluginSettings settings = settingsService.settings();
        Player player = event.getPlayer();
        DeathKickContext deathKickContext = pendingDeathKicks.remove(player.getUniqueId());

        if (deathKickContext != null) {
            event.quitMessage(null);
            if (settings.deathKick().showLeaveMessage()) {
                Component message = messageFormatter.quitMessage(
                    settings.deathKick().leaveTemplate(),
                    player,
                    deathKickContext.location(),
                    defaultQuitMessage(player)
                );
                sendNearby(message, deathKickContext.location(), settings.deathKick().leaveRadius(), player, false);
            }
            return;
        }

        if (!settings.leaveMessage().enabled()) {
            return;
        }

        Component vanillaQuitMessage = event.quitMessage();
        Location quitLocation = player.getLocation().clone();
        event.quitMessage(null);
        Component message = messageFormatter.quitMessage(
            settings.leaveMessage().template(),
            player,
            quitLocation,
            vanillaQuitMessage == null ? defaultQuitMessage(player) : vanillaQuitMessage
        );
        sendNearby(message, quitLocation, settings.leaveMessage().radius(), player, false);
    }

    public void clearPendingDeathKicks() {
        pendingDeathKicks.clear();
    }

    private void sendNearby(Component message, Location origin, double radius, Player subject, boolean includeSubject) {
        if (messageFormatter.isVisiblyEmpty(message)) {
            return;
        }
        for (Player recipient : recipientSelector.nearbyPlayers(origin, radius, subject, includeSubject)) {
            recipient.sendMessage(message);
        }
    }

    private void playNearbyDeathSound(
        PluginSettings.DeathSoundSettings deathSoundSettings,
        Location deathLocation,
        Player player
    ) {
        for (Player recipient : recipientSelector.nearbyPlayers(deathLocation, deathSoundSettings.radius(), player, true)) {
            SoundEffectPlayer.play(
                recipient,
                deathLocation,
                deathSoundSettings.sound(),
                deathSoundSettings.category(),
                deathSoundSettings.volume(),
                deathSoundSettings.pitch()
            );
        }
    }

    private void scheduleDeathKick(Player player, Location deathLocation, PluginSettings.DeathKickSettings deathKickSettings) {
        UUID playerId = player.getUniqueId();
        if (pendingDeathKicks.containsKey(playerId)) {
            return;
        }

        pendingDeathKicks.put(playerId, new DeathKickContext(deathLocation.clone()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!pendingDeathKicks.containsKey(playerId)) {
                return;
            }

            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                pendingDeathKicks.remove(playerId);
                return;
            }

            Component kickMessage = messageFormatter.playerMessage(
                settingsService.settings().deathKick().kickMessage(),
                onlinePlayer,
                deathLocation
            );
            onlinePlayer.kick(kickMessage);
            Bukkit.getScheduler().runTaskLater(plugin, () -> pendingDeathKicks.remove(playerId), 200L);
        }, deathKickSettings.delayTicks());
    }

    private static Component defaultDeathMessage(Player player) {
        return Component.text(player.getName() + " died");
    }

    private static Component defaultQuitMessage(Player player) {
        return Component.text(player.getName() + " left the game", NamedTextColor.YELLOW);
    }

    private static Component defaultJoinMessage(Player player) {
        return Component.text(player.getName() + " joined the game", NamedTextColor.YELLOW);
    }

    private record DeathKickContext(Location location) {
    }
}

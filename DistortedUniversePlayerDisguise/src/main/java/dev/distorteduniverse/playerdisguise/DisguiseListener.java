package dev.distorteduniverse.playerdisguise;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
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

public final class DisguiseListener implements Listener {
    private final Plugin plugin;
    private final DisguiseStore disguiseStore;
    private final NicknameStore nicknameStore;
    private final DisguiseDisplayService displayService;

    public DisguiseListener(Plugin plugin, DisguiseStore disguiseStore, NicknameStore nicknameStore, DisguiseDisplayService displayService) {
        this.plugin = plugin;
        this.disguiseStore = disguiseStore;
        this.nicknameStore = nicknameStore;
        this.displayService = displayService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean changed = disguiseStore.updateKnownName(player.getUniqueId(), player.getName());
        if (changed) {
            disguiseStore.save();
        }
        if (nicknameStore.updateKnownName(player.getUniqueId(), player.getName())) {
            nicknameStore.save();
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

    // Rewrites the death message ("kill feed") so disguised players show their disguise name
    // instead of their real one. Runs at HIGH, before DistortedUniversePlayerEvent's HIGHEST
    // handler captures and rebroadcasts the message, so the rewritten names flow through.
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component message = event.deathMessage();
        if (message == null) {
            return;
        }

        Player victim = event.getEntity();
        Component rewritten = applyDisguiseName(message, victim);

        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            rewritten = applyDisguiseName(rewritten, killer);
        }

        if (rewritten != message) {
            event.deathMessage(rewritten);
        }
    }

    private Component applyDisguiseName(Component message, Player player) {
        // hasMetadata mirrors an actively applied nickname/disguise (set only while the plugin is
        // enabled and the player has one), so a stored-but-inactive entry won't rename the feed.
        if (!player.hasMetadata(DisguiseDisplayService.DISGUISE_METADATA_KEY)) {
            return message;
        }
        // Nickname wins for the shown name; otherwise use the disguise's profile name.
        String shownName = nicknameStore.nickname(player.getUniqueId())
            .orElseGet(() -> disguiseStore.entry(player.getUniqueId())
                .map(DisguiseStore.DisguiseEntry::profileName)
                .orElse(null));
        if (shownName == null) {
            return message;
        }
        String realName = player.getName();
        if (realName.equals(shownName)) {
            return message;
        }
        return message.replaceText(builder -> builder
            .match(Pattern.compile("\\b" + Pattern.quote(realName) + "\\b"))
            .replacement(shownName));
    }
}


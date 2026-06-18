package dev.distorteduniverse.playerdisguise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

public final class DisguiseDisplayService {
    public static final String DISGUISE_METADATA_KEY = "distorteduniverse.playerdisguise.active";

    private final Plugin plugin;
    private final DisguiseSettingsService settingsService;
    private final DisguiseStore disguiseStore;
    private final NicknameStore nicknameStore;
    private final DisguiseProtocolService protocolService;

    public DisguiseDisplayService(
        Plugin plugin,
        DisguiseSettingsService settingsService,
        DisguiseStore disguiseStore,
        NicknameStore nicknameStore,
        DisguiseProtocolService protocolService
    ) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.disguiseStore = disguiseStore;
        this.nicknameStore = nicknameStore;
        this.protocolService = protocolService;
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
        protocolService.refreshAllTracked();
    }

    public void apply(Player player) {
        DisguiseSettings settings = settingsService.settings();
        DisguiseStore.DisguiseEntry entry = disguiseStore.entry(player.getUniqueId()).orElse(null);
        String nickname = nicknameStore.nickname(player.getUniqueId()).orElse(null);
        if (!settings.enabled() || (entry == null && nickname == null)) {
            clear(player);
            return;
        }

        // Nickname wins for the shown name; otherwise fall back to the disguise's profile name.
        String shownName = nickname != null ? nickname : entry.profileName();
        player.setMetadata(DISGUISE_METADATA_KEY, new FixedMetadataValue(plugin, shownName));
        Component displayName = Component.text(shownName);
        if (settings.apply().chatDisplayName()) {
            player.displayName(displayName);
        }
        if (settings.apply().tabListName()) {
            player.playerListName(displayName);
        }
        protocolService.refreshTracked(player);
    }

    public void clear(Player player) {
        player.removeMetadata(DISGUISE_METADATA_KEY, plugin);
        player.displayName(Component.text(player.getName()));
        player.playerListName(Component.text(player.getName()));
        protocolService.refreshTracked(player);
    }

    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removeMetadata(DISGUISE_METADATA_KEY, plugin);
            player.displayName(Component.text(player.getName()));
            player.playerListName(Component.text(player.getName()));
        }
        protocolService.refreshAllTracked();
    }

    public List<Player> viewersFor(Player subject) {
        List<Player> viewers = new ArrayList<>();
        boolean selfSeesDisguise = settingsService.settings().visibility().selfSeesDisguise();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!selfSeesDisguise && viewer.getUniqueId().equals(subject.getUniqueId())) {
                continue;
            }
            if (viewer.canSee(subject)) {
                viewers.add(viewer);
            }
        }
        return viewers;
    }

    public boolean shouldViewerSeeDisguise(Player viewer, UUID subjectId) {
        if (!settingsService.settings().visibility().selfSeesDisguise() && viewer.getUniqueId().equals(subjectId)) {
            return false;
        }
        Player subject = Bukkit.getPlayer(subjectId);
        return subject == null || viewer.canSee(subject);
    }
}

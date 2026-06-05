package dev.distorteduniverse.playerdisguise;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class DisguiseProtocolService {
    private final Plugin plugin;
    private final DisguiseSettingsService settingsService;
    private final DisguiseStore disguiseStore;
    private final NicknameStore nicknameStore;
    private ProtocolManager protocolManager;

    public DisguiseProtocolService(Plugin plugin, DisguiseSettingsService settingsService, DisguiseStore disguiseStore, NicknameStore nicknameStore) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.disguiseStore = disguiseStore;
        this.nicknameStore = nicknameStore;
    }

    public void start() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(
            plugin,
            ListenerPriority.HIGH,
            PacketType.Play.Server.PLAYER_INFO,
            PacketType.Play.Server.PLAYER_INFO_REMOVE
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                rewritePlayerInfo(event);
            }
        });
    }

    public void stop() {
        if (protocolManager != null) {
            protocolManager.removePacketListeners(plugin);
        }
    }

    public void refreshTracked(Player subject) {
        // Bukkit flips the plugin to disabled before onDisable() runs, so the shutdown-time
        // clearAll() must not schedule the hide/show respawn task (the scheduler rejects it).
        if (protocolManager == null || !plugin.isEnabled() || !subject.isOnline()) {
            return;
        }

        List<Player> viewers = trackedViewers(subject);
        if (viewers.isEmpty()) {
            return;
        }

        try {
            protocolManager.updateEntity(subject, viewers);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not refresh disguised player entity.", exception);
        }

        List<Player> entityReloadViewers = viewers.stream()
            .filter(viewer -> !viewer.getUniqueId().equals(subject.getUniqueId()))
            .toList();
        if (entityReloadViewers.isEmpty()) {
            return;
        }

        // updateEntity above only resends entity metadata; the client keeps the game profile
        // (skin + name above head) it cached when the player entity first spawned. To actually
        // change the rendered skin and overhead name we must respawn the entity for each viewer:
        // hidePlayer drops the player-info entry and despawns the entity, then showPlayer re-sends
        // the ADD_PLAYER packet (which our PLAYER_INFO listener rewrites with the disguise profile)
        // and respawns the entity so it re-reads the disguised skin/name. Without this, only the
        // tab list and chat (Bukkit display-name overrides) change.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!subject.isOnline()) {
                return;
            }

            for (Player viewer : entityReloadViewers) {
                if (viewer.isOnline() && viewer.canSee(subject)) {
                    viewer.hidePlayer(plugin, subject);
                }
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!subject.isOnline()) {
                    return;
                }

                List<Player> stillOnlineViewers = new ArrayList<>();
                for (Player viewer : entityReloadViewers) {
                    if (viewer.isOnline()) {
                        viewer.showPlayer(plugin, subject);
                        stillOnlineViewers.add(viewer);
                    }
                }
                if (!stillOnlineViewers.isEmpty()) {
                    try {
                        protocolManager.updateEntity(subject, stillOnlineViewers);
                    } catch (RuntimeException exception) {
                        plugin.getLogger().log(Level.WARNING, "Could not refresh disguised player entity after reload.", exception);
                    }
                }
            }, 2L);
        });
    }

    private List<Player> trackedViewers(Player subject) {
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

    public void refreshAllTracked() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refreshTracked(player);
        }
    }

    private void rewritePlayerInfo(PacketEvent event) {
        if (!settingsService.settings().enabled() || !settingsService.settings().apply().protocolProfile()) {
            return;
        }

        try {
            PacketContainer packet = event.getPacket();
            if (packet.getType() == PacketType.Play.Server.PLAYER_INFO) {
                rewritePlayerInfoDataList(event.getPlayer(), packet);
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not rewrite player info packet for disguise.", exception);
        }
    }

    private void rewritePlayerInfoDataList(Player viewer, PacketContainer packet) {
        // ProtocolLib exposes more than one List<PlayerInfoData> field for the 1.21.x player-info
        // update packet, and only one of them holds the real entries; the others read back as a
        // list of nulls. Process every field and rewrite whichever one actually carries entries.
        int listFields = packet.getPlayerInfoDataLists().size();
        for (int field = 0; field < listFields; field++) {
            List<PlayerInfoData> originalList = packet.getPlayerInfoDataLists().readSafely(field);
            if (originalList == null || originalList.isEmpty()) {
                continue;
            }

            List<PlayerInfoData> rewrittenList = new ArrayList<>(originalList.size());
            boolean changed = false;
            for (PlayerInfoData original : originalList) {
                if (original == null) {
                    rewrittenList.add(null);
                    continue;
                }
                PlayerInfoData rewritten = rewriteData(viewer, original);
                rewrittenList.add(rewritten);
                changed = changed || rewritten != original;
            }

            if (changed) {
                packet.getPlayerInfoDataLists().write(field, rewrittenList);
            }
        }
    }

    private PlayerInfoData rewriteData(Player viewer, PlayerInfoData original) {
        UUID subjectId = original.getProfileId();
        if (subjectId == null && original.getProfile() != null) {
            subjectId = original.getProfile().getUUID();
        }
        if (subjectId == null) {
            return original;
        }

        if (!shouldViewerSeeDisguise(viewer, subjectId)) {
            return original;
        }

        DisguiseStore.DisguiseEntry entry = disguiseStore.entry(subjectId).orElse(null);
        String nickname = nicknameStore.nickname(subjectId).orElse(null);
        if (entry == null && nickname == null) {
            return original;
        }

        // Precedence: a nickname overrides the shown name; the disguise supplies the skin
        // (and the name only when there is no nickname).
        String shownName = nickname != null ? nickname : entry.profileName();
        WrappedGameProfile profile = new WrappedGameProfile(subjectId, shownName);
        if (entry != null && !entry.textureValue().isBlank()) {
            String signature = entry.textureSignature().isBlank() ? null : entry.textureSignature();
            profile.getProperties().put(
                "textures",
                WrappedSignedProperty.fromValues("textures", entry.textureValue(), signature)
            );
        } else if (original.getProfile() != null) {
            // Nickname-only (no skin disguise): keep the player's real skin by carrying the
            // original profile's properties onto the renamed profile.
            profile.getProperties().putAll(original.getProfile().getProperties());
        }

        return new PlayerInfoData(
            subjectId,
            original.getLatency(),
            original.isListed(),
            original.getGameMode(),
            profile,
            original.getDisplayName(),
            original.isShowHat(),
            original.getListOrder(),
            original.getRemoteChatSessionData()
        );
    }

    private boolean shouldViewerSeeDisguise(Player viewer, UUID subjectId) {
        if (!settingsService.settings().visibility().selfSeesDisguise() && viewer.getUniqueId().equals(subjectId)) {
            return false;
        }
        Player subject = Bukkit.getPlayer(subjectId);
        return subject == null || viewer.canSee(subject);
    }
}

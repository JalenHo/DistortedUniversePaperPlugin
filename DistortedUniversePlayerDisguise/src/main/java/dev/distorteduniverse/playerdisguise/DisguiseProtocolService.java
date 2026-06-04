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
    private ProtocolManager protocolManager;

    public DisguiseProtocolService(Plugin plugin, DisguiseSettingsService settingsService, DisguiseStore disguiseStore) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.disguiseStore = disguiseStore;
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
        if (protocolManager == null || !subject.isOnline()) {
            return;
        }

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
        if (!viewers.isEmpty()) {
            protocolManager.updateEntity(subject, viewers);
        }
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
        if (packet.getPlayerInfoDataLists().size() == 0) {
            return;
        }

        List<PlayerInfoData> originalList = packet.getPlayerInfoDataLists().readSafely(0);
        if (originalList == null || originalList.isEmpty()) {
            return;
        }

        List<PlayerInfoData> rewrittenList = new ArrayList<>(originalList.size());
        boolean changed = false;
        for (PlayerInfoData original : originalList) {
            PlayerInfoData rewritten = rewriteData(viewer, original);
            rewrittenList.add(rewritten);
            changed = changed || rewritten != original;
        }

        if (changed) {
            packet.getPlayerInfoDataLists().write(0, rewrittenList);
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
        if (entry == null) {
            return original;
        }

        WrappedGameProfile profile = new WrappedGameProfile(subjectId, entry.profileName());
        String signature = entry.textureSignature().isBlank() ? null : entry.textureSignature();
        profile.getProperties().put(
            "textures",
            WrappedSignedProperty.fromValues("textures", entry.textureValue(), signature)
        );

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


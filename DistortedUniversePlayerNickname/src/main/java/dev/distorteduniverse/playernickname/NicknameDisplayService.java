package dev.distorteduniverse.playernickname;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class NicknameDisplayService {
    public static final String DISGUISE_METADATA_KEY = "distorteduniverse.playerdisguise.active";
    private static final String TEAM_PREFIX = "dunick_";

    private final Plugin plugin;
    private final NicknameSettingsService settingsService;
    private final NicknameStore nicknameStore;
    private final NicknameFormatter nicknameFormatter;
    private final NamespacedKey displayMarkerKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, TextDisplay> labels = new HashMap<>();
    private BukkitTask updateTask;
    private int ticksSinceLabelUpdate;

    public NicknameDisplayService(
        Plugin plugin,
        NicknameSettingsService settingsService,
        NicknameStore nicknameStore,
        NicknameFormatter nicknameFormatter
    ) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.nicknameStore = nicknameStore;
        this.nicknameFormatter = nicknameFormatter;
        displayMarkerKey = new NamespacedKey(plugin, "nickname_text_display");
        ownerKey = new NamespacedKey(plugin, "nickname_owner");
    }

    public void start() {
        cleanupStaleLabels();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLabels, 1L, 1L);
        refreshAll();
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        removeAllLabels();
        cleanupManagedTeams();
    }

    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
        }
    }

    public void apply(Player player) {
        removeLabel(player);
        clearManagedTeam(player);

        NicknameSettings settings = settingsService.settings();
        if (!settings.enabled()) {
            resetPaperNames(player);
            return;
        }

        if (player.hasMetadata(DISGUISE_METADATA_KEY)) {
            return;
        }

        NicknameStore.NicknameEntry entry = nicknameStore.entry(player.getUniqueId()).orElse(null);
        if (entry == null) {
            resetPaperNames(player);
            return;
        }

        NicknameFormatter.FormatResult formatted = nicknameFormatter.format(entry.nickname(), settings.validation());
        if (!formatted.success()) {
            plugin.getLogger().warning("Skipping invalid nickname for " + player.getName() + ": " + formatted.message());
            resetPaperNames(player);
            return;
        }

        if (settings.apply().chatDisplayName()) {
            player.displayName(formatted.component());
        }
        if (settings.apply().tabListName()) {
            player.playerListName(formatted.component());
        }

        applyAboveHead(player, formatted);
    }

    public void clear(Player player) {
        removeLabel(player);
        clearManagedTeam(player);
        resetPaperNames(player);
    }

    public void removeLabel(Player player) {
        TextDisplay label = labels.remove(player.getUniqueId());
        if (label != null && label.isValid()) {
            label.remove();
        }
    }

    private void applyAboveHead(Player player, NicknameFormatter.FormatResult formatted) {
        NicknameSettings settings = settingsService.settings();
        switch (settings.aboveHead().mode()) {
            case DISABLED -> {
            }
            case SCOREBOARD_AFFIX -> applyScoreboardAffix(player, formatted.component(), true);
            case TEXT_DISPLAY -> {
                if (settings.aboveHead().hideVanillaName()) {
                    applyScoreboardAffix(player, Component.empty(), false);
                }
                spawnLabel(player, formatted.component());
            }
        }
    }

    private void spawnLabel(Player player, Component nickname) {
        Location location = labelLocation(player);
        TextDisplay label = player.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(nickname);
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setGravity(false);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setVisibleByDefault(true);
            display.setShadowed(settingsService.settings().aboveHead().shadowed());
            display.setSeeThrough(settingsService.settings().aboveHead().seeThrough());
            display.setDefaultBackground(settingsService.settings().aboveHead().defaultBackground());
            display.setViewRange((float) settingsService.settings().aboveHead().viewRange());
            display.getPersistentDataContainer().set(displayMarkerKey, PersistentDataType.BYTE, (byte) 1);
            display.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        });
        labels.put(player.getUniqueId(), label);
    }

    private void tickLabels() {
        NicknameSettings settings = settingsService.settings();
        if (settings.aboveHead().mode() != NicknameSettings.AboveHeadMode.TEXT_DISPLAY) {
            return;
        }
        ticksSinceLabelUpdate++;
        if (ticksSinceLabelUpdate < settings.aboveHead().updateIntervalTicks()) {
            return;
        }
        ticksSinceLabelUpdate = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            TextDisplay label = labels.get(player.getUniqueId());
            if (label == null || !label.isValid()) {
                if (settings.enabled() && nicknameStore.entry(player.getUniqueId()).isPresent() && !player.hasMetadata(DISGUISE_METADATA_KEY)) {
                    apply(player);
                }
                continue;
            }

            label.teleport(labelLocation(player));
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(player) || viewer.canSee(player)) {
                    viewer.showEntity(plugin, label);
                } else {
                    viewer.hideEntity(plugin, label);
                }
            }
        }
    }

    private Location labelLocation(Player player) {
        return player.getLocation().clone().add(0.0D, settingsService.settings().aboveHead().yOffset(), 0.0D);
    }

    private void applyScoreboardAffix(Player player, Component prefix, boolean showRealName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String entry = player.getName();
        Team existingTeam = scoreboard.getEntryTeam(entry);
        if (existingTeam != null && !isManagedTeam(existingTeam) && !settingsService.settings().scoreboard().overrideExistingTeams()) {
            return;
        }

        if (existingTeam != null && isManagedTeam(existingTeam)) {
            existingTeam.removeEntry(entry);
        }

        Team team = scoreboard.getTeam(teamName(player.getUniqueId()));
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName(player.getUniqueId()));
        }
        team.prefix(prefix);
        team.suffix(Component.empty());
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, showRealName ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
    }

    private void clearManagedTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(teamName(player.getUniqueId()));
        if (team != null) {
            team.removeEntry(player.getName());
            if (team.getSize() == 0) {
                team.unregister();
            }
        }
    }

    private void cleanupManagedTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (isManagedTeam(team)) {
                team.unregister();
            }
        }
    }

    private static boolean isManagedTeam(Team team) {
        return team.getName().startsWith(TEAM_PREFIX);
    }

    private static String teamName(UUID playerId) {
        return TEAM_PREFIX + playerId.toString().replace("-", "").substring(0, 9);
    }

    private void resetPaperNames(Player player) {
        player.displayName(Component.text(player.getName()));
        player.playerListName(Component.text(player.getName()));
    }

    private void removeAllLabels() {
        for (TextDisplay label : labels.values()) {
            if (label.isValid()) {
                label.remove();
            }
        }
        labels.clear();
    }

    private void cleanupStaleLabels() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getType() == EntityType.TEXT_DISPLAY
                    && entity.getPersistentDataContainer().has(displayMarkerKey, PersistentDataType.BYTE)) {
                    entity.remove();
                }
            }
        }
    }
}

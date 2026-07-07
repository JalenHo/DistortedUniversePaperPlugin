package dev.distorteduniverse.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class TeamManager {
    private final DistortedUniverseTeamPlugin plugin;
    private final TeamStore teamStore;
    private final TeamPlayerStore playerStore;
    private final Scoreboard mainScoreboard;
    private final Map<UUID, String> playerTeams = new HashMap<>();

    public TeamManager(DistortedUniverseTeamPlugin plugin, TeamStore teamStore, TeamPlayerStore playerStore) {
        this.plugin = plugin;
        this.teamStore = teamStore;
        this.playerStore = playerStore;
        this.mainScoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
    }

    public void initialize() {
        for (Team team : teamStore.getAll()) {
            registerScoreboardTeam(team);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            applyTeamToPlayer(onlinePlayer);
        }
    }

    public void createTeam(String id, String displayName, String color, int maxSize) {
        TeamSettings settings = plugin.getSettingsService().settings();

        Team team = new Team(
            id,
            displayName,
            color,
            maxSize >= 0 ? maxSize : settings.defaultMaxSize(),
            settings.glow().enabledByDefault(),
            settings.glow().defaultColor(),
            null,
            new ArrayList<>()
        );

        teamStore.add(id, team);
        registerScoreboardTeam(team);
        teamStore.save();
    }

    public boolean deleteTeam(String id) {
        Team team = teamStore.get(id).orElse(null);
        if (team == null) {
            return false;
        }

        for (UUID member : team.members()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                removePlayerFromTeam(player);
            }
            playerStore.update(member, Bukkit.getOfflinePlayer(member).getName(), null);
        }

        unregisterScoreboardTeam(id);
        teamStore.remove(id);
        teamStore.save();
        return true;
    }

    public boolean addPlayerToTeam(Player player, String teamId) {
        Team team = teamStore.get(teamId).orElse(null);
        if (team == null) {
            return false;
        }

        Optional<Team> currentTeam = teamStore.getTeamByMember(player.getUniqueId());
        if (currentTeam.isPresent()) {
            removePlayerFromTeam(player);
        }

        if (team.isFull()) {
            return false;
        }

        List<UUID> members = new ArrayList<>(team.members());
        members.add(player.getUniqueId());
        team = team.withMembers(members);
        teamStore.update(teamId, team);

        playerStore.update(player.getUniqueId(), player.getName(), teamId);
        applyTeamToPlayer(player);

        teamStore.save();
        playerStore.save();

        if (team.autoKit() != null && plugin.getSettingsService().settings().autokit().enabled()) {
            plugin.getAutoKitIntegration().giveKit(player, team.autoKit());
        }

        return true;
    }

    public void removePlayerFromTeam(Player player) {
        Optional<Team> team = teamStore.getTeamByMember(player.getUniqueId());
        if (team.isEmpty()) {
            return;
        }

        Team t = team.get();
        List<UUID> members = new ArrayList<>(t.members());
        members.remove(player.getUniqueId());
        t = t.withMembers(members);
        teamStore.update(t.id(), t);

        playerTeams.remove(player.getUniqueId());
        playerStore.update(player.getUniqueId(), player.getName(), null);

        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam("duteam_" + t.id());
        if (scoreboardTeam != null) {
            scoreboardTeam.removePlayer(player);
        }

        teamStore.save();
        playerStore.save();
    }

    public void applyTeamToPlayer(Player player) {
        Optional<String> teamId = playerStore.getTeamId(player.getUniqueId());
        if (teamId.isEmpty()) {
            return;
        }

        Optional<Team> team = teamStore.get(teamId.get());
        if (team.isEmpty()) {
            return;
        }

        Team t = team.get();
        playerTeams.put(player.getUniqueId(), t.id());

        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam("duteam_" + t.id());
        if (scoreboardTeam != null) {
            scoreboardTeam.addPlayer(player);
            updateTeamGlow(scoreboardTeam, player, t);
        }
    }

    private void registerScoreboardTeam(Team team) {
        String teamName = "duteam_" + team.id();

        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam(teamName);
        if (scoreboardTeam == null) {
            scoreboardTeam = mainScoreboard.registerNewTeam(teamName);
        }

        scoreboardTeam.displayName(Component.text(team.displayName()));
        scoreboardTeam.prefix(Component.text("[", NamedTextColor.BLACK)
            .append(Component.text(team.displayName(), team.getTextColor()))
            .append(Component.text("] ", NamedTextColor.BLACK)));

        TeamSettings settings = plugin.getSettingsService().settings();
        scoreboardTeam.setAllowFriendlyFire(settings.friendlyFire());

        for (UUID member : team.members()) {
            Player player = Bukkit.getPlayer(member);
            if (player != null) {
                scoreboardTeam.addPlayer(player);
                updateTeamGlow(scoreboardTeam, player, team);
            }
        }
    }

    private void unregisterScoreboardTeam(String teamId) {
        org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam("duteam_" + teamId);
        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
        }
    }

    private void updateTeamGlow(org.bukkit.scoreboard.Team scoreboardTeam, Player player, Team team) {
        boolean hasAdminGlow = player.hasPermission("duteam.admin.glow");
        boolean shouldGlow = team.glowEnabled() || hasAdminGlow;

        player.setGlowing(shouldGlow);
        if (shouldGlow) {
            NamedTextColor glowColor = toNamedTextColor(team.glowColor());
            if (glowColor != null) {
                scoreboardTeam.color(glowColor);
            }
        }
    }

    private NamedTextColor toNamedTextColor(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "black" -> NamedTextColor.BLACK;
            case "dark_blue" -> NamedTextColor.DARK_BLUE;
            case "dark_green" -> NamedTextColor.DARK_GREEN;
            case "dark_aqua", "cyan" -> NamedTextColor.DARK_AQUA;
            case "dark_red", "red" -> NamedTextColor.DARK_RED;
            case "dark_purple", "purple" -> NamedTextColor.DARK_PURPLE;
            case "gold", "orange" -> NamedTextColor.GOLD;
            case "gray" -> NamedTextColor.GRAY;
            case "dark_gray" -> NamedTextColor.DARK_GRAY;
            case "blue" -> NamedTextColor.BLUE;
            case "green" -> NamedTextColor.GREEN;
            case "aqua" -> NamedTextColor.AQUA;
            case "light_purple", "pink" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> NamedTextColor.YELLOW;
            case "white" -> NamedTextColor.WHITE;
            default -> NamedTextColor.WHITE;
        };
    }


    public Optional<Team> getTeamById(String id) {
        return teamStore.get(id);
    }

    public Optional<Team> getTeamByPlayer(UUID uuid) {
        return teamStore.getTeamByMember(uuid);
    }

    public Collection<Team> getAllTeams() {
        return teamStore.getAll();
    }

    public void updateTeamSettings(Team team) {
        teamStore.update(team.id(), team);
        unregisterScoreboardTeam(team.id());
        registerScoreboardTeam(team);
        teamStore.save();
    }

    public void onPlayerJoin(Player player) {
        playerStore.update(player.getUniqueId(), player.getName(),
            playerStore.getTeamId(player.getUniqueId()).orElse(null));
        applyTeamToPlayer(player);
    }

    public void onPlayerQuit(Player player) {
        playerStore.save();
    }

    public void refreshFriendlyFire() {
        boolean allow = plugin.getSettingsService().settings().friendlyFire();
        for (Team team : teamStore.getAll()) {
            org.bukkit.scoreboard.Team scoreboardTeam = mainScoreboard.getTeam("duteam_" + team.id());
            if (scoreboardTeam != null) {
                scoreboardTeam.setAllowFriendlyFire(allow);
            }
        }
    }
}

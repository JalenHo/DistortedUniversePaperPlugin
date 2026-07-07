package dev.distorteduniverse.team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabExecutor {
    private final DistortedUniverseTeamPlugin plugin;
    private final TeamManager teamManager;
    private final TeamGuiManager guiManager;
    private final TeamSettingsService settingsService;

    private static final List<String> COLORS = List.of(
        "black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple",
        "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple",
        "yellow", "white"
    );

    public TeamCommand(DistortedUniverseTeamPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = plugin.getTeamManager();
        this.guiManager = plugin.getGuiManager();
        this.settingsService = plugin.getSettingsService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setcolor" -> handleSetColor(sender, args);
            case "setglow" -> handleSetGlow(sender, args);
            case "setmaxsize" -> handleSetMaxSize(sender, args);
            case "setkit" -> handleSetKit(sender, args);
            case "friendlyfire" -> handleFriendlyFire(sender, args);
            case "gui" -> handleGui(sender);
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "kick" -> handleKick(sender, args);
            case "reload" -> handleReload(sender);
            case "save" -> handleSave(sender);
            default -> {
                sendHelp(sender);
                yield false;
            }
        };
    }

    private boolean handleList(CommandSender sender) {
        Collection<Team> teams = teamManager.getAllTeams();

        if (teams.isEmpty()) {
            sender.sendMessage(Component.text("No teams exist yet.", NamedTextColor.YELLOW));
            return true;
        }

        sender.sendMessage(Component.text("=== Teams ===", NamedTextColor.GOLD));
        for (Team team : teams) {
            Component line = Component.text(team.displayName(), team.getTextColor())
                .append(Component.text(" (" + team.members().size() + " members)", NamedTextColor.GRAY));
            sender.sendMessage(line);
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /duteam info <team>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        Optional<Team> optTeam = teamManager.getTeamById(teamId);

        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get();
        sender.sendMessage(Component.text("=== Team Info: " + team.displayName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("ID: " + team.id(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Color: " + team.color(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Glow: " + (team.glowEnabled() ? "ON" : "OFF"), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Max Size: " + (team.maxSize() < 0 ? "Unlimited" : team.maxSize()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Members: " + team.members().size(), NamedTextColor.GRAY));
        if (team.autoKit() != null) {
            sender.sendMessage(Component.text("AutoKit: " + team.autoKit(), NamedTextColor.GRAY));
        }

        if (!team.members().isEmpty()) {
            sender.sendMessage(Component.text("Member List:", NamedTextColor.DARK_GRAY));
            for (UUID memberId : team.members()) {
                String memberName = Bukkit.getOfflinePlayer(memberId).getName();
                sender.sendMessage(Component.text("  - " + memberName, NamedTextColor.GRAY));
            }
        }
        return true;
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam create <id> <name> [color] [max-size]", NamedTextColor.RED));
            return true;
        }

        String id = args[1].toLowerCase();
        String name = args[2];
        String color = args.length >= 4 ? args[3] : settingsService.settings().defaultColor();
        int maxSize = args.length >= 5 ? parseIntSafe(args[4]) : -1;

        if (teamManager.getTeamById(id).isPresent()) {
            sender.sendMessage(Component.text("Team already exists: " + id, NamedTextColor.RED));
            return true;
        }

        teamManager.createTeam(id, name, color, maxSize);
        sender.sendMessage(Component.text("Created team: " + name + " (ID: " + id + ")", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /duteam delete <team>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        if (teamManager.deleteTeam(teamId)) {
            sender.sendMessage(Component.text("Deleted team: " + teamId, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleSetColor(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam setcolor <team> <color>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        String color = args[2].toLowerCase();

        Optional<Team> optTeam = teamManager.getTeamById(teamId);
        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get().withColor(color);
        teamManager.updateTeamSettings(team);
        sender.sendMessage(Component.text("Updated color for " + team.displayName() + " to " + color, NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetGlow(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam setglow <team> <on|off>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        boolean enabled = args[2].equalsIgnoreCase("on");

        Optional<Team> optTeam = teamManager.getTeamById(teamId);
        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get().withGlowEnabled(enabled);
        teamManager.updateTeamSettings(team);
        sender.sendMessage(Component.text("Glow " + (enabled ? "enabled" : "disabled") + " for " + team.displayName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetMaxSize(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam setmaxsize <team> <size|-1>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        int maxSize = parseIntSafe(args[2]);

        Optional<Team> optTeam = teamManager.getTeamById(teamId);
        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get().withMaxSize(maxSize);
        teamManager.updateTeamSettings(team);
        sender.sendMessage(Component.text("Max size for " + team.displayName() + " set to " + (maxSize < 0 ? "unlimited" : maxSize), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSetKit(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam setkit <team> <kit-id|none>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        String kitId = args[2].equalsIgnoreCase("none") ? null : args[2];

        Optional<Team> optTeam = teamManager.getTeamById(teamId);
        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get().withAutoKit(kitId);
        teamManager.updateTeamSettings(team);
        sender.sendMessage(Component.text("AutoKit for " + team.displayName() + " set to " + (kitId != null ? kitId : "none"), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleFriendlyFire(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            boolean enabled = args[1].equalsIgnoreCase("on");
            settingsService.settings().writeTo(plugin.getConfig());
            plugin.reloadConfig();
            sender.sendMessage(Component.text("Friendly fire " + (enabled ? "enabled" : "disabled"), NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Current friendly fire: " +
                (settingsService.settings().friendlyFire() ? "ENABLED" : "DISABLED"), NamedTextColor.AQUA));
        }
        return true;
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("GUI can only be opened by players!", NamedTextColor.RED));
            return true;
        }

        guiManager.openMainGui(player);
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can join teams!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /duteam join <team>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        Optional<Team> optTeam = teamManager.getTeamById(teamId);

        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        Team team = optTeam.get();
        if (team.isFull()) {
            sender.sendMessage(Component.text("Team " + team.displayName() + " is full!", NamedTextColor.RED));
            return true;
        }

        if (teamManager.addPlayerToTeam(player, teamId)) {
            sender.sendMessage(Component.text("You joined team " + team.displayName() + "!", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Failed to join team. You may already be in a team.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can leave teams!", NamedTextColor.RED));
            return true;
        }

        Optional<Team> currentTeam = teamManager.getTeamByPlayer(player.getUniqueId());
        if (currentTeam.isEmpty()) {
            sender.sendMessage(Component.text("You are not in a team!", NamedTextColor.YELLOW));
            return true;
        }

        teamManager.removePlayerFromTeam(player);
        sender.sendMessage(Component.text("You left team " + currentTeam.get().displayName(), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /duteam kick <team> <player>", NamedTextColor.RED));
            return true;
        }

        String teamId = args[1].toLowerCase();
        String playerName = args[2];

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + playerName, NamedTextColor.RED));
            return true;
        }

        Optional<Team> optTeam = teamManager.getTeamById(teamId);
        if (optTeam.isEmpty()) {
            sender.sendMessage(Component.text("Team not found: " + teamId, NamedTextColor.RED));
            return true;
        }

        if (!optTeam.get().hasMember(target.getUniqueId())) {
            sender.sendMessage(Component.text(target.getName() + " is not in team " + optTeam.get().displayName(), NamedTextColor.RED));
            return true;
        }

        teamManager.removePlayerFromTeam(target);
        sender.sendMessage(Component.text("Kicked " + target.getName() + " from " + optTeam.get().displayName(), NamedTextColor.GREEN));
        target.sendMessage(Component.text("You were kicked from " + optTeam.get().displayName(), NamedTextColor.RED));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        settingsService.reload();
        sender.sendMessage(Component.text("Configuration reloaded!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleSave(CommandSender sender) {
        plugin.saveAll();
        sender.sendMessage(Component.text("Data saved!", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== /duteam Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/duteam list", NamedTextColor.AQUA).append(Component.text(" - List all teams", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam info <team>", NamedTextColor.AQUA).append(Component.text(" - Show team info", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam create <id> <name> [color] [max]", NamedTextColor.AQUA).append(Component.text(" - Create team", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam delete <team>", NamedTextColor.AQUA).append(Component.text(" - Delete team", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam setcolor <team> <color>", NamedTextColor.AQUA).append(Component.text(" - Set team color", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam setglow <team> <on|off>", NamedTextColor.AQUA).append(Component.text(" - Toggle glow", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam setmaxsize <team> <size>", NamedTextColor.AQUA).append(Component.text(" - Set max size", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam setkit <team> <kit|none>", NamedTextColor.AQUA).append(Component.text(" - Set AutoKit", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam gui", NamedTextColor.AQUA).append(Component.text(" - Open admin GUI", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam join <team>", NamedTextColor.AQUA).append(Component.text(" - Join a team", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam leave", NamedTextColor.AQUA).append(Component.text(" - Leave current team", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam kick <team> <player>", NamedTextColor.AQUA).append(Component.text(" - Kick player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/duteam reload, /duteam save", NamedTextColor.AQUA).append(Component.text(" - Admin commands", NamedTextColor.GRAY)));
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("list", "info", "create", "delete", "setcolor", "setglow",
                "setmaxsize", "setkit", "friendlyfire", "gui", "join", "leave", "kick", "reload", "save"), args[0]);
        }

        return switch (args[0].toLowerCase()) {
            case "info", "delete", "setcolor", "setglow", "setmaxsize", "setkit", "kick" -> {
                if (args.length == 2) {
                    yield filter(teamManager.getAllTeams().stream().map(Team::id).toList(), args[1]);
                } else if (args.length == 3 && "kick".equals(args[0].toLowerCase())) {
                    yield filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
                } else if (args.length == 3 && "setcolor".equals(args[0].toLowerCase())) {
                    yield filter(COLORS, args[2]);
                } else if (args.length == 3 && "setglow".equals(args[0].toLowerCase())) {
                    yield filter(Arrays.asList("on", "off"), args[2]);
                }
                yield Collections.emptyList();
            }
            case "join" -> {
                if (args.length == 2) {
                    yield filter(teamManager.getAllTeams().stream().map(Team::id).toList(), args[1]);
                }
                yield Collections.emptyList();
            }
            case "friendlyfire" -> filter(Arrays.asList("on", "off"), args.length >= 2 ? args[1] : "");
            default -> Collections.emptyList();
        };
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
            .filter(s -> s.toLowerCase().startsWith(lower))
            .sorted()
            .collect(Collectors.toList());
    }
}

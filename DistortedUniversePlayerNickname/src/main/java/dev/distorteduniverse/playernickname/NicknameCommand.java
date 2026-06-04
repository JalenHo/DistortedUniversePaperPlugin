package dev.distorteduniverse.playernickname;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class NicknameCommand implements TabExecutor {
    private static final String PREFIX = "[DUNickname] ";
    private static final List<String> SUBCOMMANDS = List.of(
        "help",
        "status",
        "set",
        "clear",
        "list",
        "refresh",
        "reload",
        "save",
        "get",
        "config"
    );

    private final NicknameSettingsService settingsService;
    private final NicknameStore nicknameStore;
    private final NicknameFormatter nicknameFormatter;
    private final NicknameDisplayService displayService;

    public NicknameCommand(
        NicknameSettingsService settingsService,
        NicknameStore nicknameStore,
        NicknameFormatter nicknameFormatter,
        NicknameDisplayService displayService
    ) {
        this.settingsService = settingsService;
        this.nicknameStore = nicknameStore;
        this.nicknameFormatter = nicknameFormatter;
        this.displayService = displayService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status" -> sendStatus(sender, args);
            case "set" -> setNickname(sender, label, args);
            case "clear" -> clearNickname(sender, label, args);
            case "list" -> listNicknames(sender);
            case "refresh" -> {
                displayService.refreshAll();
                sender.sendMessage(PREFIX + "Refreshed online nicknames.");
            }
            case "reload" -> {
                settingsService.reload();
                nicknameStore.load();
                displayService.refreshAll();
                sender.sendMessage(PREFIX + "Reloaded config.yml and data.yml.");
            }
            case "save" -> {
                settingsService.save();
                nicknameStore.save();
                sender.sendMessage(PREFIX + "Saved config.yml and data.yml.");
            }
            case "get" -> getSetting(sender, label, args);
            case "config" -> setSetting(sender, label, args);
            default -> sender.sendMessage(PREFIX + "Unknown command. Use /" + label + " help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(SUBCOMMANDS, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("set", "clear", "status").contains(subcommand)) {
            return matching(playerNames(), args[1]);
        }
        if (args.length == 2 && (subcommand.equals("get") || subcommand.equals("config"))) {
            return matching(NicknameSettingParser.PATHS, args[1]);
        }
        if (args.length == 3 && subcommand.equals("config")) {
            return suggestionsForValue(args[1], args[2]);
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PREFIX + "/" + label + " status [player]");
        sender.sendMessage(PREFIX + "/" + label + " set <player> <nickname...>");
        sender.sendMessage(PREFIX + "/" + label + " clear <player>");
        sender.sendMessage(PREFIX + "/" + label + " list");
        sender.sendMessage(PREFIX + "/" + label + " refresh");
        sender.sendMessage(PREFIX + "/" + label + " get <path>");
        sender.sendMessage(PREFIX + "/" + label + " config <path> <value...>");
        sender.sendMessage(PREFIX + "/" + label + " reload");
        sender.sendMessage(PREFIX + "/" + label + " save");
    }

    private void sendStatus(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            TargetPlayer target = resolveTarget(args[1]);
            if (target == null) {
                sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
                return;
            }
            String nickname = nicknameStore.entry(target.playerId())
                .map(NicknameStore.NicknameEntry::nickname)
                .orElse("none");
            sender.sendMessage(PREFIX + target.name() + " nickname: " + nickname);
            return;
        }

        NicknameSettings settings = settingsService.settings();
        sender.sendMessage(PREFIX + "plugin: " + enabled(settings.enabled())
            + ", chat-display-name=" + settings.apply().chatDisplayName()
            + ", tab-list-name=" + settings.apply().tabListName()
            + ", above-head.mode=" + settings.aboveHead().mode().configValue());
        sender.sendMessage(PREFIX + "nicknamed players: " + nicknameStore.size());
    }

    private void setNickname(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " set <player> <nickname...>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        String nickname = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        NicknameFormatter.FormatResult formatted = nicknameFormatter.format(nickname, settingsService.settings().validation());
        if (!formatted.success()) {
            sender.sendMessage(PREFIX + "Error: " + formatted.message());
            return;
        }

        boolean changed = nicknameStore.setNickname(target.playerId(), target.name(), nickname);
        nicknameStore.save();
        Player onlinePlayer = Bukkit.getPlayer(target.playerId());
        if (onlinePlayer != null) {
            displayService.apply(onlinePlayer);
        }
        sender.sendMessage(PREFIX + target.name() + (changed ? " nickname set to " : " nickname was already ") + formatted.plainText() + ".");
    }

    private void clearNickname(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " clear <player>");
            return;
        }

        TargetPlayer target = resolveTarget(args[1]);
        if (target == null) {
            sender.sendMessage(PREFIX + "Player must be online or known to this server: " + args[1]);
            return;
        }

        boolean changed = nicknameStore.clearNickname(target.playerId());
        nicknameStore.save();
        Player onlinePlayer = Bukkit.getPlayer(target.playerId());
        if (onlinePlayer != null) {
            displayService.clear(onlinePlayer);
        }
        sender.sendMessage(PREFIX + target.name() + (changed ? " nickname cleared." : " had no nickname."));
    }

    private void listNicknames(CommandSender sender) {
        List<NicknameStore.NicknameEntry> entries = nicknameStore.entries();
        if (entries.isEmpty()) {
            sender.sendMessage(PREFIX + "No nicknames are set.");
            return;
        }

        String summary = String.join(", ", entries.stream()
            .map(entry -> entry.name() + "=" + entry.nickname())
            .toList());
        sender.sendMessage(PREFIX + "Nicknames: " + summary);
    }

    private void getSetting(CommandSender sender, String label, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " get <path>");
            return;
        }
        sendResult(sender, settingsService.get(args[1]));
    }

    private void setSetting(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /" + label + " config <path> <value...>");
            return;
        }
        String path = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        sendResult(sender, settingsService.set(path, value));
        displayService.refreshAll();
    }

    private TargetPlayer resolveTarget(String rawName) {
        Player onlinePlayer = Bukkit.getPlayerExact(rawName);
        if (onlinePlayer != null) {
            return new TargetPlayer(onlinePlayer.getUniqueId(), onlinePlayer.getName());
        }

        return nicknameStore.findByName(rawName)
            .map(entry -> new TargetPlayer(entry.playerId(), entry.name()))
            .orElseGet(() -> resolveCachedOfflinePlayer(rawName));
    }

    private static TargetPlayer resolveCachedOfflinePlayer(String rawName) {
        String normalizedName = rawName.toLowerCase(Locale.ROOT);
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).equals(normalizedName)) {
                return new TargetPlayer(offlinePlayer.getUniqueId(), name);
            }
        }
        return null;
    }

    private List<String> playerNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        names.addAll(nicknameStore.knownNames());
        return new ArrayList<>(names);
    }

    private static void sendResult(CommandSender sender, NicknameSettingsService.SettingResult result) {
        sender.sendMessage(PREFIX + (result.success() ? result.message() : "Error: " + result.message()));
    }

    private static String enabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static List<String> suggestionsForValue(String rawPath, String prefix) {
        String path = NicknameSettingParser.normalizePath(rawPath);
        if (!NicknameSettingParser.PATHS.contains(path)) {
            return List.of();
        }
        if (path.endsWith(".enabled")
            || path.startsWith("apply.")
            || path.startsWith("validation.allow-")
            || path.equals("above-head.hide-vanilla-name")
            || path.equals("above-head.shadowed")
            || path.equals("above-head.see-through")
            || path.equals("above-head.default-background")
            || path.equals("scoreboard.override-existing-teams")) {
            return matching(List.of("true", "false"), prefix);
        }
        if (path.equals("above-head.mode")) {
            return matching(List.of("text-display", "scoreboard-affix", "disabled"), prefix);
        }
        return List.of();
    }

    private static List<String> matching(List<String> values, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private record TargetPlayer(UUID playerId, String name) {
    }
}


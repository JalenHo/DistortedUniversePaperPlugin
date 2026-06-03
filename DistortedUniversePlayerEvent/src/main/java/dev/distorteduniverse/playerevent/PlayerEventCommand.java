package dev.distorteduniverse.playerevent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class PlayerEventCommand implements TabExecutor {
    private static final String PREFIX = "[DUPlayerEvent] ";
    private static final List<String> SUBCOMMANDS = List.of(
        "status",
        "enable",
        "disable",
        "get",
        "set",
        "reload",
        "save",
        "test-sound",
        "help"
    );

    private final SettingsService settingsService;
    private final MessageFormatter messageFormatter;

    public PlayerEventCommand(SettingsService settingsService, MessageFormatter messageFormatter) {
        this.settingsService = settingsService;
        this.messageFormatter = messageFormatter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status" -> sendStatus(sender);
            case "reload" -> {
                settingsService.reload();
                sender.sendMessage(PREFIX + "Reloaded config.yml. Live settings updated.");
            }
            case "save" -> {
                settingsService.save();
                sender.sendMessage(PREFIX + "Saved live settings to config.yml.");
            }
            case "enable", "disable" -> setModuleEnabled(sender, args, subcommand.equals("enable"));
            case "get" -> getSetting(sender, args);
            case "set" -> setSetting(sender, args);
            case "test-sound" -> testSound(sender, args);
            default -> {
                sender.sendMessage(PREFIX + "Unknown command. Use /" + label + " help.");
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(SUBCOMMANDS, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (subcommand.equals("enable") || subcommand.equals("disable"))) {
            return matching(SettingValueParser.MODULES, args[1]);
        }
        if (args.length == 2 && (subcommand.equals("get") || subcommand.equals("set"))) {
            return matching(SettingValueParser.PATHS, args[1]);
        }
        if (args.length == 3 && subcommand.equals("set")) {
            return suggestionsForValue(args[1], args[2]);
        }
        if (args.length == 2 && subcommand.equals("test-sound")) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + "/duplayerevent status");
        sender.sendMessage(PREFIX + "/duplayerevent enable <death-sound|death-message|leave-message|join-message|death-kick>");
        sender.sendMessage(PREFIX + "/duplayerevent disable <death-sound|death-message|leave-message|join-message|death-kick>");
        sender.sendMessage(PREFIX + "/duplayerevent get <path>");
        sender.sendMessage(PREFIX + "/duplayerevent set <path> <value...>");
        sender.sendMessage(PREFIX + "/duplayerevent reload");
        sender.sendMessage(PREFIX + "/duplayerevent save");
        sender.sendMessage(PREFIX + "/duplayerevent test-sound [player]");
    }

    private void sendStatus(CommandSender sender) {
        PluginSettings settings = settingsService.settings();
        sender.sendMessage(PREFIX + "death-sound: " + enabled(settings.deathSound().enabled())
            + ", radius=" + settings.deathSound().radius()
            + ", sound=" + settings.deathSound().sound()
            + ", category=" + settings.deathSound().category()
            + ", volume=" + settings.deathSound().volume()
            + ", pitch=" + settings.deathSound().pitch()
            + ", suppress-vanilla=" + settings.deathSound().suppressVanilla());
        sender.sendMessage(PREFIX + "death-message: " + enabled(settings.deathMessage().enabled())
            + ", radius=" + settings.deathMessage().radius()
            + ", respect-gamerule=" + settings.deathMessage().respectGamerule()
            + ", template=" + settings.deathMessage().template());
        sender.sendMessage(PREFIX + "leave-message: " + enabled(settings.leaveMessage().enabled())
            + ", radius=" + settings.leaveMessage().radius()
            + ", template=" + settings.leaveMessage().template());
        sender.sendMessage(PREFIX + "join-message: " + enabled(settings.joinMessage().enabled())
            + ", radius=" + settings.joinMessage().radius()
            + ", template=" + settings.joinMessage().template());
        sender.sendMessage(PREFIX + "death-kick: " + enabled(settings.deathKick().enabled())
            + ", delay-ticks=" + settings.deathKick().delayTicks()
            + ", show-leave-message=" + settings.deathKick().showLeaveMessage()
            + ", leave-radius=" + settings.deathKick().leaveRadius());
    }

    private void setModuleEnabled(CommandSender sender, String[] args, boolean enabled) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /duplayerevent " + (enabled ? "enable" : "disable") + " <module>");
            return;
        }
        SettingsService.SettingResult result = settingsService.setModuleEnabled(args[1], enabled);
        sendResult(sender, result);
    }

    private void getSetting(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(PREFIX + "Usage: /duplayerevent get <path>");
            return;
        }
        sendResult(sender, settingsService.get(args[1]));
    }

    private void setSetting(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(PREFIX + "Usage: /duplayerevent set <path> <value...>");
            return;
        }
        String path = args[1];
        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (SettingValueParser.isTemplatePath(path) && !messageFormatter.isTemplateValid(value)) {
            sender.sendMessage(PREFIX + "Invalid MiniMessage template.");
            return;
        }
        sendResult(sender, settingsService.set(path, value));
    }

    private void testSound(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(PREFIX + "Player is not online: " + args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage(PREFIX + "Console must provide a player: /duplayerevent test-sound <player>");
            return;
        }

        PluginSettings.DeathSoundSettings sound = settingsService.settings().deathSound();
        target.playSound(target.getLocation(), sound.sound(), sound.category(), sound.volume(), sound.pitch());
        sender.sendMessage(PREFIX + "Played " + sound.sound() + " for " + target.getName() + ".");
    }

    private static void sendResult(CommandSender sender, SettingsService.SettingResult result) {
        sender.sendMessage(PREFIX + (result.success() ? result.message() : "Error: " + result.message()));
    }

    private static String enabled(boolean enabled) {
        return enabled ? "enabled" : "disabled";
    }

    private static List<String> suggestionsForValue(String rawPath, String prefix) {
        String path = SettingValueParser.normalizePath(rawPath);
        if (!SettingValueParser.PATHS.contains(path)) {
            return List.of();
        }
        if (path.endsWith(".enabled")
            || path.equals("death-sound.suppress-vanilla")
            || path.equals("death-message.respect-gamerule")
            || path.equals("death-kick.show-leave-message")) {
            return matching(List.of("true", "false"), prefix);
        }
        if (path.equals("death-sound.category")) {
            return matching(Stream.of(SoundCategory.values()).map(Enum::name).toList(), prefix);
        }
        if (path.equals("death-sound.sound")) {
            return matching(List.of(
                "minecraft:entity.wither.death",
                "minecraft:entity.player.death",
                "minecraft:entity.lightning_bolt.thunder"
            ), prefix);
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
}

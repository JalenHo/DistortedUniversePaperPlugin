# DistortedUniversePlayerEvent

A PaperMC server-side plugin for Minecraft `1.21.11`.

The plugin scopes player death sounds, death messages, join messages, leave messages, and death-kick quit notices by distance. All feature modules can be changed live in game by an operator without restarting the server.

## Requirements

- Minecraft / Paper `1.21.11`
- Java `21`
- Paper API `1.21.11`

## Build

Use the included Gradle wrapper:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
```

The plugin jar is created at:

```text
DistortedUniversePlayerEvent/build/libs/DistortedUniversePlayerEvent-0.9.2.jar
```

## Install

1. Build the jar.
2. Copy `DistortedUniversePlayerEvent/build/libs/DistortedUniversePlayerEvent-0.9.2.jar` into your Paper server's `plugins` folder.
3. Start or restart the server once.
4. Configure live in game with `/duplayerevent`.

The admin permission is:

```text
distorteduniverse.playerevent.admin
```

It defaults to server operators.

## Command

Main command:

```text
/duplayerevent
```

Alias:

```text
/dupe
```

## Commands Reference

| Command | Description |
| --- | --- |
| `/duplayerevent help` | Show command usage. Same as running `/duplayerevent` with no arguments. |
| `/duplayerevent status` | Print the current live value of every module and setting. |
| `/duplayerevent enable <module>` | Turn on one feature module. Equivalent to setting `<module>.enabled` to `true`. |
| `/duplayerevent disable <module>` | Turn off one feature module. Equivalent to setting `<module>.enabled` to `false`. |
| `/duplayerevent get <path>` | Read one setting path from the live in-memory config. |
| `/duplayerevent set <path> <value...>` | Change one setting path live, validate it, and save to `config.yml`. Template paths are checked for valid MiniMessage before saving. |
| `/duplayerevent reload` | Reload `config.yml` from disk and replace the live in-memory settings. |
| `/duplayerevent save` | Write the current live settings back to `config.yml`. |
| `/duplayerevent test-sound [player]` | Play the configured death sound once for a player. If no player is given, the command sender must be a player and hears the sound at their own location. Console must provide a player name. |

## Modules Reference

Each module is a group of related settings under one prefix. Use `enable` / `disable` or set `<module>.enabled` directly.

| Module | What it does |
| --- | --- |
| `death-sound` | Plays a custom sound to nearby players when someone dies. Can optionally suppress the vanilla death sound. |
| `death-message` | Replaces the global death broadcast with a distance-scoped custom message. Can respect the `showDeathMessages` gamerule. |
| `join-message` | Replaces the global join broadcast with a distance-scoped custom message. |
| `leave-message` | Replaces the global quit broadcast with a distance-scoped custom message for normal disconnects. |
| `death-kick` | Kicks a player shortly after death and can optionally send a nearby leave-style notice at the death location. |

## Settings Reference

Boolean settings accept `true` / `false`, `on` / `off`, `yes` / `no`, `enable` / `disable`, and `1` / `0`.

### `death-sound`

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `death-sound.enabled` | boolean | `true` | Master switch for the custom nearby death sound. |
| `death-sound.radius` | number | `64.0` | Distance in blocks. Only same-world players within this radius hear the sound. Range: `0` to `100000`. |
| `death-sound.sound` | sound key | `minecraft:entity.wither.death` | Namespaced sound key. The `minecraft:` prefix is added automatically if omitted. Vanilla `minecraft:` keys are validated against the server registry. |
| `death-sound.category` | enum | `PLAYERS` | Bukkit `SoundCategory` name, such as `MASTER`, `MUSIC`, `RECORDS`, `WEATHER`, `BLOCKS`, `HOSTILE`, `NEUTRAL`, `PLAYERS`, `AMBIENT`, `VOICE`, `UI`. |
| `death-sound.volume` | number | `1.0` | Playback volume. Range: `0.0` to `10.0`. |
| `death-sound.pitch` | number | `1.0` | Playback pitch. Range: `0.5` to `2.0`. |
| `death-sound.suppress-vanilla` | boolean | `true` | When `true`, cancels the vanilla death sound for the dying player so only the configured sound is used. |

### `death-message`

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `death-message.enabled` | boolean | `true` | Master switch for custom nearby death messages. |
| `death-message.radius` | number | `64.0` | Distance in blocks for recipients in the same world. Range: `0` to `100000`. |
| `death-message.respect-gamerule` | boolean | `true` | When `true`, no custom death message is sent if the `showDeathMessages` gamerule is off. |
| `death-message.template` | MiniMessage | `<death_message>` | Message format sent to nearby players. See Message Placeholders below. |

### `join-message`

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `join-message.enabled` | boolean | `true` | Master switch for custom nearby join messages. |
| `join-message.radius` | number | `64.0` | Distance in blocks for recipients in the same world. Range: `0` to `100000`. |
| `join-message.template` | MiniMessage | `<join_message>` | Message format sent to nearby players when someone joins. |

### `leave-message`

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `leave-message.enabled` | boolean | `true` | Master switch for custom nearby leave messages on normal quits. |
| `leave-message.radius` | number | `64.0` | Distance in blocks for recipients in the same world. Range: `0` to `100000`. |
| `leave-message.template` | MiniMessage | `<quit_message>` | Message format sent to nearby players when someone leaves normally. |

### `death-kick`

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `death-kick.enabled` | boolean | `true` | When `true`, kicks the player shortly after death. |
| `death-kick.delay-ticks` | integer | `0` | Ticks to wait after death before kicking. `20` ticks = 1 second. Range: `0` to `1200`. |
| `death-kick.kick-message` | MiniMessage | `You died.` | Message shown only to the kicked player. |
| `death-kick.show-leave-message` | boolean | `false` | When `true`, sends a nearby leave-style notice at the death location when the kicked player disconnects. |
| `death-kick.leave-radius` | number | `64.0` | Distance in blocks for the death-kick leave notice. Range: `0` to `100000`. |
| `death-kick.leave-template` | MiniMessage | `<yellow><player_name> left the game</yellow>` | Message format for the optional death-kick leave notice. |

## Message Placeholders

Message templates use MiniMessage and support:

| Placeholder | Description |
| --- | --- |
| `<player>` | The player's current display name component. |
| `<player_name>` | The player's real username string. |
| `<display_name>` | Same source as `<player>`. |
| `<world>` | World name at the event location. |
| `<x>`, `<y>`, `<z>` | Formatted coordinates at the event location. |
| `<death_message>` | Vanilla death message component, when applicable. |
| `<join_message>` | Vanilla join message component, when applicable. |
| `<quit_message>` | Vanilla quit message component, when applicable. |

MiniMessage color and style tags are also supported, including `<red>`, `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, and `<reset>`.

Default templates preserve vanilla-looking messages:

```text
death-message.template = <death_message>
join-message.template = <join_message>
leave-message.template = <quit_message>
```

## Examples

Disable death kicking:

```text
/duplayerevent disable death-kick
```

Change death sound radius:

```text
/duplayerevent set death-sound.radius 80
```

Use a vanilla sound:

```text
/duplayerevent set death-sound.sound minecraft:entity.lightning_bolt.thunder
```

Set max death sound volume:

```text
/duplayerevent set death-sound.volume 10
```

Change join message:

```text
/duplayerevent set join-message.template <green><player_name></green> joined nearby
```

Change leave message:

```text
/duplayerevent set leave-message.template <yellow><player_name></yellow> left nearby
```

Turn on a distance-scoped quit notice after death kick:

```text
/duplayerevent set death-kick.show-leave-message true
```

## Sound Notes

Vanilla sound keys work server-side:

```text
minecraft:entity.player.death
minecraft:entity.wither.death
minecraft:entity.lightning_bolt.thunder
```

Custom non-vanilla sound keys are supported, but clients must have a resource pack that defines the sound. A server-side plugin cannot send brand-new audio files by itself.

When setting `death-sound.sound`, vanilla `minecraft:` keys are validated against the server sound registry. Unknown vanilla keys are rejected with an error message.

## Default Behavior

- Death sounds are sent only to same-world players within `death-sound.radius`.
- Death messages are sent only to same-world players within `death-message.radius`.
- Join messages are sent only to same-world players within `join-message.radius`.
- Leave messages are sent only to same-world players within `leave-message.radius`.
- Players are kicked shortly after death when `death-kick.enabled` is true.
- Death-kick leave notices are disabled by default.
- During a death kick, the vanilla kick leave message is suppressed so only the configured death-kick leave notice can appear.
- Death-kick state is cleaned up after quit or rejoin so a later normal quit does not reuse the death-kick leave template.

## Local Testing

This repository does not include a Paper server jar or generated server world files. Keep local test servers outside the repository and copy the built plugin jar into that server's `plugins` folder.

Paper downloads: <https://papermc.io/downloads/paper>

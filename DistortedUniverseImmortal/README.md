# DistortedUniverseImmortal

A PaperMC server-side plugin for Minecraft `1.21.11`.

The plugin lets operators mark specific players as immortal. Immortal players still take normal damage until the configured health floor is reached, then further damage is reduced so they do not drop below that amount.

## Requirements

- Minecraft / Paper `1.21.11`
- Java `21`
- Paper API `1.21.11`

## Build

Use the included Gradle wrapper:

```powershell
.\gradlew.bat :DistortedUniverseImmortal:clean :DistortedUniverseImmortal:build
```

The plugin jar is created at:

```text
DistortedUniverseImmortal/build/libs/DistortedUniverseImmortal-0.9.2.jar
```

## Install

1. Build the jar.
2. Copy `DistortedUniverseImmortal/build/libs/DistortedUniverseImmortal-0.9.2.jar` into your Paper server's `plugins` folder.
3. Start or restart the server once.
4. Configure live in game with `/duimmortal`.

The admin permission is:

```text
distorteduniverse.immortal.admin
```

It defaults to server operators.

## Commands

Main command:

```text
/duimmortal
```

Aliases:

```text
/dui
/immortal
```

## Commands Reference

| Command | Description |
| --- | --- |
| `/duimmortal help` | Show command usage. Same as running `/duimmortal` with no arguments. |
| `/duimmortal status` | Show plugin-wide settings and how many players are marked immortal. |
| `/duimmortal status <player>` | Show whether one player is currently marked immortal. |
| `/duimmortal set <player>` | Mark a player immortal and save to `data.yml`. |
| `/duimmortal unset <player>` | Remove a player's immortality and save to `data.yml`. |
| `/duimmortal toggle <player>` | Flip a player's immortality on or off. |
| `/duimmortal list` | List all players currently stored as immortal. |
| `/duimmortal get <path>` | Read one setting path from the live in-memory config. |
| `/duimmortal config <path> <value>` | Change one setting path live, validate it, and save to `config.yml`. |
| `/duimmortal reload` | Reload `config.yml` and `data.yml` from disk. |
| `/duimmortal save` | Write the current live config and immortal player list back to disk. |
| `/immortal <player>` | Shortcut alias for `/duimmortal toggle <player>`. |

Player targeting accepts:

- an online player name
- a name already stored in `data.yml`
- a name found in the server's offline-player cache

## Settings Reference

Boolean settings accept `true` / `false`, `on` / `off`, `yes` / `no`, `enable` / `disable`, and `1` / `0`.

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Master switch for immortality handling. When `false`, no damage adjustment runs, but saved immortal players remain in `data.yml`. |
| `minimum-health` | number | `1.0` | Lowest health an immortal player can be reduced to. Minecraft uses `2.0` health points per heart, so `1.0` is half a heart. Range: `0.5` to `1024.0`. |
| `totem-compatibility.enabled` | boolean | `true` | When `true`, lethal damage can still trigger a held Totem of Undying in main hand or off hand instead of being reduced to the floor. |

## How Immortality Works

| Behavior | Description |
| --- | --- |
| Normal damage down to the floor | Immortal players take ordinary damage until they reach `minimum-health`. |
| Floor protection | Once at or near the floor, incoming damage is reduced so health does not fall below `minimum-health`. |
| Totem bypass | If lethal damage would kill the player and totem compatibility is enabled, the totem can still activate normally. |
| Invulnerability window | When damage is fully absorbed at the floor, the plugin preserves the normal post-hit invulnerability ticks so rapid hits do not stack through the floor. |
| Persistence | Immortal status is stored by UUID in `plugins/DistortedUniverseImmortal/data.yml`. |
| Name tracking | Player names in `data.yml` are refreshed when they join. |

## Examples

Set a player as immortal:

```text
/duimmortal set Steve
```

Remove immortality:

```text
/duimmortal unset Steve
```

Toggle immortality:

```text
/immortal Steve
```

Set the floor to one full heart:

```text
/duimmortal config minimum-health 2.0
```

Disable the whole plugin without removing the saved immortal list:

```text
/duimmortal config enabled false
```

Check plugin status:

```text
/duimmortal status
```

List immortal players:

```text
/duimmortal list
```

## Data Files

```text
plugins/DistortedUniverseImmortal/config.yml
plugins/DistortedUniverseImmortal/data.yml
```

- `config.yml` stores plugin settings.
- `data.yml` stores immortal player UUIDs and their last known names.

## Notes

- A player must be online or already known to the server cache to be targeted by command.
- If `totem-compatibility.enabled` is true, lethal damage can still activate a held Totem of Undying.

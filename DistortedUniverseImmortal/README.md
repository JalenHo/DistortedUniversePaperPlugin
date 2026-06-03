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
DistortedUniverseImmortal/build/libs/DistortedUniverseImmortal-0.9.0.jar
```

## Install

1. Build the jar.
2. Copy `DistortedUniverseImmortal/build/libs/DistortedUniverseImmortal-0.9.0.jar` into your Paper server's `plugins` folder.
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

Common commands:

```text
/duimmortal status
/duimmortal status <player>
/duimmortal set <player>
/duimmortal unset <player>
/duimmortal toggle <player>
/duimmortal list
/duimmortal get <path>
/duimmortal config <path> <value>
/duimmortal reload
/duimmortal save
```

Shortcut:

```text
/immortal <player>
```

This toggles immortality for the player.

## Settings

```text
enabled
minimum-health
totem-compatibility.enabled
```

Minecraft health uses `2.0` points per heart. The default `minimum-health` is `1.0`, which means half a heart.

## Examples

Set a player as immortal:

```text
/duimmortal set Steve
```

Remove immortality:

```text
/duimmortal unset Steve
```

Set the floor to one full heart:

```text
/duimmortal config minimum-health 2.0
```

Disable the whole plugin without removing the saved immortal list:

```text
/duimmortal config enabled false
```

## Notes

- Immortal players are stored by UUID in `plugins/DistortedUniverseImmortal/data.yml`.
- A player must be online or already known to the server cache to be targeted by command.
- If `totem-compatibility.enabled` is true, lethal damage can still activate a held Totem of Undying.

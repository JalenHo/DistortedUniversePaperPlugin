# DistortedUniversePlayerNickname

A PaperMC server-side plugin for Minecraft `1.21.11`.

The plugin stores persistent per-player nicknames by UUID and applies them to Paper display names, tab list names, and an optional above-head visual label.

This is an optional companion plugin. `DistortedUniversePlayerDisguise` already includes a simpler built-in nickname (`/dudisguise nick`) for tab, chat, nametag, and kill-feed names. Use this plugin when you need above-head display modes, MiniMessage validation, or nickname settings independent of ProtocolLib disguises.

The GitHub prerelease workflow does not upload this jar. Build it locally when needed.

## Requirements

- Minecraft / Paper `1.21.11`
- Java `21`
- Paper API `1.21.11`

## Build

```powershell
.\gradlew.bat :DistortedUniversePlayerNickname:clean :DistortedUniversePlayerNickname:build
```

The plugin jar is created at:

```text
DistortedUniversePlayerNickname/build/libs/DistortedUniversePlayerNickname-0.9.2.jar
```

## Install

1. Build the jar.
2. Copy the jar into your Paper server's `plugins` folder.
3. Start or restart the server once.
4. Configure live in game with `/dunickname`.

The admin permission is:

```text
distorteduniverse.playernickname.admin
```

## Commands

```text
/dunickname status [player]
/dunickname set <player> <nickname...>
/dunickname clear <player>
/dunickname list
/dunickname refresh
/dunickname get <path>
/dunickname config <path> <value...>
/dunickname reload
/dunickname save
```

Alias:

```text
/dunick
```

## Settings

```text
enabled
apply.chat-display-name
apply.tab-list-name
above-head.mode
above-head.hide-vanilla-name
above-head.y-offset
above-head.update-interval-ticks
above-head.view-range
above-head.shadowed
above-head.see-through
above-head.default-background
scoreboard.override-existing-teams
validation.allow-minimessage
validation.allow-spaces
validation.min-plain-length
validation.max-plain-length
```

## Examples

```text
/dunickname set Steve <gold>Traveler</gold>
/dunickname clear Steve
/dunickname config above-head.mode text-display
/dunickname config above-head.mode scoreboard-affix
/dunickname config above-head.mode disabled
```

## Notes

- `text-display` mode uses a separate `TextDisplay` entity, so the above-head label may not be visually identical to Minecraft's native player nametag.
- `scoreboard-affix` mode can add a prefix but cannot fully replace the real username above the head.
- If `DistortedUniversePlayerDisguise` is active for a player (disguise or built-in `/dudisguise nick`), nickname display is skipped for that player until the disguise plugin clears that state.
- Other chat, tab, scoreboard, nickname, or disguise plugins may override this plugin's output.

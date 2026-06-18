# DistortedUniversePlayerDisguise

An experimental PaperMC server-side plugin for Minecraft `1.21.11` using ProtocolLib.

The plugin stores persistent per-player disguises by UUID and attempts to make one player appear as another Minecraft username and skin to other players. It also includes a built-in custom nickname (`/dudisguise nick`) that changes only the shown name while keeping the player's real skin.

## Requirements

- Minecraft / Paper `1.21.11`
- Java `21`
- Paper API `1.21.11`
- ProtocolLib compatible with Minecraft `1.21.11`. Local Java `21` smoke testing used the GitHub ProtocolLib `5.4.0` release jar; ProtocolLib warns that Minecraft `1.21.11` is not yet tested.

## Build

```powershell
.\gradlew.bat :DistortedUniversePlayerDisguise:clean :DistortedUniversePlayerDisguise:build
```

The plugin jar is created at:

```text
DistortedUniversePlayerDisguise/build/libs/DistortedUniversePlayerDisguise-0.9.2.jar
```

## Install

1. Install ProtocolLib on the server. The GitHub ProtocolLib `5.4.0` release jar was smoke-tested with Java `21` and Paper `1.21.11`.
2. Build this jar.
3. Copy the jar into your Paper server's `plugins` folder.
4. Start or restart the server once.
5. Configure live in game with `/dudisguise`.

The admin permission is:

```text
distorteduniverse.playerdisguise.admin
```

## Commands

```text
/dudisguise status [player]
/dudisguise set <player> <minecraft-username>
/dudisguise clear <player>
/dudisguise nick <player> <nickname>
/dudisguise nick <player>
/dudisguise list
/dudisguise refresh
/dudisguise cache-refresh <minecraft-username>
/dudisguise get <path>
/dudisguise config <path> <value...>
/dudisguise reload
/dudisguise save
```

`/dudisguise nick` stores a plain-text nickname in `nicknames.yml`. Omit the nickname argument to clear it. A nickname can be used with or without a skin disguise.

Alias:

```text
/dudis
```

## Settings

```text
enabled
visibility.self-sees-disguise
profile-lookup.cache-days
profile-lookup.refresh-expired-cache
apply.chat-display-name
apply.tab-list-name
apply.protocol-profile
```

## Examples

```text
/dudisguise set Steve Notch
/dudisguise clear Steve
/dudisguise nick Steve Traveler
/dudisguise nick Steve
/dudisguise cache-refresh Notch
/dudisguise config visibility.self-sees-disguise true
```

## Data Files

```text
plugins/DistortedUniversePlayerDisguise/data.yml
plugins/DistortedUniversePlayerDisguise/nicknames.yml
```

- `data.yml` stores skin disguises and cached Mojang profile data.
- `nicknames.yml` stores custom nicknames from `/dudisguise nick`.
- `/dudisguise reload` and `/dudisguise save` touch `config.yml`, `data.yml`, and `nicknames.yml`.

## Notes

- This plugin is experimental and version-sensitive because it rewrites ProtocolLib player-info packets.
- Disguise changes what clients see where packet rewriting supports it; it does not change real authentication, UUID, permissions, bans, logs, or server identity.
- Headless server smoke tests can verify plugin load, commands, persistence, and Mojang profile lookup, but final skin and nametag appearance should be checked with real Minecraft clients.
- The current ProtocolLib dev-build jar may require a newer Java runtime than Java `21`; use a runtime-compatible ProtocolLib jar.
- By default, the disguised player does not see their own disguise.
- Custom nicknames take precedence over a disguise's profile name. The disguise still supplies the skin.
- Death messages are rewritten so the kill feed shows the disguised or nicknamed name for victims and killers.
- `DistortedUniversePlayerNickname` is optional. If installed, this plugin refreshes it after disguise changes. While a disguise or built-in nickname is active, the standalone nickname plugin skips that player.
- Other chat, tab, scoreboard, nickname, or disguise plugins may override this plugin's output.

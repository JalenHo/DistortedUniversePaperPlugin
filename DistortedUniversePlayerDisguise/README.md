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

Main command:

```text
/dudisguise
```

Alias:

```text
/dudis
```

## Commands Reference

| Command | Description |
| --- | --- |
| `/dudisguise help` | Show command usage. Same as running `/dudisguise` with no arguments. |
| `/dudisguise status` | Show plugin settings plus counts of disguised and nicknamed players. |
| `/dudisguise status <player>` | Show one player's active disguise profile and custom nickname, if any. |
| `/dudisguise set <player> <minecraft-username>` | Look up a Mojang profile and disguise the player as that username and skin. Reuses cached profile data when available. |
| `/dudisguise clear <player>` | Remove a player's skin disguise. Any custom nickname remains active. |
| `/dudisguise nick <player> <nickname>` | Set a plain-text custom nickname for tab, chat, nametag, and kill-feed display. |
| `/dudisguise nick <player>` | Clear a player's custom nickname. |
| `/dudisguise list` | List all stored disguises and all stored custom nicknames. |
| `/dudisguise refresh` | Re-apply disguises and nicknames for all online players and refresh tracked ProtocolLib entities. |
| `/dudisguise cache-refresh <minecraft-username>` | Force a fresh Mojang profile lookup and update every disguise that uses that username. |
| `/dudisguise get <path>` | Read one setting path from the live in-memory config. |
| `/dudisguise config <path> <value...>` | Change one setting path live, validate it, save to `config.yml`, and refresh all online disguises. |
| `/dudisguise reload` | Reload `config.yml`, `data.yml`, and `nicknames.yml`, then refresh all online disguises. |
| `/dudisguise save` | Write the current live config, disguise data, and nickname data back to disk. |

Player targeting accepts:

- an online player name
- a name already stored in `data.yml` or `nicknames.yml`
- a name found in the server's offline-player cache

Minecraft usernames and nicknames must be `1` to `16` characters and contain only letters, numbers, or underscores. Disguise usernames must be at least `3` characters.

## Settings Reference

Boolean settings accept `true` / `false`, `on` / `off`, `yes` / `no`, `enable` / `disable`, and `1` / `0`.

| Path | Type | Default | Description |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Master switch for disguise and nickname application. When `false`, stored data remains on disk but nothing is applied. |
| `visibility.self-sees-disguise` | boolean | `false` | When `true`, the disguised player also sees their own disguise. When `false`, only other players see it. |
| `profile-lookup.cache-days` | integer | `7` | How long cached Mojang profile data is considered fresh. `0` disables expiry. Range: `0` to `365`. |
| `profile-lookup.refresh-expired-cache` | boolean | `true` | When `true`, expired cache entries trigger a new Mojang lookup on disguise set. When `false`, expired cache may still be reused. |
| `apply.chat-display-name` | boolean | `true` | Update Paper chat/display name APIs for disguised or nicknamed players. |
| `apply.tab-list-name` | boolean | `true` | Update the tab-list name for disguised or nicknamed players. |
| `apply.protocol-profile` | boolean | `true` | Rewrite ProtocolLib player-info packets so clients see the disguised skin and nametag profile. |

## Feature Reference

| Feature | Description |
| --- | --- |
| Skin disguise | Makes a player appear as another Minecraft account's username and skin to other clients. |
| Built-in nickname | Changes only the shown name while keeping the player's real skin. Stored separately from disguises. |
| Nickname precedence | If both a disguise and a custom nickname are set, the nickname is shown as the name and the disguise still supplies the skin. |
| Kill-feed rewrite | Death messages are rewritten so victims and killers show their disguised or nicknamed name in the kill feed. |
| Profile cache | Mojang profile lookups are cached in `data.yml` to reduce repeated API calls. |
| Entity refresh | Disguise changes trigger a tracked entity refresh so clients re-read the rewritten profile. |

## Examples

```text
/dudisguise set Steve Notch
/dudisguise clear Steve
/dudisguise nick Steve Traveler
/dudisguise nick Steve
/dudisguise cache-refresh Notch
/dudisguise config visibility.self-sees-disguise true
/dudisguise status Steve
/dudisguise list
```

## Data Files

```text
plugins/DistortedUniversePlayerDisguise/config.yml
plugins/DistortedUniversePlayerDisguise/data.yml
plugins/DistortedUniversePlayerDisguise/nicknames.yml
```

| File | Description |
| --- | --- |
| `config.yml` | Plugin settings. |
| `data.yml` | Skin disguises and cached Mojang profile data. |
| `nicknames.yml` | Custom nicknames from `/dudisguise nick`. |

`/dudisguise reload` and `/dudisguise save` touch all three files.

## Notes

- This plugin is experimental and version-sensitive because it rewrites ProtocolLib player-info packets.
- Disguise changes what clients see where packet rewriting supports it; it does not change real authentication, UUID, permissions, bans, logs, or server identity.
- Headless server smoke tests can verify plugin load, commands, persistence, and Mojang profile lookup, but final skin and nametag appearance should be checked with real Minecraft clients.
- The current ProtocolLib dev-build jar may require a newer Java runtime than Java `21`; use a runtime-compatible ProtocolLib jar.
- By default, the disguised player does not see their own disguise.
- Other chat, tab, scoreboard, nickname, or disguise plugins may override this plugin's output.

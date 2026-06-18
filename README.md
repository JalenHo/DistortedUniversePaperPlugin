# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

Released plugins for Minecraft/Paper `1.21.11` (`v0.9.2`):

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks.
- [DistortedUniverseImmortal](DistortedUniverseImmortal/) - per-player immortality with normal damage down to a configurable health floor.
- [DistortedUniversePlayerDisguise](DistortedUniversePlayerDisguise/) - experimental ProtocolLib-based player name and skin disguises. Includes `/dudisguise nick` for tab, chat, nametag, and kill-feed names.

Optional source module (not included in GitHub prerelease assets):

- [DistortedUniversePlayerNickname](DistortedUniversePlayerNickname/) - standalone nickname plugin with above-head display modes. Use this only if you need features beyond `/dudisguise nick`.

## Branches

- `Dev` is the active development branch.
- `1.21.11` tracks the same code as `Dev` for Minecraft/Paper `1.21.11` releases.
- `main` should be kept in sync with `Dev` for CI and default checkouts.

At the current `v0.9.2` prerelease line, `Dev` and `1.21.11` point to the same commit.

## Requirements

- Java `21`
- Paper target: Minecraft `1.21.11`
- `DistortedUniversePlayerDisguise` requires ProtocolLib at runtime. Local smoke testing used the GitHub ProtocolLib `5.4.0` release jar with Java `21` and Paper `1.21.11`; ProtocolLib warns that Minecraft `1.21.11` is not yet tested.

## Build

```powershell
.\gradlew.bat clean build
```

Build one released plugin:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
.\gradlew.bat :DistortedUniverseImmortal:clean :DistortedUniverseImmortal:build
.\gradlew.bat :DistortedUniversePlayerDisguise:clean :DistortedUniversePlayerDisguise:build
```

Build the optional nickname module:

```powershell
.\gradlew.bat :DistortedUniversePlayerNickname:clean :DistortedUniversePlayerNickname:build
```

## GitHub Prerelease

Run **Actions -> Prerelease Plugins -> Run workflow** to build and publish a GitHub prerelease for Minecraft/Paper `1.21.11`.

Default prerelease tag:

```text
v0.9.2
```

Prerelease assets:

```text
DistortedUniversePlayerEvent
DistortedUniverseImmortal
DistortedUniversePlayerDisguise
```

`DistortedUniversePlayerDisguise` includes a built-in nickname command (`/dudisguise nick`), so the standalone `DistortedUniversePlayerNickname` jar is not uploaded to the prerelease.

Released jar output paths:

```text
DistortedUniversePlayerEvent/build/libs/
DistortedUniverseImmortal/build/libs/
DistortedUniversePlayerDisguise/build/libs/
```

Optional module output:

```text
DistortedUniversePlayerNickname/build/libs/
```

## Repository Layout

```text
.
|-- DistortedUniversePlayerEvent/      # released
|-- DistortedUniverseImmortal/         # released
|-- DistortedUniversePlayerDisguise/   # released
|-- DistortedUniversePlayerNickname/   # optional source module
|-- gradle/
|-- gradlew
|-- gradlew.bat
|-- settings.gradle.kts
`-- build.gradle.kts
```

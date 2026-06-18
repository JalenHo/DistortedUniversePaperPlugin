# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks for Minecraft/Paper `1.21.11`.
- [DistortedUniverseImmortal](DistortedUniverseImmortal/) - per-player immortality with normal damage down to a configurable health floor for Minecraft/Paper `1.21.11`.
- [DistortedUniversePlayerNickname](DistortedUniversePlayerNickname/) - persistent player nicknames for chat, tab list, and visual above-head labels for Minecraft/Paper `1.21.11`.
- [DistortedUniversePlayerDisguise](DistortedUniversePlayerDisguise/) - experimental ProtocolLib-based player name and skin disguises for Minecraft/Paper `1.21.11`.

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

Build one plugin:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
.\gradlew.bat :DistortedUniverseImmortal:clean :DistortedUniverseImmortal:build
.\gradlew.bat :DistortedUniversePlayerNickname:clean :DistortedUniversePlayerNickname:build
.\gradlew.bat :DistortedUniversePlayerDisguise:clean :DistortedUniversePlayerDisguise:build
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

`DistortedUniversePlayerDisguise` includes a built-in nickname command (`/dudisguise nick`) for tab, chat, nametag, and kill-feed names, so the standalone `DistortedUniversePlayerNickname` jar is not uploaded to the prerelease. You can still build and install `DistortedUniversePlayerNickname` locally when you need its above-head display modes and validation settings.

The built jars will be under:

```text
DistortedUniversePlayerEvent/build/libs/
DistortedUniverseImmortal/build/libs/
DistortedUniversePlayerNickname/build/libs/
DistortedUniversePlayerDisguise/build/libs/
```

## Repository Layout

```text
.
|-- DistortedUniversePlayerEvent/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- DistortedUniverseImmortal/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- DistortedUniversePlayerNickname/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- DistortedUniversePlayerDisguise/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- gradle/
|-- gradlew
|-- gradlew.bat
|-- settings.gradle.kts
`-- build.gradle.kts
```

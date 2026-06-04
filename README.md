# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks for Minecraft/Paper `1.21.11`.
- [DistortedUniverseImmortal](DistortedUniverseImmortal/) - per-player immortality with normal damage down to a configurable health floor for Minecraft/Paper `1.21.11`.
- [DistortedUniversePlayerNickname](DistortedUniversePlayerNickname/) - persistent player nicknames for chat, tab list, and visual above-head labels for Minecraft/Paper `1.21.11`.
- [DistortedUniversePlayerDisguise](DistortedUniversePlayerDisguise/) - experimental ProtocolLib-based player name and skin disguises for Minecraft/Paper `1.21.11`.

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

Run **Actions -> Prerelease Plugins -> Run workflow** to build all plugins and publish them as a GitHub prerelease for Minecraft/Paper `1.21.11`.

Default prerelease tag:

```text
v0.9.2
```

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

# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks for Minecraft/Paper `1.21.11`.
- [DistortedUniverseImmortal](DistortedUniverseImmortal/) - per-player immortality with normal damage down to a configurable health floor for Minecraft/Paper `1.21.11`.

## Requirements

- Java `21`
- Paper target: Minecraft `1.21.11`

## Build

```powershell
.\gradlew.bat clean build
```

Build one plugin:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
.\gradlew.bat :DistortedUniverseImmortal:clean :DistortedUniverseImmortal:build
```

The built jars will be under:

```text
DistortedUniversePlayerEvent/build/libs/
DistortedUniverseImmortal/build/libs/
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
|-- gradle/
|-- gradlew
|-- gradlew.bat
|-- settings.gradle.kts
`-- build.gradle.kts
```

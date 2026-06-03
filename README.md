# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks for Minecraft/Paper `26.1.2`.

## Requirements

- Java `25`
- Paper target: Minecraft `26.1.2`

## Build

```powershell
.\gradlew.bat clean build
```

Build one plugin:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
```

The built jar will be under:

```text
DistortedUniversePlayerEvent/build/libs/
```

## Repository Layout

```text
.
├── DistortedUniversePlayerEvent/
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
└── build.gradle.kts
```

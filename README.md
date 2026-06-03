# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks for Minecraft/Paper `26.1.2`.

## Build All Plugins

```powershell
.\gradlew.bat clean build
```

## Build One Plugin

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

Local Paper server runtimes, downloaded server jars, worlds, logs, and generated build output should stay out of this repository.

## GitHub Actions

This repo includes a build workflow that runs on pushes, pull requests, and manual dispatches. It builds and tests all plugins, then uploads plugin jars as workflow artifacts.

Recommended release flow:

1. Push source code to GitHub.
2. Let GitHub Actions build and test it.
3. Download workflow artifacts for testing builds.
4. Attach jars to GitHub Releases for stable public versions.

Do not commit built jars or local server files into the repository.

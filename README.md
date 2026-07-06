# DistortedUniverse Paper Plugins

Monorepo for DistortedUniverse PaperMC plugins.

## Plugins

Released plugins for Minecraft/Paper `1.21.4` (`v0.9.3`):

- [DistortedUniversePlayerEvent](DistortedUniversePlayerEvent/) - distance-scoped player event messages, death sounds, and death kicks. See its README for full command, module, and setting reference.
- [DistortedUniverseImmortal](DistortedUniverseImmortal/) - per-player immortality with normal damage down to a configurable health floor. See its README for full command and setting reference.
- [DistortedUniversePlayerDisguise](DistortedUniversePlayerDisguise/) - experimental ProtocolLib-based player name and skin disguises. Includes `/dudisguise nick` for tab, chat, nametag, and kill-feed names. See its README for full command and setting reference.
- [DistortedUniverseFakePlayer](DistortedUniverseFakePlayer/) - fake player NPCs with custom skins, preset names, wandering AI, and configurable chat responses.
- [DistortedUniverseTeam](DistortedUniverseTeam/) - team management with colored glow outlines, GUI, and AutoKit integration.

## Branches

- `Dev` is the active development branch.
- `1.21.4` tracks the same code as `Dev` for Minecraft/Paper `1.21.4` releases.
- `main` should be kept in sync with `Dev` for CI and default checkouts.

At the current `v0.9.3` prerelease line, `Dev` and `1.21.4` point to the same commit.

## Requirements

- Java `21`
- Paper target: Minecraft `1.21.4`
- `DistortedUniversePlayerDisguise` requires ProtocolLib at runtime. Local smoke testing used the GitHub ProtocolLib `5.4.0` release jar with Java `21` and Paper `1.21.4`; ProtocolLib warns that Minecraft `1.21.4` is not yet tested.

## Build

```powershell
.\gradlew.bat clean build
```

Build one plugin:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
.\gradlew.bat :DistortedUniverseImmortal:clean :DistortedUniverseImmortal:build
.\gradlew.bat :DistortedUniversePlayerDisguise:clean :DistortedUniversePlayerDisguise:build
```

## GitHub Prerelease

Run **Actions -> Prerelease Plugins -> Run workflow** to build and publish a GitHub prerelease for Minecraft/Paper `1.21.4`.

Default prerelease tag:

```text
v0.9.3
```

Prerelease assets:

```text
DistortedUniversePlayerEvent
DistortedUniverseImmortal
DistortedUniversePlayerDisguise
DistortedUniverseFakePlayer
DistortedUniverseTeam
```

The built jars will be under:

```text
DistortedUniversePlayerEvent/build/libs/
DistortedUniverseImmortal/build/libs/
DistortedUniversePlayerDisguise/build/libs/
DistortedUniverseFakePlayer/build/libs/
DistortedUniverseTeam/build/libs/
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
|-- DistortedUniversePlayerDisguise/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- DistortedUniverseFakePlayer/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- DistortedUniverseTeam/
|   |-- src/
|   |-- build.gradle.kts
|   `-- README.md
|-- gradle/
|-- gradlew
|-- gradlew.bat
|-- settings.gradle.kts
`-- build.gradle.kts
```

# DistortedUniverseFakePlayer

Server-side fake players spawned through Paper NMS (`ServerPlayer` + fake network connection). Bots use real player nametags, skins, knockback, and death handling while staying command-driven.

## Features

- **NMS Fake Players**: Spawns real `Player` entities (not Mannequins)
- **Spawn / Despawn**: Create and remove fake players at your location
- **Command Walking**: Wander nearby or walk to coordinates with collision checks
- **Join/Leave Messages**: Integrates with DistortedUniversePlayerEvent when installed
- **Persistence**: Bot data survives server restarts via `fakeplayers.yml`

## Commands

| Command | Description |
|---------|-------------|
| `/dfp spawn <name>` | Spawn a fake player at your location |
| `/dfp despawn <name\|all>` | Despawn one or all fake players (`remove` alias supported) |
| `/dfp move <name> wander [radius]` | Start wandering within radius (default from config) |
| `/dfp move <name> <x> <y> <z>` | Walk to coordinates |
| `/dfp move <name> stop` | Stop movement |
| `/dfp list` | List all fake players and status |
| `/dfp reload` | Reload configuration |

## Configuration

```yaml
config-version: 4
enabled: true

display:
  tab-list: false  # reserved for future tab-list control

movement:
  speed: 0.2
  arrival-distance: 1.5
  tick-interval: 2
  wander-radius: 10

behavior:
  invulnerable: true
  knockback-when-invulnerable: true
  gravity: true
  immovable: false

messages:
  use-player-event-settings: true

skins:
  folder: "skins"
  default: "default"
```

## Build

This module uses **paperweight userdev** for NMS access and targets **Paper 26.2** with Mojang mappings.

```bash
./gradlew :DistortedUniverseFakePlayer:build
```

## Requirements

- Paper 26.2
- Java 25

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `distorteduniverse.fakeplayer.admin` | Allows managing fake players | OP |

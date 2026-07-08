# DistortedUniverseFakePlayer

Server-side fake player bots using Paper Mannequin entities. Bots look like normal players (no extra nametags) and can walk around via commands.

## Features

- **Spawn / Despawn**: Create and remove fake players at your location
- **Command Walking**: Wander nearby or walk to coordinates
- **Live Settings**: Change plugin behavior and fake player names/skins without restarting
- **Entity-like Reactions**: Fake players receive knockback, play death behavior, and float in water
- **Default Skin**: Uses `skins/default.json` for player appearance
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
| `/dfp config list` | Show live plugin settings |
| `/dfp config get <setting>` | Show one setting value |
| `/dfp config set <setting> <value>` | Update and apply a setting immediately |
| `/dfp set <name> name <newName>` | Rename a fake player immediately |
| `/dfp set <name> skin <skin>` | Change a fake player's skin immediately |
| `/dfp set <name> immortal <true\|false>` | Toggle global invulnerability immediately (`immortal` alias) |

Live config paths:

- `enabled`
- `movement.speed`
- `movement.arrival-distance`
- `movement.tick-interval`
- `movement.wander-radius`
- `behavior.invulnerable` (`behavior.immortal` alias)
- `behavior.gravity`
- `behavior.immovable`
- `skins.folder`
- `skins.default`

## Configuration

```yaml
config-version: 2
enabled: true

movement:
  speed: 0.2
  arrival-distance: 1.5
  tick-interval: 2
  wander-radius: 10

behavior:
  invulnerable: false
  gravity: true
  immovable: false

skins:
  folder: "skins"
  default: "default"
```

## Skins

Place skin JSON files in the `skins/` folder (relative to plugin directory). Each file should be named `<skin-name>.json` and contain:

```json
{
    "name": "my-skin",
    "value": "base64-encoded-texture-data",
    "signature": "base64-encoded-signature"
}
```

## Requirements

- Paper API 26.2 (Minecraft 26.2)
- Java 25

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `distorteduniverse.fakeplayer.admin` | Allows managing fake players | OP |

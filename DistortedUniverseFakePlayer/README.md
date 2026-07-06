# DistortedUniverseFakePlayer

Fake player NPCs with custom skins, names, wandering AI, and configurable chat responses.

## Features

- **Custom Skins**: Load skins from JSON files in the `skins/` folder
- **Preset Names**: Configure a list of names for random spawning
- **Wandering AI**: NPCs can wander within a configurable radius
- **Chat Responses**: Configurable responses to player chat messages
- **Join/Leave Messages**: Customizable notification messages
- **Damage Prevention**: Fake players are invulnerable by default

## Commands

| Command | Description |
|---------|-------------|
| `/dfp spawn <name>` | Spawn a fake player with a specific name |
| `/dfp spawn-random` | Spawn a fake player with a random preset name |
| `/dfp remove <name\|all>` | Remove one or all fake players |
| `/dfp list` | List all fake players and their status |
| `/dfp skin <player> <skin>` | Change a fake player's skin |
| `/dfp skin-list` | List available skins |
| `/dfp move <name\|all> [x y z]` | Move or start wandering |
| `/dfp status` | Show plugin status |
| `/dfp reload` | Reload configuration |
| `/dfp save` | Save all data |

## Configuration

```yaml
config-version: 1
enabled: true

names:
  - "Steve"
  - "Alex"
  - "Herobrine"
  - "Notch"

wandering:
  default-radius: 10
  tick-interval: 40
  use-pathfinding: true

messages:
  join:
    enabled: true
    template: "<yellow>{player} joined</yellow>"
  leave:
    enabled: true
    template: "<yellow>{player} left</yellow>"
  death:
    enabled: true
    template: "<red>{player} died</red>"

chat:
  enabled: true
  responses:
    "hello": "<gray>{player}: Hello!"
    "hi": "<gray>{player}: Hey there!"
    "help": "<gray>{player}: I'm just a bot!"

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

- Paper API 26.2 (Minecraft 1.21.4)
- ProtocolLib at runtime
- Java 21

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `distorteduniverse.fakeplayer.admin` | Allows managing fake players | OP |

# DistortedUniverseTeam

Team management with colored glow outlines, paginated admin GUI, and AutoKit integration.

## Features

- **Team Creation**: Create teams with custom names, colors, and max sizes
- **Colored Glow**: Glow outlines in team colors (Mojang glow feature)
- **Admin GUI**: Inventory-based paginated GUI for team management
- **AutoKit Integration**: Automatically give kits when joining a team
- **Player Self-Join**: Players can join teams via command
- **Friendly Fire Toggle**: Server-wide friendly fire setting
- **Admin Glow Override**: Permission to see all glow colors

## Commands

### Admin Commands

| Command | Description |
|---------|-------------|
| `/duteam list` | List all teams |
| `/duteam info <team>` | Show team details |
| `/duteam create <id> <name> [color] [max]` | Create a team |
| `/duteam delete <team>` | Delete a team |
| `/duteam setcolor <team> <color>` | Change team color |
| `/duteam setglow <team> <on\|off>` | Toggle team glow |
| `/duteam setmaxsize <team> <size>` | Set max team size |
| `/duteam setkit <team> <kit\|none>` | Set AutoKit for team |
| `/duteam gui` | Open admin GUI |
| `/duteam kick <team> <player>` | Kick player from team |
| `/duteam friendlyfire [on\|off]` | Toggle friendly fire |
| `/duteam reload` | Reload configuration |
| `/duteam save` | Save all data |

### Player Commands

| Command | Description |
|---------|-------------|
| `/duteam join <team>` | Join a team |
| `/duteam leave` | Leave current team |

## Configuration

```yaml
config-version: 1
enabled: true
friendly-fire: false

glow:
  enabled-by-default: true
  default-color: white

default-color: white
default-max-size: -1

autokit:
  enabled: true
```

## Team Colors

Available colors: `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `dark_gray`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white`

## Data Storage

Teams and player data are stored in:
- `teams.yml` - Team definitions and member lists
- `players.yml` - Player-to-team associations

## Requirements

- Paper API 26.2 (Minecraft 1.21.4)
- Java 21
- Optional: AutoKit plugin for kit integration

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `distorteduniverse.team.admin` | Admin commands | OP |
| `distorteduniverse.team.join` | Join teams via command | All |
| `duteam.admin.glow` | See all glow colors | OP |

## AutoKit Integration

When AutoKit is installed and configured, teams can be linked to specific kits. Players joining the team will automatically receive the linked kit.

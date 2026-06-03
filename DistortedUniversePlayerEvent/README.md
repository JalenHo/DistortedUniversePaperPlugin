# DistortedUniversePlayerEvent

A PaperMC server-side plugin for Minecraft `1.21.11`.

The plugin scopes player death sounds, death messages, join messages, leave messages, and death-kick quit notices by distance. All feature modules can be changed live in game by an operator without restarting the server.

## Requirements

- Minecraft / Paper `1.21.11`
- Java `21`
- Paper API `1.21.11`

## Build

Use the included Gradle wrapper:

```powershell
.\gradlew.bat :DistortedUniversePlayerEvent:clean :DistortedUniversePlayerEvent:build
```

The plugin jar is created at:

```text
DistortedUniversePlayerEvent/build/libs/DistortedUniversePlayerEvent-0.9.0.jar
```

## Install

1. Build the jar.
2. Copy `DistortedUniversePlayerEvent/build/libs/DistortedUniversePlayerEvent-0.9.0.jar` into your Paper server's `plugins` folder.
3. Start or restart the server once.
4. Configure live in game with `/duplayerevent`.

The admin permission is:

```text
distorteduniverse.playerevent.admin
```

It defaults to server operators.

## Command

Main command:

```text
/duplayerevent
```

Aliases:

```text
/dupe
```

Common commands:

```text
/duplayerevent status
/duplayerevent enable <module>
/duplayerevent disable <module>
/duplayerevent get <path>
/duplayerevent set <path> <value...>
/duplayerevent reload
/duplayerevent save
/duplayerevent test-sound [player]
```

Modules:

```text
death-sound
death-message
join-message
leave-message
death-kick
```

## Settings

Death sound:

```text
death-sound.enabled
death-sound.radius
death-sound.sound
death-sound.category
death-sound.volume
death-sound.pitch
death-sound.suppress-vanilla
```

Death message:

```text
death-message.enabled
death-message.radius
death-message.respect-gamerule
death-message.template
```

Join message:

```text
join-message.enabled
join-message.radius
join-message.template
```

Leave message:

```text
leave-message.enabled
leave-message.radius
leave-message.template
```

Death kick:

```text
death-kick.enabled
death-kick.delay-ticks
death-kick.kick-message
death-kick.show-leave-message
death-kick.leave-radius
death-kick.leave-template
```

## Examples

Disable death kicking:

```text
/duplayerevent disable death-kick
```

Change death sound radius:

```text
/duplayerevent set death-sound.radius 80
```

Use a vanilla sound:

```text
/duplayerevent set death-sound.sound minecraft:entity.lightning_bolt.thunder
```

Set max death sound volume:

```text
/duplayerevent set death-sound.volume 10
```

Change join message:

```text
/duplayerevent set join-message.template <green><player_name></green> joined nearby
```

Change leave message:

```text
/duplayerevent set leave-message.template <yellow><player_name></yellow> left nearby
```

Turn on a distance-scoped quit notice after death kick:

```text
/duplayerevent set death-kick.show-leave-message true
```

## Message Placeholders

Message templates use MiniMessage and support:

```text
<player>
<player_name>
<display_name>
<world>
<x>
<y>
<z>
<death_message>
<join_message>
<quit_message>
```

MiniMessage color and style tags are also supported:

```text
<black>
<dark_blue>
<dark_green>
<dark_aqua>
<dark_red>
<dark_purple>
<gold>
<gray>
<dark_gray>
<blue>
<green>
<aqua>
<red>
<light_purple>
<yellow>
<white>
<bold>
<italic>
<underlined>
<strikethrough>
<reset>
```

Examples:

```text
/duplayerevent set join-message.template <green><player_name></green> joined nearby
/duplayerevent set leave-message.template <gray><italic><player_name> left nearby</italic></gray>
/duplayerevent set death-message.template <red><bold><player_name></bold></red> died at <yellow><x> <y> <z></yellow>
```

Default templates preserve vanilla-looking messages:

```text
death-message.template = <death_message>
join-message.template = <join_message>
leave-message.template = <quit_message>
```

## Sound Notes

Vanilla sound keys work server-side:

```text
minecraft:entity.player.death
minecraft:entity.wither.death
minecraft:entity.lightning_bolt.thunder
```

Custom non-vanilla sound keys are supported, but clients must have a resource pack that defines the sound. A server-side plugin cannot send brand-new audio files by itself.

## Default Behavior

- Death sounds are sent only to same-world players within `death-sound.radius`.
- Death messages are sent only to same-world players within `death-message.radius`.
- Join messages are sent only to same-world players within `join-message.radius`.
- Leave messages are sent only to same-world players within `leave-message.radius`.
- Players are kicked shortly after death when `death-kick.enabled` is true.
- Death-kick leave notices are disabled by default.

## Local Testing

This repository does not include a Paper server jar or generated server world files. Keep local test servers outside the repository and copy the built plugin jar into that server's `plugins` folder.

Paper downloads: <https://papermc.io/downloads/paper>

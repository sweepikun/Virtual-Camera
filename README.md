# Virtual Camera Plugin

A Minecraft plugin that provides virtual camera functionality for players to create cinematic experiences.

## Features

- Create and manage multiple camera presets
- Smooth transitions between camera positions
- Camera sequences for automated cinematic shots
- Session management for player camera states
- Customizable camera types and transition effects

## Installation

1. Download the latest build from [Releases](#)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server

## Configuration

Edit `config.yml` to customize plugin behavior:

```yaml
# Example configuration
camera:
  default-transition: LINEAR
  max-distance: 50
```

## Usage

Basic commands:
- `/camera create <name>` - Create a new camera preset
- `/camera list` - List available camera presets
- `/camera start <name>` - Start a camera session
- `/camera stop` - Stop current camera session

## Development

### Building

```bash
./gradlew build
```

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── cn/popcraft/
│   │       ├── command/      # Command implementations
│   │       ├── listener/     # Event listeners
│   │       ├── manager/      # Core functionality managers
│   │       ├── model/        # Data models
│   │       └── session/      # Session management
│   └── resources/            # Configuration files
```

### Dependencies

- Spigot/Bukkit API
- Java 17+

## License

This project is licensed under the [GPL-V3 License](LICENSE).
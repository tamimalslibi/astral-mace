# Astral Mace Plugin

A Minecraft plugin for Paper/Spigot 1.21 that adds the Astral Mace - a powerful weapon that launches players vertically into the air.

## Features

✅ **Vertical Launch**: Launches hit players straight up, then brings them straight back down  
✅ **15-Second Cooldown**: Per-player cooldown system  
✅ **OP Only**: Only server operators can obtain the Astral Mace  
✅ **Blue Bold Name**: "ASTRAL MACE" in bold blue text  
✅ **Enchantments**: Density V, Wind Burst II, Mending, Unbreaking  
✅ **Enchanted Glow**: From the enchantments  
✅ **Custom Lore**: Descriptive lore matching modern plugin style  

## Installation

1. Download or compile the plugin `.jar` file
2. Place it in your server's `plugins/` folder
3. Restart your server
4. Use `/astralmace` as an OP to obtain the mace

## Commands

- `/astralmace` - Get an Astral Mace (requires OP)

## Permissions

- `astralmace.get` - Allows getting the Astral Mace (default: op)

## Building from Source

### Requirements
- Java 21 or higher
- Maven 3.6+

### Steps

```bash
# Clone the repository
git clone https://github.com/yourusername/AstralMacePlugin.git
cd AstralMacePlugin

# Compile with Maven
mvn clean package

# The compiled .jar will be in target/AstralMacePlugin-1.0.jar
```

## How It Works

1. Operator uses `/astralmace` to get the weapon
2. Hit another player with the Astral Mace
3. Target is launched straight upward with no horizontal movement
4. After reaching peak height, they're brought straight back down
5. 15-second cooldown activates immediately
6. Cooldown is tracked separately for each player

## Technical Details

- **Server**: Paper/Spigot 1.21+
- **API Version**: 1.21
- **Java Version**: 21
- **Launch Duration**: ~4 seconds total (1.5s up + 2.5s down)
- **Horizontal Lock**: X and Z coordinates stay fixed throughout

## License

This plugin is provided as-is for use on Minecraft servers.

## Support

For issues or questions, please open an issue on GitHub.

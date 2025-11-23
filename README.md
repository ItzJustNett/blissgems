# BlissGems

A Minecraft gem system plugin for Paper/Purpur 1.21+ servers. Players can equip gems in their offhand to gain passive abilities and activate special powers.

## Requirements

- **Java 17+**
- **Paper/Purpur 1.21+** (tested on Purpur 1.21.10)
- **Oraxen 1.195.1+** (for custom items)
- **PlaceholderAPI 2.11.6+** (optional, for placeholders)

## Installation

### 1. Install Dependencies

Download and install the following plugins to your `plugins` folder:

- [Oraxen](https://www.spigotmc.org/resources/oraxen.72448/) - Required for custom gem items
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) - Optional

### 2. Install BlissGems

1. Build the plugin:
   ```bash
   mvn clean package
   ```
2. Copy `target/BlissGems-1.0.1.jar` to your server's `plugins` folder
3. Start the server

### 3. Configure Oraxen Items

Create the following items in your Oraxen configuration (`plugins/Oraxen/items/`):

#### Gem Items (Required)

Each gem type needs Tier 1 and Tier 2 versions:

```yaml
# Example: plugins/Oraxen/items/gems.yml

# Tier 1 Gems
astra_gem_t1:
  displayname: "<light_purple>Astra Gem"
  material: AMETHYST_SHARD

fire_gem_t1:
  displayname: "<gold>Fire Gem"
  material: BLAZE_POWDER

flux_gem_t1:
  displayname: "<aqua>Flux Gem"
  material: PRISMARINE_CRYSTALS

life_gem_t1:
  displayname: "<green>Life Gem"
  material: EMERALD

puff_gem_t1:
  displayname: "<white>Puff Gem"
  material: FEATHER

speed_gem_t1:
  displayname: "<yellow>Speed Gem"
  material: SUGAR

strength_gem_t1:
  displayname: "<red>Strength Gem"
  material: NETHER_STAR

wealth_gem_t1:
  displayname: "<gold>Wealth Gem"
  material: GOLD_NUGGET

# Tier 2 Gems (same pattern with _t2 suffix)
astra_gem_t2:
  displayname: "<light_purple><bold>Astra Gem II"
  material: AMETHYST_SHARD

# ... repeat for all gem types
```

#### Additional Items

```yaml
# Energy Bottle - restores 1 energy when used
energy_bottle:
  displayname: "<aqua>Energy Bottle"
  material: EXPERIENCE_BOTTLE

# Gem Trader - randomly changes your gem type
gem_trader:
  displayname: "<gold>Gem Trader"
  material: ENDER_EYE

# Gem Upgraders - upgrade Tier 1 to Tier 2
astra_gem_upgrader:
  displayname: "<light_purple>Astra Upgrader"
  material: END_CRYSTAL

fire_gem_upgrader:
  displayname: "<gold>Fire Upgrader"
  material: END_CRYSTAL

# ... repeat for all gem types
```

### 4. Reload Oraxen

After adding items, reload Oraxen:
```
/oraxen reload items
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/bliss give <player> <gem_type> [tier]` | Give a gem to a player | `blissgems.admin` |
| `/bliss energy <player> <set/add/remove> <amount>` | Manage player energy | `blissgems.admin` |
| `/bliss withdraw` | Extract 1 energy into a bottle | None |
| `/bliss info` | Show your gem info | None |
| `/bliss reload` | Reload configuration | `blissgems.admin` |

### Gem Types
- `astra`, `fire`, `flux`, `life`, `puff`, `speed`, `strength`, `wealth`

### Examples
```
/bliss give Steve fire 1      # Give Tier 1 Fire Gem
/bliss give Steve astra 2     # Give Tier 2 Astra Gem
/bliss energy Steve set 10    # Set energy to max
/bliss energy Steve add 5     # Add 5 energy
```

## Gem Abilities

### Usage
- **Hold gem in offhand** for passive abilities
- **Hold gem in main hand + Right-click** for active abilities
- **Shift + Right-click** for Tier 2 secondary abilities

### Gem Overview

| Gem | Passive | Primary Ability (RMB) | Secondary (Shift+RMB, T2) |
|-----|---------|----------------------|---------------------------|
| **Astra** | 15% phase through attacks | Summon homing daggers | - |
| **Fire** | Auto-smelt ores | Charged Fireball | Place healing Campfire |
| **Flux** | Shocking arrows (electric damage) | Freeze ground | Chain lightning |
| **Life** | 3x damage to undead, 2x saturation | Heart Drainer | Circle of Life |
| **Puff** | No fall damage, double jump | Dash forward | Breezy Bash (slam) |
| **Speed** | Soul sand immunity | Sedative (slow enemies) | Speed Storm |
| **Strength** | - | Bloodthorns (AoE damage) | Chad Strength (Strength II) |
| **Wealth** | - | Durability Chip | Pockets, Rich Rush, etc. |

### Auto-Enchantments (Tier 2 Only)

When holding tools with a Tier 2 gem in offhand:

| Gem | Enchantments |
|-----|--------------|
| Fire | Flame (bows), Fire Aspect (swords) |
| Puff | Feather Falling IV (boots) |
| Speed | Efficiency V (tools) |
| Strength | Sharpness V (swords, axes) |
| Wealth | Fortune III, Looting III, Mending |

## Energy System

- **Max Energy:** 10
- **Starting Energy:** 5 (for new gems)
- **Gain:** +1 on player kill
- **Lose:** -1 on death

### Energy States

| Energy | State | Effect |
|--------|-------|--------|
| 2-10 | Pristine | Full functionality |
| 1 | Ruined | Passives disabled |
| 0 | Broken | Abilities disabled |

## Configuration

All settings are in `plugins/BlissGems/config.yml`:

```yaml
# Energy System
energy:
  gain-on-kill: 1
  loss-on-death: 1
  max-energy: 10
  starting-energy: 5

# Ability Cooldowns (seconds)
abilities:
  cooldowns:
    fire-fireball: 60
    fire-campfire: 60
    # ... etc

# Ability Damage
  damage:
    fire-fireball: 6.0
    fire-campfire: 2.0
    # ... etc
```

## Permissions

| Permission | Description |
|------------|-------------|
| `blissgems.admin` | Access to admin commands (give, energy, reload) |

## Building from Source

```bash
# Clone repository
git clone <repository-url>
cd BlissGems_src

# Build
mvn clean package

# Output: target/BlissGems-1.0.1.jar
```

## Troubleshooting

### Common Issues

1. **"Cannot find symbol: ENTITY_TYPES_UNDEAD"**
   - Using outdated API. Plugin requires Paper 1.21+

2. **Gems not working**
   - Ensure Oraxen items are correctly named (e.g., `fire_gem_t1`, `fire_gem_t2`)
   - Check player has energy > 0

3. **Auto-enchant not working**
   - Requires Tier 2 gem
   - Check `auto-enchant.enabled: true` in config
   - Player must have passives active (energy > 1)

### Support

Report issues at the repository issue tracker.

## License

Private - Bliss SMP

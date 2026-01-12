# BlissGems v4.1 - Codebase Index

**Generated:** 2026-01-12
**Version:** 2.6.0
**Author:** magicc

---

## 1. Overall Project Structure

### Root Directory: `/home/magicc/projects/serv/BlissGemsV4.1`

#### Main Directories:

- **`src/main/java/dev/xoperr/blissgems/`** - Core plugin source code (480KB)
  - `abilities/` - Gem-specific ability implementations (8 classes)
  - `commands/` - Command handlers
  - `listeners/` - Event listeners (10 classes)
  - `managers/` - System managers (14 classes)
  - `utils/` - Utility classes and enums

- **`src/main/resources/`** - Plugin resources
  - `config.yml` - Default configuration template
  - `plugin.yml` - Plugin metadata and commands

- **`BlissGems/`** - Minecraft resource pack (Tier 1 textures)
  - `assets/` - Resource pack assets
  - `pack.mcmeta` - Resource pack metadata (format 34, MC 1.21+)

- **`BlissGems-remastered/`** - Alternative resource pack (remastered textures)

- **`server/`** - Test/development Minecraft server (ignored in git)
  - `plugins/` - Server plugins directory
  - `world/`, `world_nether/`, `world_the_end/` - World data

- **`target/`** - Maven build output (ignored in git)

- **`researches/`** - Documentation and research directory

- **`.github/`** - GitHub configuration
  - `appmod/code-migration/` - Migration tools

---

## 2. Key Files and Their Contents

### Core Plugin Files:

**`/src/main/java/dev/xoperr/blissgems/BlissGems.java`**
- Main plugin class extending JavaPlugin
- Initializes all managers and ability handlers
- Registers listeners and commands
- Version: 2.6.0
- Entry point: `onEnable()`, `onDisable()`, `onLoad()`

**`/config.yml`** (Root configuration)
- Energy system settings (gain/loss, max energy: 10)
- 8 gem types with enable/disable toggles
- Passive effect configurations
- Ability cooldowns and damage values
- Auto-enchanting settings (Tier 2 only)
- Upgrader, Trader, Energy Bottle systems
- Repair Kit/Pedestal mechanics
- All messages and formatting

**`/pom.xml`**
- Maven project configuration
- Group: dev.xoperr
- Artifact: BlissGems
- Version: 2.6.0
- Java 17
- Dependencies: Paper API 1.21, PlaceholderAPI 2.11.6
- ProGuard obfuscation plugin configured
- Soft dependencies: Oraxen (legacy), PlaceholderAPI

**`/src/main/resources/plugin.yml`**
- Plugin metadata
- Commands: `/bliss` (aliases: `/gems`, `/bg`)
- Permissions system (admin, user, specific abilities)
- API version: 1.21
- Author: magicc
- Website: https://mc.obriy.fun

**`/LICENSE`**
- All Rights Reserved
- Copyright 2025 ItzJustNett
- Proprietary software

### Utility Classes:

**`GemType.java`** - Enum defining 8 gem types:
- ASTRA (Light Purple) - "Phase through attacks and teleport with astral daggers"
- FIRE (Red) - "Burn your enemies with charged fireballs and cozy campfires"
- FLUX (Aqua) - "Stun enemies and unleash electric power"
- LIFE (Green) - "Heal yourself and drain the life from enemies"
- PUFF (White) - "Defy gravity with double jumps and immunity to fall damage"
- SPEED (Yellow) - "Move faster and sedate your foes"
- STRENGTH (Dark Red) - "Empower your attacks and weaken your enemies"
- WEALTH (Gold) - "Find fortune and unlock hidden riches"

**`EnergyState.java`** - Enum defining energy levels (0-10):
- BROKEN (0) - No abilities or passives
- RUINED (1) - Abilities disabled, passives disabled
- SHATTERED (2) - Abilities enabled, passives disabled
- CRACKED (3-4) - Normal state
- PRISTINE (5) - Starting state
- PRISTINE_PLUS_1 through PRISTINE_PLUS_5 (6-10) - Enhanced states

**`CustomItemManager.java`** (534 lines)
- Custom item system replacing Oraxen
- Uses PDC (Persistent Data Container) + Custom Model Data
- Material: ECHO_SHARD for all gems
- Custom model data ranges: 1001-1008 (T1), 2001-2008 (T2)
- Defines all gem lore, abilities, and visual properties
- Special items: gem_fragment, gem_trader, gem_upgrader, repair_kit, revive_beacon, energy_bottle

**`ConfigManager.java`**
- Configuration management with auto-repair functionality
- Version tracking
- Backward compatibility with missing keys
- Automatic backup creation

---

## 3. Main Features and Functionality

### Core Systems:

**Energy System**
- 0-10 energy levels affecting gem power
- Gain 1 energy on player kill
- Lose 1 energy on death
- Starting energy: 10 (Pristine)
- Below 2 energy: abilities disabled
- Below 2 energy: passives disabled
- Energy affects gem texture/appearance

**Gem System**
- Single gem per player (enforced)
- 8 unique gem types with distinct abilities
- 2 tiers: Basic (T1) and Enhanced (T2)
- Gems stored in inventory, must be in offhand for passives
- Gem-specific passives and active abilities
- Right-click or command-based ability activation

**Ability System**
- Individual cooldowns per ability (1-540 seconds)
- Global 1-second cooldown between abilities
- Energy requirement check
- Cooldown persistence across server restarts
- Cooldown display in action bar
- Tier-based ability unlocks (T2 gets additional abilities)

**Passive Effects System**
- Automatic effects when gem is in offhand
- Disabled when energy too low (Ruined/Broken state)
- 20-tick (1 second) update interval
- Tier-specific enhancements

### Advanced Features:

**Trusted Players System**
- Players can trust others to prevent friendly fire
- Trust list persisted to disk
- Prevents gem abilities from harming trusted players
- Self-trust always enabled

**Repair Kit / Pedestal System**
- Beacon-based energy restoration
- Restores 1 energy per second
- 10-block heal radius
- Maximum 10 total energy per kit
- Prioritizes lowest energy players
- Visual particle effects

**Revive Beacon System**
- Placeable beacon that allows respawn at location
- Duration and range configurable
- Tracked per player
- Prevents death in range

**Soul System (Astra Gem)**
- Soul Absorption: Heal on kills (2.5 hearts for mobs, 5 hearts for players)
- Soul Capture: Capture up to 2 mobs
- Soul Release: Spawn captured mobs

**Flow State System (Flux Gem)**
- Repeated actions become faster
- 5 flow levels
- 3-second timeout between actions
- Tracks: block breaking, arrow shooting, attacking, sprinting, jumping

**Critical Hit System (Strength Gem)**
- Charges special attack from critical hits
- T1: Every 8 crits = 2x damage
- T2: Every 3 crits = 2x damage
- Visual feedback on charged attack

**Auto-Enchanting (Tier 2 Only)**
- Automatic enchantments when holding appropriate tools
- Fire: Flame, Fire Aspect
- Puff: Power, Punch, Feather Falling
- Speed: Efficiency
- Strength: Sharpness
- Wealth: Fortune, Looting, Mending

**Recipe/Crafting System**
- Custom crafting recipes using vanilla materials
- Gem Fragment: Base crafting ingredient
- Gem Trader: Trade between gem types
- Gem Upgrader: Upgrade T1 to T2
- Repair Kit: Energy restoration item
- Revive Beacon: Respawn point item
- Energy Bottle: Portable energy storage

**GUI System**
- Main menu accessible via `/bliss` command
- Gem info display
- Energy status with visual bar
- Settings panel
- Interactive inventory interface

---

## 4. Technology Stack

### Core Technologies:
- **Language:** Java 17
- **Build Tool:** Maven 3.x
- **Server Platform:** Paper/Spigot 1.21 (Paper API)
- **Plugin API:** Bukkit/Spigot/Paper API 1.21-R0.1-SNAPSHOT

### Dependencies:
- **Paper API** (provided) - Core server API
- **PlaceholderAPI 2.11.6** (provided, soft) - Placeholder support
- **Oraxen** (soft-dependency, legacy support) - Custom items (being replaced)

### Build Tools:
- **Maven Compiler Plugin 3.11.0** - Java compilation
- **Maven JAR Plugin 3.3.0** - JAR packaging
- **ProGuard Maven Plugin 2.6.1** - Code obfuscation

### Development Tools:
- Git version control
- Custom push script (`push.sh`) with SSH key deployment
- Claude Code for AI-assisted development

### Resource Pack:
- Format: 34 (Minecraft 1.21+)
- Custom models using Custom Model Data
- ECHO_SHARD base item for all gems
- Texture-based energy states

---

## 5. Entry Points and Configuration Files

### Plugin Entry Points:

1. **Main Class:** `/src/main/java/dev/xoperr/blissgems/BlissGems.java`
   - `onEnable()` - Plugin initialization
   - `onDisable()` - Cleanup and data saving
   - `onLoad()` - Pre-initialization

2. **Commands:**
   - `/bliss` - Main command (opens GUI or shows help)
   - Subcommands: give, giveitem, energy, withdraw, info, reload, pockets, amplify, toggle_click, ability:main, ability:secondary, trust, untrust, list, autosmelt

3. **Events:** 10 listener classes handle all game events
   - PlayerJoinListener - First gem assignment
   - PlayerDeathListener - Energy loss, soul absorption
   - GemInteractListener - Ability activation
   - PassiveListener - Passive effects, phasing, double jump
   - AutoEnchantListener - Tier 2 auto-enchants
   - GemDropListener - Energy bottle drops
   - UpgraderListener - Gem upgrading
   - RepairKitListener - Pedestal placement
   - ReviveBeaconListener - Revive system
   - StunListener - Movement blocking during stun

### Configuration Files:

1. **`config.yml`** (root and resources)
   - Energy system configuration
   - Gem enable/disable toggles
   - Passive effect settings
   - Ability cooldowns and damage
   - Message templates with color codes

2. **`plugin.yml`**
   - Plugin metadata
   - Command definitions
   - Permission nodes hierarchy

3. **Data Storage:**
   - `/plugins/BlissGems/playerdata/*.yml` - Player energy storage
   - `/plugins/BlissGems/cooldowns/*.yml` - Ability cooldowns (persistent)

4. **Resource Packs:**
   - `/BlissGems/pack.mcmeta` - Resource pack metadata
   - `/BlissGems/assets/` - Custom textures and models

---

## 6. Architectural Patterns and Decisions

### Design Patterns:

1. **Manager Pattern**
   - 14 specialized managers handle different aspects
   - Separation of concerns (GemManager, EnergyManager, AbilityManager, etc.)
   - Centralized access through main plugin instance

2. **Strategy Pattern**
   - 8 ability classes (one per gem type)
   - Each implements gem-specific behavior
   - Common interface through main plugin

3. **Observer Pattern**
   - Event-driven architecture using Bukkit's event system
   - 10 listener classes observe game events
   - Decoupled event handling

4. **Singleton Pattern**
   - Main plugin class as singleton
   - Managers instantiated once and accessed globally

5. **Caching Pattern**
   - In-memory caching for player data (energy, active gems, cooldowns)
   - Periodic disk persistence
   - Cache invalidation on player quit

### Key Architectural Decisions:

1. **Custom Item System**
   - Migrated from Oraxen to custom PDC-based system
   - Uses vanilla ECHO_SHARD as base item
   - Custom Model Data for textures (1001-1008 for T1, 2001-2008 for T2)
   - Persistent Data Container for item identification
   - Allows standalone operation without Oraxen

2. **Energy-Based Progression**
   - Energy affects both abilities and passives
   - Creates risk/reward through PvP
   - Visual feedback through gem texture changes
   - Persistent across sessions

3. **Tier System**
   - Two-tier progression (Basic → Enhanced)
   - Tier 2 unlocks additional abilities
   - Tier 2 enables auto-enchanting
   - Upgrader item required for progression

4. **Ability Activation**
   - Multiple activation methods:
     - Right-click with gem in offhand
     - Commands (`/bliss ability:main`, `/bliss ability:secondary`)
     - Click activation toggle
   - Shift-key for secondary abilities (Tier 2)

5. **Data Persistence**
   - YAML-based storage for player data
   - Cooldowns persist across restarts
   - Energy saved on every change
   - Automatic backup creation

6. **Security & Obfuscation**
   - ProGuard obfuscation in build process
   - Source code protected
   - Mappings saved for debugging
   - All Rights Reserved license

7. **Compatibility**
   - Soft dependency on PlaceholderAPI
   - Legacy Oraxen support (being phased out)
   - Paper-optimized but Spigot-compatible
   - Version-specific (1.21+)

8. **Performance Optimizations**
   - Scheduled tasks for passive effects (1-second intervals)
   - Lazy loading of player data
   - Caching to reduce disk I/O
   - Efficient particle rendering

9. **Feature Modularity**
   - Each gem type is self-contained
   - Managers handle specific responsibilities
   - Easy to add new gem types
   - Configuration-driven behavior

10. **User Experience**
    - Visual feedback (particles, sounds)
    - Action bar messages for cooldowns
    - GUI for easy access
    - Color-coded messages
    - Trusted players system prevents griefing

### Code Organization:
- Package structure: `dev.xoperr.blissgems.*`
- Clear separation: abilities, commands, listeners, managers, utils
- Decompiled code (CFR 0.152) - obfuscated production version
- Consistent naming conventions
- Manager-based dependency injection

### Notable Implementation Details:
- Charging mechanics for Fire and Flux gems
- Ray-tracing for targeted abilities
- Particle trail systems for projectiles
- Stun system with movement blocking
- Flow state with action tracking
- Critical hit counter system
- Soul capture/release system
- Portable inventory (Pockets ability)
- Astral Projection with spectator mode
- Auto-smelting for Wealth gem

---

## Summary

This is a sophisticated, feature-rich Minecraft plugin designed for the Bliss SMP server, with a focus on unique gem-based gameplay mechanics, balanced PvP through the energy system, and extensive customization options.

The plugin implements 8 unique gem types, each with their own passive effects and active abilities across 2 tiers. The energy system creates a risk/reward dynamic in PvP, and the custom item system allows for standalone operation without external dependencies.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build the plugin
mvn clean package

# Build quietly (less output)
mvn clean package -q

# Output JAR location
target/BlissGems-1.0.1.jar
```

## Project Overview

BlissGems is a Minecraft plugin for Paper/Purpur 1.21+ that implements a gem-based ability system. Players equip gems in their offhand for passives and use them in main hand for active abilities.

**Dependencies:** Oraxen (custom items), PlaceholderAPI (optional)

## Architecture

### Core Plugin Structure

- **BlissGems.java** - Main plugin class, initializes all managers and abilities
- All components accessed via getters from main plugin instance

### Manager Layer

- **GemManager** - Tracks active gems, handles gem giving/detection via Oraxen IDs
- **EnergyManager** - Manages energy system (0-10), persists to player data
- **AbilityManager** - Handles cooldowns for all abilities
- **ConfigManager** - Centralized config access with getters for all values
- **PassiveManager** - Periodic passive effect application
- **CooldownDisplayManager** - Action bar cooldown display

### Ability Classes (one per gem type)

Each ability class in `abilities/` follows the pattern:
- `onRightClick(Player, int tier)` - Main entry point
- Tier 1: Primary ability on RMB
- Tier 2: Secondary ability on Shift+RMB, falls back to primary otherwise

### Listener Layer

- **GemInteractListener** - Routes RMB to correct ability class based on gem type
- **PassiveListener** - Handles passive effects (damage modifiers, fall immunity, etc.)
- **AutoEnchantListener** - Applies/removes enchants when switching held items (Tier 2 only)
- **UpgraderListener** - Handles gem upgrade items
- **PlayerDeathListener** - Energy loss/gain on kills

### Oraxen Integration

Gems are Oraxen custom items with specific ID patterns:
- `{type}_gem_t1` - Tier 1 (e.g., `fire_gem_t1`)
- `{type}_gem_t2` - Tier 2 (e.g., `fire_gem_t2`)
- `{type}_gem_upgrader` - Upgrader items
- `energy_bottle`, `gem_trader` - Special items

GemType enum handles ID parsing/building.

## Key Patterns

### Adding New Abilities

1. Add method to appropriate ability class
2. Wire it in `onRightClick()` with tier/sneak checks
3. Add cooldown key to config.yml
4. Use `plugin.getAbilityManager().canUseAbility()` / `useAbility()`

### Passive Effects

Passives check gem type via `plugin.getGemManager().hasGemTypeInOffhand()` and energy state via `plugin.getEnergyManager().arePassivesActive()`.

### Energy States

- Energy 2-10: Full functionality
- Energy 1: Passives disabled (Ruined)
- Energy 0: Abilities disabled (Broken)

## API Compatibility

Uses Paper 1.21+ API. Key 1.21 changes:
- `Tag.ENTITY_TYPES_SENSITIVE_TO_SMITE` instead of deprecated `getCategory()`

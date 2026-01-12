# COMPLETE GEM ABILITIES & PARTICLE ANALYSIS
## All 8 Bliss SMP Gems - Skript Implementation

**Generated:** 2026-01-12
**Source Files:** `Bliss Script Base.sk`, `Bliss Script Flux.sk`
**Location:** `researches/particles example/`

---

## TABLE OF CONTENTS

1. [Astra Gem](#astra-gem)
2. [Fire Gem](#fire-gem)
3. [Flux Gem](#flux-gem)
4. [Life Gem](#life-gem)
5. [Puff Gem](#puff-gem)
6. [Speed Gem](#speed-gem)
7. [Strength Gem](#strength-gem)
8. [Wealth Gem](#wealth-gem)
9. [Particle Color Reference](#particle-color-reference)
10. [Technical Implementation Notes](#technical-implementation-notes)

---

# ASTRA GEM

**Theme:** "Manage the tides of the cosmos"
**Colors:** Light Purple / Deep Purple
**Item Types:**
- T1: Amethyst Shard (custom model data 13-96)
- T2: Prismarine Shard (custom model data 13-96)

---

## TIER 1 ABILITIES

### PRIMARY: Dimensional Drift
**Activation:** Double right-click with gem in offhand (sword/axe/air in main hand)
**Cooldown:** 40 seconds
**Energy Cost:** Requires energy > 1
**Duration:** 5 seconds

**Mechanics:**
1. Spawns invisible, invincible horse with enhanced attributes
   - Movement speed: 0.3
   - Jump strength: 1.2
   - Max health: 53 hearts
2. Player becomes invisible and rides the horse
3. Armor becomes "crimson button" (invisible) while retaining enchantments
4. Can cancel early by sneaking
5. Taking damage cancels ability and sets cooldown to 35s

**Particles:** None (invisibility effect only)

**Code Location:** Lines 4474-4557 in Base.sk

### PASSIVES (T1):
1. **Phasing** - Phase through obstacles
2. **Soul Healing** - Regeneration based on soul mechanics
3. **Soul Capture** - Captures souls from defeated enemies

---

## TIER 2 ABILITIES

### PRIMARY: Astral Void
**Activation:** Right-click on entity with gem in main hand
**Cooldown:** 2 minutes (or 1 minute with dragon egg)
**Energy Cost:** Requires energy > 1
**Range:** 6 blocks radius
**Duration:** 30 seconds

**Mechanics:**
1. Creates expanding purple circle animation at target location
2. Applies Darkness I effect to non-trusted players within 6 blocks
3. Continuous area denial for 30 seconds
4. Draws circles at multiple heights simultaneously

**Particle Formation:**
```skript
set particle of {_astcircles::*} to dust particle using dustOption(rgb(106, 11, 184), 1)
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (106, 11, 184) - Deep Purple
- **Size:** 1.0
- **Force:** Enabled (renders through blocks)

**Animation Pattern:**
- Circle radii progression: 0.75 → 1 → 1.25 → 1.5 → 1.75 → 2 → 2.5 → 3 → 3.5 → 4 → 4.5 → 5 → 5.5 → 6 blocks
- Simultaneous layers:
  - finalcircle (6 blocks) at ground level
  - largecircle2 (5 blocks) at +1 block
  - largecircle (4 blocks) at +2 blocks
  - mediumcircle2 (3 blocks) at +3 blocks
  - mediumcircle (2 blocks) at +4 blocks
  - smallcircle (1 block) at +5 blocks
- Redraws every 15 ticks (0.75 seconds)
- Total 10 iterations over 30 seconds

**Visual Effect:** Multi-layered purple dome that pulses outward

**Code Location:** Lines 4580-4651 in Base.sk

### SECONDARY: Astral Projection
**Activation:** Right-click with gem in main hand (no entity target)
**Cooldown:** 5 minutes
**Energy Cost:** Requires energy > 1

**Mechanics:**
1. Spawns zombie clone at player location with player's equipment
2. Clone is immobile (Slowness 255), invincible, and has player's name
3. Player becomes invisible (Invisibility for 6900 days)
4. Walk speed increased to 0.35
5. Inventory cleared and stored
6. Toggle off by activating again

**Particles:** None (invisibility and clone visual only)

**Code Location:** Lines 4653-4708 in Base.sk

---

# FIRE GEM

**Theme:** "Manipulate fire"
**Colors:** Bright Orange / Red
**Item Types:**
- T1: Amethyst Shard (custom model data 1-81)
- T2: Prismarine Shard (custom model data 1-81)

---

## TIER 1 ABILITIES

### PRIMARY: Crisp
**Activation:** Double right-click with gem in offhand (sword/axe/air in main hand)
**Cooldown:** 42 seconds
**Energy Cost:** Requires energy > 1
**Range:** 5 blocks radius
**Duration:** 10 seconds (terrain transformation)

**Mechanics:**
1. Creates expanding orange fire circles
2. Transforms terrain within 5 blocks:
   - Grass/Stone/Dirt → Netherrack/Magma/Basalt
   - Oak/Spruce/Birch logs → Crimson/Warped Stem
   - All leaves → Nether Wart Block/Warped Wart Block
   - Short grass → Crimson/Warped Root
   - Tall grass → Twisting/Weeping Vines
   - Water → Evaporates (plays extinguish sound, spawns cloud particles)
3. Blocks become unbreakable during duration
4. All blocks revert after 10 seconds

**Particle Formation:**
```skript
set particle of {_fccircles::*} to dust particle using dustOption(rgb(255, 119, 0), 1) with force
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (255, 119, 0) - Bright Orange
- **Size:** 1.0
- **Force:** Enabled

**Animation Pattern (8 ticks):**
- Tick 1: circles radius 0.75, 1
- Tick 2: circles radius 1.25, 1.5
- Tick 3: circles radius 1.75, 2
- Tick 4: circles radius 2.25, 2.5
- Tick 5: circles radius 2.75, 3
- Tick 6: circles radius 3.5, 4
- Tick 7: circles radius 4.5, 5
- Tick 8: circle radius 5 (final)

**Visual Effect:** Expanding orange shockwave at ground level

**Water Evaporation Effect:**
```skript
# 30% chance when water block found
draw 2 cloud at loop-block's location with offset vector(0.1, 0.1, 0.1) with extra 0
play sound "BLOCK_FIRE_EXTINGUISH" with volume 0.2 and pitch 1
```

**Code Location:** Lines 6356-6503 in Base.sk

### PASSIVES (T1):
1. **Fire Resistance** - Immune to fire and lava damage
2. **Autosmelt** - Automatically smelts mined ores (lines 6309+)
3. **Flamestrike** - 5% chance to ignite attacked entities
4. **Fireshot** - 5% chance to ignite shot projectiles

---

## TIER 2 ABILITIES

### PRIMARY: Meteor Shower
**Activation:** Hit an entity while holding Fire T2
**Cooldown:** 1 minute 12 seconds (or 36 seconds with dragon egg)
**Duration:** 10 seconds (60 fireballs total)

**Mechanics:**
1. Spawns 60 large fireballs above target
2. Random spawn positions: 10 blocks above, ±2 blocks horizontal
3. Fireballs pushed downward at force 2
4. ExplosionPower: 1.3
5. Tracks target movement (checks if victim is alive)
6. One fireball every 10 ticks (0.5 seconds)

**Particles:** Fireball trails (inherent from large fireball entity)

**Code Location:** Lines 6763-6792 in Base.sk

### SECONDARY: Charged Fireball
**Activation:** Hold right-click to charge, then left-click to launch
**Cooldown:** 1 minute (or 30 seconds with dragon egg)
**Charging System:** 6 charge levels (1-6)

**Charge Level Mechanics:**
- Level 1: ExplosionPower 1.25
- Level 2: ExplosionPower 1.5
- Level 3: ExplosionPower 1.75
- Level 4: ExplosionPower 2.0
- Level 5: ExplosionPower 2.25
- Level 6: ExplosionPower 2.5
- Launch force: 3
- Player immune to own explosion for 4 seconds

**Visual Feedback:** Boss bar showing charge progress

**Particles:** Fireball trail (inherent from large fireball entity)

**Code Location:** Lines 6794-6909 in Base.sk

### ENHANCED PASSIVES (T2):
1. **Fire Resistance** - Same as T1
2. **Autosmelt** - Same as T1
3. **Flamestrike** - Increased to 8% chance
4. **Fireshot** - Increased to 8% chance

---

# FLUX GEM

**Theme:** "Everything is a fluctuation"
**Colors:** Cyan / Electric Blue / Dark Cyan
**Item Types:**
- T1: Amethyst Shard (custom model data 97-105)
- T2: Prismarine Shard (custom model data 97-105)

---

## TIER 1 ABILITIES

### PRIMARY: Static Burst
**Activation:** Double right-click with gem in offhand (sword/axe/air in main hand)
**Cooldown:** 30 seconds
**Energy Cost:** Requires energy > 1
**Range:** 5 blocks radius
**Damage:** 3 × stored damage percentage of target's health

**Mechanics:**
1. Pushes all non-trusted entities within 5 blocks away at speed 2
2. Damages entities based on accumulated damage variable
3. Creates expanding cyan circles with smoke
4. Resets stored damage after use

**Particle Formation:**
```skript
function FluxCircle(s: number, p: location):
  # Uses dust particle: dustOption(rgb(94, 215, 255), 1)
  # Draws smoke particles alongside
  # Spacing: 0.1 blocks between particles
  # Circumference calculation: 2 × π × radius
```

**Particle Details:**
- **Type:** Dust + Smoke particles
- **RGB Color:** (94, 215, 255) - Cyan/Electric Blue
- **Size:** 1.0
- **Force:** Enabled

**Animation Pattern:**
- 10 iterations total
- Starts at radius 1
- Increases by 0.5 each tick
- Final radius: 5.5 blocks
- Last iteration repeats 7 times with 0.3s delays (FluxCirclelast function)

**Visual Effect:** Expanding cyan electrical burst with smoke

**Code Location:** Lines 9600-9632 in Flux.sk

### PASSIVES (T1):
1. **Flow State** - Movement speed increases with continuous movement
   - Resets when damaged
   - Progressive speed boost system
2. **Shocking Chance** - Extra damage based on stored damage
3. **Tireless** - Removes Slowness, Hunger, Weakness every tick
4. **Conduction** - Stores 1/6th of damage taken for 2 minutes
   - Displayed in action bar
5. **Charged** - Related to damage storage system

---

## TIER 2 ABILITIES

### PRIMARY: Flux Ray (Charged Beam)
**Activation:** Left-click with gem in main hand
**Cooldown:** 30 seconds (or 15 seconds with dragon egg)
**Energy Cost:** Consumes stored Kinetic charge (0-200%)
**Charging System:** Multi-tier based on Kinetic charge percentage

**Charge Tier Mechanics:**
- **0%**: No effect, plays failure sounds
- **0.01-5%**: Weak burst
- **5-41%**: Multiple tiers with increasing beam duration
- **56-65%**: Laser beam mode (~2 seconds)
- **65-76%**: Extended beam with ground particles
- **76-85%**: Longer beam with additional effects
- **85-100.1%**: Maximum beam power
- **100.1-199.99%**: Expansion burst
- **200%**: OVERCHARGED BEAM
  - Self-damage: reduces player to 0.1 health
  - Pushes player backwards (speed 4) and upward (speed 2)
  - Maximum duration and damage
  - Multiple particle effects combined

**Beam Particle Formation:**
```skript
function energyboom1001(loc: location, p: player):
  set {_fluxn} to 1  # Distance counter
  set {_fluxloc} to {_loc}
  set {_superfkux} to 5  # Beam radius

  loop 25 times:  # Beam segments
    if {-fluxShootingBeam::%{_p}'s uuid%} is true:
      loop 3 times:  # Sub-iterations
        set {_fluxloc} to location {_fluxn} metres in front of {_loc}
        set {_pl} to {_fluxloc}
        set {_step} to 45  # Angular spacing
        set {_iterations} to 8  # Points in circle

        loop {_iterations} times:
          set {_angle} to loop-value-3 * {_step} + 30
          # Spherical vector for circular cross-section
          set {_v} to spherical vector radius {_superfkux}, yaw of {_fluxloc} - 90, pitch {_angle}
          draw 3 sonic_boom at {_pl} ~ {_v} with extra 0.000000001 with force

        add 0.3 to {_fluxn}  # Move forward
        remove 0.1 from {_superfkux}  # Taper beam
      wait 1 tick
```

**Particle Details:**
- **Type:** sonic_boom particles
- **Pattern:** 8 points per cross-section (45° spacing)
- **Beam Structure:**
  - 25 segments along beam path
  - 3 sub-iterations per segment
  - Each segment advances 0.3 meters
  - Total beam length: ~7.5 meters
- **Tapering:** Radius starts at 5, gradually decreases
- **Animation:** 1 tick delay between segments (smooth movement)

**Visual Effect:** Animated cyan energy beam with circular cross-section that tapers toward the end

**Sound Effects:**
- "item.axe.scrape" (charge sounds)
- "block.beacon.ambient" (beam activation)
- "entity.warden.sonic_boom" (beam fire)
- "entity.generic.explode" (impact)
- "entity.lightning_bolt.thunder" (200% overcharge)

**60 Beam Functions:** energyboom1001 through energyboom1060
- Each function represents different charge levels
- Nearly identical structure with slight parameter variations
- Progressive beam effects based on charge percentage

**Code Location:** Lines 9849-10247 in Flux.sk

### SECONDARY: Ground Stun
**Activation:** Hit entity with gem in main hand
**Cooldown:** 30 seconds (or 15 seconds with dragon egg)
**Duration:** 3 seconds

**Mechanics:**
1. Connects cyan line particles from attacker to victim
2. Victim cannot move (movement cancelled)
3. Deals damage based on attacker's Kinetic charge
4. Resets attacker's Kinetic charge to 0
5. Draws line particles 3 times over duration

**Particle Formation:**
```skript
set particle of {_trail} to dust particle using dustOption(rgb(16, 131, 173), 1.5) with force
# Draws line 10 times, 3 iterations, 0.1s between each
```

**Particle Details:**
- **Type:** Dust line particles
- **RGB Color:** (16, 131, 173) - Dark Cyan
- **Size:** 1.5
- **Pattern:** Line from victim to attacker
- **Animation:** Redraws 10 times per iteration, 3 total iterations

**Visual Effect:** Dark cyan tether binding victim to attacker

**Code Location:** Lines 9801-9843 in Flux.sk

### TERTIARY: Kinetic Overdrive
**Activation:** Right-click with gem in main hand
- No target = self-buff
- With target = buff target player

**Cooldown:** 1 minute 20 seconds (or 40 seconds with dragon egg)
**Duration:** 30 seconds

**Mechanics:**
1. **Single Target Mode:** Builds attack multiplier as target takes damage
   - 3 hits = 1.1× damage on next attack
   - 6 hits = 1.2× damage
   - 9 hits = 1.3× damage
   - 12 hits = 1.4× damage
   - 15 hits = 1.5× damage
2. **Self-Buff Mode:** Activates Kinetic Overdrive on self
3. **Mining Bonus:** Breaking blocks gives Haste stacks
   - 5 blocks = Haste I (20s)
   - 10 blocks = Haste II (40s)
   - 15 blocks = Haste III (60s)
   - 20 blocks = Haste IV (80s)
   - 25 blocks = Haste V (100s)
4. Cyan line particles connect caster to target every 0.1s

**Self-Cast Particle Formation:**
```skript
set particle of {_flcircles::*} to dust particle using dustOption(rgb(94, 215, 255), 0.9) with force
# Radii: 0.25 → 0.5 → 0.75 → 1 → 1.25 → 1.5 → 1.75 → 2 → 2.25 → 2.5 → 2.75 → 3
```

**Target-Cast Particle Formation:**
```skript
set particle of {_trail} to dust particle using dustOption(rgb(94, 215, 255), 1.5) with force
# Line from caster to target, 10 times per 0.1s, 3 iterations
```

**Particle Details:**
- **Type:** Dust particles (circles or line)
- **RGB Color:** (94, 215, 255) - Cyan
- **Size:** 0.9 (circles) or 1.5 (line)
- **Animation:** Expanding circles (self) or pulsing line (target)

**Visual Effect:** Cyan energy field (self) or energy tether (target)

**Code Location:** Lines 9694-9778 in Flux.sk

### CHARGING SYSTEM (T2 Only)
**Activation:** Drop Flux T2 gem
**Max Charge:** 2,000,000 watts = 100% Kinetic charge
**Charge Rate:** 0.667% per second when holding gem

**Overcharging:**
- Activates at >100% Kinetic charge
- 5 second warning countdown
- Charges from 100% → 200% at 0.667% per second
- Allows access to 200% Overcharged Beam

**Charge Sources:** Players must feed items to charge
- Diamond = 5,202 watts
- Diamond Block = 46,819 watts
- Netherite Ingot = 46,819 watts
- Netherite Block = 421,378 watts

**Charging Particle Effects:**
```skript
# Active charging particles (every second)
draw 16 of dust using dustOption(rgb(94, 215, 255), 1) at location 1 block above player
  with offset vector(0.4, 0.4, 0.4) with extra 0 with force
draw 4 of smoke at location 1 block above player
  with offset vector(0.5, 0.5, 0.5) with extra 0 with force
```

**Particle Details:**
- 16 cyan dust particles + 4 smoke particles
- Spawns 1 block above player
- Offset creates cloud effect
- Indicates active charging state

**Code Location:** Lines 9245-9344 in Flux.sk

### ENHANCED PASSIVES (T2):
1. **Flow State** - Same as T1
2. **Shocking Chance** - Same as T1
3. **Tireless** - Same as T1
4. **Conduction** - Enhanced damage storage system
5. **Charged** - Enables Kinetic charge system (0-200%)

### ACTION BAR DISPLAY:
- **T1:** `<##009ac9>🔺 &b{damage}%`
- **T2:** `&7☄ {Ground Stun CD} <##5ED7FF>🔮 {Static Burst CD} <##5ED7FF>🔮 {Kinetic %} &7🌀 {Kinetic Overdrive CD}`

---

# LIFE GEM

**Theme:** "Heal yourself and drain life from enemies"
**Colors:** Pink / Magenta
**Item Types:**
- T1: Amethyst Shard (custom model data)
- T2: Prismarine Shard (custom model data)

---

## TIER 1 ABILITIES

### PRIMARY: Vitality Vortex
**Activation:** Double right-click with gem in offhand (sword/axe/air in main hand)
**Cooldown:** 45 seconds
**Energy Cost:** Requires energy > 1
**Range:** 5 blocks radius
**Duration:** 9 seconds

**Mechanics:**
1. Transforms natural blocks in 5-block radius temporarily
2. Block transformations:
   - Flowers → Dead corals (grants Regeneration 2 for 1s)
   - Leaves → Tuff/dead coral blocks (grants Absorption 2 for 1s)
   - Grass/Stone/Dirt → Tuff/dead coral blocks
   - Logs → Acacia logs (grants Resistance 1 for 1s)
3. Removes water blocks in radius
4. Sets player food level to 10, saturation to 20
5. Radius expands incrementally (1→2→3→4→5 blocks) over 5 iterations
6. Makes blocks unbreakable during active period
7. Restores original blocks after 9 seconds

**Particles:** None explicitly coded for this ability

**Code Location:** Base.sk

### PASSIVE: Healing
**Activation:** Passive when gem in offhand or main hand
**Cooldown:** 9 seconds between heals
**Mechanics:**
- Heals 0.5 hearts (1 HP) every 9 seconds
- Removes Wither effect
- Action bar: "&d🔺 &b%cooldown%"

---

## TIER 2 ABILITIES

### PRIMARY: Vitality Vortex (T2)
**Activation:** Double right-click with gem in offhand
**Cooldown:** 45 seconds
**Mechanics:** Identical to T1 version

### SECONDARY: Heart Lock
**Activation:** Attack entity while holding Life T2
**Cooldown:** 2 minutes (1 minute with dragon egg)
**Duration:** 20 seconds (15 seconds locked, then resets)

**Mechanics:**
1. Locks victim's max health for 20 seconds
2. If victim health > 3: max health = current health
3. If victim health ≤ 3: max health = 3
4. After 15 seconds, max health resets to 10

**Particle Formation:**
```skript
loop 5 times: # 5 seconds
  loop 10 times: # 1 second
    set {_attacker} to location 0.5 metres above attacker
    set {_victim} to location 0.5 metres above victim
    set {_trail} to a line from {_victim} to {_attacker}
    set particle of {_trail} to dust particle using dustOption(rgb(255, 0, 180), 1.5) with force
    draw shapes {_trail} at {_victim}
    wait 0.1 seconds
```

**Particle Details:**
- **Type:** Dust line particles
- **RGB Color:** (255, 0, 180) - Pink/Magenta
- **Size:** 1.5
- **Animation:** Energy beam from victim to attacker
- **Duration:** First 5 seconds of 20-second effect
- **Update Rate:** 10 times per second (50 total draws)

**Visual Effect:** Pink energy drain beam

**Code Location:** Base.sk

### TERTIARY: Heart Drainer (Group AOE)
**Activation:** Left-click with Life T2 in main hand
**Cooldown:** 2 minutes (1 minute with dragon egg)
**Range:** 3 blocks radius
**Duration:** 20 seconds

**Mechanics:**
1. AOE around caster
2. Drains health from enemies in range
3. Reduces enemy max health

**Particle Formation:**
```skript
set {_lcircles::circle} to a circle of radius 0.75
set {_lcircles::smallcircle} to a circle of radius 1
set {_lcircles::smallcircle1} to a circle of radius 1.25
set {_lcircles::smallcircle2} to a circle of radius 1.5
set {_lcircles::smallcircle3} to a circle of radius 1.75
set {_lcircles::mediumcircle} to a circle of radius 2
set {_lcircles::mediumcircle1} to a circle of radius 2.25
set {_lcircles::mediumcircle2} to a circle of radius 2.5
set {_lcircles::mediumcircle3} to a circle of radius 2.75
set {_lcircles::largecircle} to a circle of radius 3
set particle of {_lcircles::*} to dust particle using dustOption(rgb(255, 0, 180), 1.5) with force
```

**Animation Pattern:**
```skript
draw shapes {_lcircles::circle} at player's location
draw shapes {_lcircles::smallcircle} at player's location
wait 1 tick
draw shapes {_lcircles::smallcircle1} at player's location
draw shapes {_lcircles::smallcircle2} at player's location
wait 1 tick
draw shapes {_lcircles::smallcircle3} at player's location
draw shapes {_lcircles::mediumcircle} at player's location
wait 1 tick
draw shapes {_lcircles::mediumcircle1} at player's location
draw shapes {_lcircles::mediumcircle2} at player's location
wait 1 tick
draw shapes {_lcircles::mediumcircle3} at player's location
draw shapes {_lcircles::largecircle} at player's location
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (255, 0, 180) - Pink/Magenta
- **Size:** 1.5
- **Animation:** Expanding concentric circles from 0.75 to 3 blocks
- **Pattern:** Two circles drawn per tick, 7 ticks total

**Visual Effect:** Pink expanding shockwave

**Code Location:** Base.sk

### QUATERNARY: Circle of Life
**Activation:** Right-click or right-click on entity with Life T2
**Cooldown:** 2 minutes 59 seconds (1:30 with dragon egg)
**Range:** 4 blocks radius
**Duration:** 30 seconds

**Mechanics:**
1. Sets player max health to 15 and heals to full
2. Creates 4-block radius healing circle
3. Lasts 30 seconds (15 cycles × 2 seconds each)
4. Heals allies within range for 2 seconds per cycle
5. Damages enemies within range for 2 seconds per cycle
6. When activated on entity: repairs armor durability (+2 per cycle, 15 cycles)

**Particle Formation:**
```skript
set {_lcircles::circle} to a circle of radius 0.75
# ... [all circles up to] ...
set {_lcircles::finalcircle} to a circle of radius 4
set particle of {_lcircles::*} to dust particle using dustOption(rgb(255, 0, 179), 1) with force
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (255, 0, 179) - Pink (slightly different from Heart Lock)
- **Size:** 1.0
- **Animation:** Expands from 0.75 to 4 blocks over 13 ticks
- **Duration:** Redraws every 0.25 seconds for 30 seconds
- **Pattern:** Maintains 4-block circle after full expansion

**Visual Effect:** Persistent pink healing circle that pulses

**Code Location:** Base.sk

### QUINARY: Enhanced Absorption
**Activation:** Consume golden apple while holding Life T2
**Cooldown:** None

**Mechanics:**
- Golden Apple: Grants Absorption 2 for 2 minutes
- Enchanted Golden Apple: Grants Absorption 5 for 2 minutes
- Effects hide particles and potion icon

### LIFE SYSTEM MECHANICS

**Max Health States:**
```
No effects: 10 hearts (20 HP)
Circle of Life active: 15 hearts (30 HP)
Solo drain active: 6 hearts (12 HP)
Group drain active: 8 hearts (16 HP)
Solo + Group drain: 6 hearts (12 HP)
Circle of Life + Solo drain: 10 hearts (20 HP)
Circle of Life + Group drain: 12 hearts (24 HP)
Circle of Life + both drains: 10 hearts (20 HP)
Heart Locked: Current health (minimum 3 hearts)
```

### PASSIVE EFFECTS
**T1:**
- Heals 0.5 hearts every 9 seconds
- Removes Wither effect

**T2:**
- Heals 0.5 hearts every 6 seconds (faster than T1)
- Removes Wither effect
- Enhanced absorption from golden apples

---

# PUFF GEM

**Theme:** "Defy gravity with double jumps"
**Colors:** White / Pure White
**Item Types:**
- T1: Amethyst Shard (custom model data)
- T2: Prismarine Shard (custom model data)

---

## TIER 1 ABILITIES

### PRIMARY: Double Jump
**Activation:** Press jump key twice (flight toggle) with gem equipped
**Cooldown:** 6 seconds
**Energy Cost:** Requires energy > 1

**Mechanics:**
- Forward speed: 0.6
- Upward speed: 0.5 (offhand) or 0.6 (main hand)
- Re-enables flight after cooldown

**Particle Formation:**
```skript
draw 5 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
loop 6 times:
  wait 1 ticks
  draw 2 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
  wait 1 ticks
  draw 3 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
```

**Particle Details:**
- **Type:** Cloud particles (native white)
- **Count:** 5 initial, then alternating 2 and 3
- **Offset:** Vector(0.1, 0.1, 0.1)
- **Duration:** 6 ticks (0.3 seconds)
- **Pattern:** Trail behind player during jump

**Visual Effect:** White cloud trail during double jump

**Code Location:** Base.sk

### PASSIVE: Auto-Enchant
**Cooldown:** 1 minute
**Mechanics:**
- Bows: Power 3 and Punch 1
- Boots: Feather Falling 2

### PASSIVES (T1):
1. **Fall Damage Immunity** - Complete negation
2. **Crop Protection** - Prevents farmland trampling
3. **Sculk Sensor Silence** - Prevents sculk activation

---

## TIER 2 ABILITIES

### PRIMARY: Double Jump (Enhanced)
**Activation:** Press jump key twice
**Cooldown:** 6 seconds

**Mechanics:**
- Forward speed: 0.8 (faster than T1)
- Upward speed: 0.7 (higher than T1)

**Particle Formation:** Same as T1
```skript
draw 5 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
loop 6 times:
  wait 1 ticks
  draw 2 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
  wait 1 ticks
  draw 3 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
```

**Visual Effect:** Same white cloud trail but with stronger push

**Code Location:** Base.sk

### SECONDARY: Breezy Bash (Single Target)
**Activation:** Attack entity while holding Puff T2
**Cooldown:** 1 minute 15 seconds (40 seconds with dragon egg)
**Duration:** 2.5 seconds

**Mechanics:**
1. Launches victim upward with speed 16
2. Then slams them down with speed 24
3. Total animation duration: 2.5 seconds

**Particle Formation:**
```skript
# Launch phase
loop 5 times:
  set {_attacker} to location 0.5 metres above attacker
  set {_victim} to location 0.5 metres above victim
  set {_trail} to a line from {_victim} to {_attacker}
  set particle of {_trail} to dust particle using dustOption(rgb(255, 255, 255), 1.5) with force
  draw shapes {_trail} at {_victim}
  wait 0.1 seconds

# Slam phase
loop 2 times:
  loop 10 times:
    # Same line trail effect
    wait 0.1 seconds
```

**Particle Details:**
- **Type:** Dust line particles
- **RGB Color:** (255, 255, 255) - Pure White
- **Size:** 1.5
- **Pattern:** Line trail from victim to attacker
- **Animation:** 5 lines during launch, 20 more during slam (25 total)
- **Duration:** 0.5 seconds (launch) + 2 seconds (slam)

**Visual Effect:** White energy tether during launch and slam

**Code Location:** Base.sk

### TERTIARY: Breezy Bash (Group AOE)
**Activation:** Left-click with Puff T2 in main hand
**Cooldown:** 1 minute 15 seconds (40 seconds with dragon egg)
**Range:** 5 blocks radius

**Mechanics:**
1. Pushes enemies up (speed 1.25) and away (speed 2.5)
2. Does not affect trusted players

**Particle Formation:**
```skript
set {_puffcircles::circle} to a circle of radius 0.75
# ... [expanding circles] ...
set {_puffcircles::finalcircle} to a circle of radius 5
set particle of {_puffcircles::*} to dust particle using dustOption(rgb(255, 255, 255), 1.5) with force
```

**Animation Pattern:**
```skript
draw shapes {_puffcircles::circle} at player's location
draw shapes {_puffcircles::smallcircle} at player's location
wait 1 tick
draw shapes {_puffcircles::smallcircle1} at player's location
draw shapes {_puffcircles::smallcircle2} at player's location
wait 1 tick
# ... [continues expanding] ...
draw shapes {_puffcircles::largecircle3} at player's location
draw shapes {_puffcircles::finalcircle} at player's location
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (255, 255, 255) - Pure White
- **Size:** 1.5
- **Animation:** Expands from 0.75 to 5 blocks over 13 ticks
- **Pattern:** Two circles drawn per tick

**Visual Effect:** White expanding shockwave at ground level

**Code Location:** Base.sk

### QUATERNARY: Dash
**Activation:** Right-click with Puff T2 in main hand
**Cooldown:** 40 seconds (20 seconds with dragon egg)

**Mechanics:**
1. Pushes player forward at speed 3
2. Damages enemies within 1.5 blocks for 5 hearts (10 HP)
3. Duration: 24 ticks (1.2 seconds)
4. Damages every tick during dash

**Particle Formation:**
```skript
loop 2 times:
  loop 12 times:
    wait 1 ticks
    draw 3 cloud at player with offset vector(0.1, 0.1, 0.1) with extra 0
```

**Particle Details:**
- **Type:** Cloud particles
- **Count:** 3 per tick
- **Offset:** Vector(0.1, 0.1, 0.1)
- **Duration:** 24 ticks total
- **Pattern:** Continuous trail behind player

**Visual Effect:** White cloud trail during dash

**Code Location:** Base.sk

### QUINARY: Auto-Enchant T2
**Cooldown:** 1 minute
**Mechanics:**
- Bows: Power 5 and Punch 2 (upgraded from T1)
- Boots: Feather Falling 4 (upgraded from T1)

### PASSIVE: Status Effects on Hit
**Activation:** Chance on damage with Puff T2 in offhand
**Mechanics:**
- 1% chance: Levitation 1 for 2 seconds
- 1% chance: Slow Falling 1 for 2 seconds

### PASSIVES (T2):
1. **Fall Damage Immunity** - Same as T1
2. **Crop Protection** - Same as T1
3. **Sculk Sensor Silence** - Same as T1
4. **Enhanced Auto-Enchant** - Power 5, Punch 2, Feather Falling 4
5. **Status Effect Chance** - Levitation/Slow Falling on hit

---

# SPEED GEM

**Theme:** "Move faster and sedate your foes"
**Colors:** Bright Yellow/Lime
**Item Types:**
- T1: Amethyst Shard (custom model data)
- T2: Prismarine Shard (custom model data)

---

## TIER 1 ABILITIES

### PRIMARY: Terminal Velocity
**Activation:** Double right-click with gem in offhand (sword/axe/air in main hand)
**Cooldown:** 40 seconds
**Energy Cost:** Requires energy > 1
**Duration:** 10 seconds

**Mechanics:**
- Grants Speed 4 for 10 seconds
- Grants Haste 2 for 10 seconds
- Effects are visible (not hidden)

**Particles:** None explicitly coded

**Code Location:** Base.sk

### PASSIVE: Auto-Enchant
**Cooldown:** 1 minute
**Mechanics:**
- Pickaxes/Axes/Hoes/Shovels: Efficiency 2

### PASSIVE: Speed Boost
**Activation:** Continuous when gem equipped
**Mechanics:**
- Speed 1 continuously
- Effects hidden (no particles/icon)

---

## TIER 2 ABILITIES

### PRIMARY: Terminal Velocity (T2)
**Activation:** Double right-click with gem in offhand
**Cooldown:** 40 seconds
**Mechanics:** Identical to T1
- Speed 4 for 10 seconds
- Haste 2 for 10 seconds

**Particles:** None

### SECONDARY: Blur
**Activation:** Attack entity while holding Speed T2
**Cooldown:** 1 minute 30 seconds (45 seconds with dragon egg)
**Duration:** ~7 seconds

**Mechanics:**
1. Creates NPC clones of attacker around victim
2. Clones attack from 7 different positions in sequence
3. Each hit deals 1.8 hearts (3.6 HP) damage
4. Total: 14 hits × 1.8 hearts = 25.2 hearts potential damage
5. Each hit applies knockback
6. Lightning effects at each clone position
7. 2 complete attack cycles

**Attack Sequence Positions:**
1. 2.5 blocks left
2. 2.5 blocks right + 1 up
3. 2.5 blocks front + 1 right
4. 2 blocks behind + 1 left + 1 up
5. 2.5 blocks right
6. 2 blocks front + 1.5 left
7. 2.5 blocks left + 1 up
(Repeats twice)

**Particles:**
- Lightning strike visual effects at each clone position
- No additional dust particles coded

**Special:**
- Clones mirror attacker's appearance
- Custom death message: "%victim% couldn't keep up with %attacker%"

**Code Location:** Base.sk

### TERTIARY: Speed Storm
**Activation:** Right-click on entity or in air with Speed T2
**Cooldown:** 2 minutes 30 seconds (1:15 with dragon egg)
**Range:** 5 blocks radius
**Duration:** 15 seconds

**Mechanics:**
1. Grants Speed 4 to player and allies for 15 seconds
2. Increases attack speed attribute to 5 for 15 seconds
3. Strikes 30 random lightning bolts in area (1 every 0.5 seconds)
4. Lightning spawns within 4 blocks of center
5. Enemies within radius: stunned (cannot move) for 2 seconds

**Particle Formation:**
```skript
set {_spcircles::circle} to a circle of radius 0.75
# ... [expanding circles] ...
set {_spcircles::finalcircle} to a circle of radius 5
set particle of {_spcircles::*} to dust particle using dustOption(rgb(244, 255, 28), 1) with force
set particle of {_sapcircles::*} to 1 of happy_villager particle
```

**Particle Details:**
- **Primary Type:** Dust particles
- **RGB Color:** (244, 255, 28) - Bright Yellow/Lime
- **Size:** 1.0
- **Secondary Type:** Happy villager particles (green sparkles)
- **Animation:** Expands from 0.75 to 5 blocks over 13 ticks
- **Pattern:** Dust circles + happy villager sparkles
- **Special:** 30 lightning strikes over 15 seconds

**Visual Effect:** Yellow expanding storm circle with green sparkles and lightning strikes

**Code Location:** Base.sk

### QUATERNARY: Auto-Enchant T2
**Cooldown:** 1 minute
**Mechanics:**
- Pickaxes/Axes/Hoes/Shovels: Efficiency 5 (upgraded from T1)

### PASSIVES (T1):
1. **Speed 1** - Continuous (hidden)
2. **Auto-Enchant** - Efficiency 2
3. **Soul Sand Speed** - Speed 4 when standing on soul sand

### PASSIVES (T2):
1. **Speed 2** - Continuous (hidden, better than T1)
2. **Dolphin's Grace** - When in water (if no Depth Strider)
3. **Auto-Enchant** - Efficiency 5
4. **Soul Sand Speed** - Speed 5 when on soul sand
5. **Sprint Particles** - When sprinting in water with Depth Strider

**Sprint Particle (T2):**
```skript
if {_p} is sprinting:
  draw 1 of dust particle using dustOption(rgb(244, 255, 28), 1) at location 0.5 blocks below {_p}
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (244, 255, 28) - Bright Yellow/Lime
- **Size:** 1.0
- **Location:** 0.5 blocks below player
- **Frequency:** Every 2 seconds while sprinting in water

**Visual Effect:** Yellow trail particles when sprinting underwater

---

# STRENGTH GEM

**Theme:** "Empower your attacks and weaken your enemies"
**Colors:** Deep Red / Dark Red
**Item Types:**
- T1: Amethyst Shard (custom model data)
- T2: Prismarine Shard (custom model data)

---

## TIER 1 ABILITIES

### PASSIVE: Berserker (Low Health Damage Boost)
**Activation:** Automatic when gem in offhand
**Energy Cost:** Requires energy > 1

**Damage Scaling:**
- Below 3 hearts: +3 damage
- 3.5-4.5 hearts: +2 to +2.5 damage
- 5-5.5 hearts: +1.5 damage
- 6-6.5 hearts: +1 damage
- 7-7.5 hearts: +0.5 damage
- Above 8 hearts: No bonus

### PASSIVE: Auto-Enchant
**Cooldown:** 59 seconds
**Mechanics:**
- Swords/Axes: Sharpness II
- Only enchants if no sharpness already present

### PASSIVES (T1):
1. **Strength I** - Continuous potion effect (hidden)
2. **Berserker** - Up to +3 damage when low health
3. **Auto-Sharpness II** - Every 59 seconds
4. **Cannot be dropped** - Anti-drop protection

---

## TIER 2 ABILITIES

### PRIMARY: Chad Strength (Solo)
**Activation:** Right-click with gem in hand (empty air)
**Cooldown:** 2 minutes 30 seconds (1:15 with dragon egg)
**Duration:** 30 seconds

**Mechanics:**
1. Activates critical hit damage multiplier mode
2. Every 4th critical hit deals 1.6× damage (60% bonus)
3. Counter tracks critical hits
4. Resets to 0 after powered attack

**Particle Formation:**
```skript
set {_scircles::circle} to a circle of radius 0.75
# ... [all circles] ...
set {_scircles::finalcircle} to a circle of radius 4
set particle of {_scircles::*} to dust particle using dustOption(rgb(199, 0, 10), 1.5) with force
```

**Animation Pattern:**
```skript
draw shapes {_scircles::circle} at player's location
draw shapes {_scircles::smallcircle} at player's location
wait 1 tick
draw shapes {_scircles::smallcircle1} at player's location
draw shapes {_scircles::smallcircle2} at player's location
wait 1 tick
# ... [continues expanding] ...
draw shapes {_scircles::largecircle3} at player's location
draw shapes {_scircles::finalcircle} at player's location
wait 1 tick
draw shapes {_scircles::finalcircle} at player's location
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (199, 0, 10) - Deep Red
- **Size:** 1.5
- **Animation:** Expands from 0.75 to 4 blocks over 7 ticks
- **Pattern:** Two circles drawn per tick for smooth expansion

**Visual Effect:** Red rippling shockwave

**Code Location:** Base.sk

### SECONDARY: Chad Strength (Group)
**Activation:** Right-click on entity with gem in hand
**Cooldown:** 2 minutes 30 seconds (1:15 with dragon egg)
**Duration:** 30 seconds (caster) / 15 seconds (allies)
**Range:** 4 blocks

**Mechanics:**
1. Grants buff to caster + all trusted players in 4-block radius
2. Allies get buff for 15 seconds
3. Caster gets buff for 30 seconds
4. Every 9th critical hit deals 1.6× damage
5. Requires more hits than solo mode

**Particles:** Same red expanding circles as solo version

**Code Location:** Base.sk

### TERTIARY: Frailer (On Hit)
**Activation:** Hit enemy while holding gem
**Cooldown:** 2 minutes (1 minute with dragon egg)
**Duration:** 30 seconds

**Mechanics:**
1. Removes ALL potion effects from target
2. Applies Weakness I for 30 seconds
3. Shows boss bar to victim tracking duration
4. Red-colored boss bar with "Frailer" title

**Particle Formation:**
```skript
loop 100 times: # 5 seconds
  set {_attacker} to location 0.5 metres above attacker
  set {_victim} to location 0.5 metres above victim
  set {_trail} to a line from {_victim} to {_attacker}
  set particle of {_trail} to dust particle using dustOption(rgb(199, 0, 10), 1.5) with force
  draw shapes {_trail} at {_victim}
  remove 0.167 from bar progress
  wait 1 tick
```

**Particle Details:**
- **Type:** Dust line particles
- **RGB Color:** (199, 0, 10) - Deep Red
- **Size:** 1.5
- **Animation:** Energy draining beam from victim to attacker
- **Duration:** 5 seconds (100 ticks)
- **Height:** 0.5 meters above both players

**Visual Effect:** Red energy drain beam

**Code Location:** Base.sk

### QUATERNARY: Nullify (AOE)
**Activation:** Left-click with gem in hand
**Cooldown:** 2 minutes (1 minute with dragon egg)
**Range:** 5 blocks radius
**Duration:** 30 seconds

**Mechanics:**
1. Affects all non-trusted players within 5 blocks
2. Stores existing potion effects temporarily
3. Clears all effects for 30 seconds
4. Prevents: drinking potions, throwing splash/lingering potions
5. Restores original effects after 30 seconds
6. Green boss bar with "Nullify" title

**Particles:** Same red expanding circles as Chad Strength (reuses animation)

**Code Location:** Base.sk

### PASSIVE: Enhanced Berserker (T2)
**Damage Scaling (better than T1):**
- Below 4 hearts: +3 damage
- 4.5-5.5 hearts: +2 to +2.5 damage
- 6-6.5 hearts: +2 damage
- 7-7.5 hearts: +1.5 damage
- 8-8.5 hearts: +1 to +0.5 damage
- Above 9 hearts: No bonus

### PASSIVE: Auto-Enchant T2
**Cooldown:** 59 seconds
**Mechanics:**
- Swords/Axes: Sharpness V (maximum level)
- Overrides existing sharpness levels

### PASSIVES (T2):
1. **Strength I** - Continuous
2. **Enhanced Berserker** - Better scaling than T1
3. **Auto-Sharpness V** - Every 59 seconds
4. **Cannot be dropped**

### CRITICAL HIT COUNTER SYSTEM:
```skript
# Solo Mode
if {Test.Double.%attacker%} is 3:
  set damage to damage * 1.6
  set {Test.Double.%attacker%} to 0
add 1 to {Test.Double.%attacker%}

# Group Mode
if {Test.DoubleGroup.%attacker%} is 8:
  set damage to damage * 1.6
  set {Test.DoubleGroup.%attacker%} to 0
add 1 to {Test.DoubleGroup.%attacker%}
```

---

# WEALTH GEM

**Theme:** "Find fortune and unlock hidden riches"
**Colors:** Bright Green / Emerald Green
**Item Types:**
- T1: Amethyst Shard (custom model data)
- T2: Prismarine Shard (custom model data)

---

## TIER 1 ABILITIES

### PRIMARY: Pockets
**Activation:** Double right-click with gem in offhand
**Cooldown:** None
**Energy Cost:** Requires energy > 1

**Mechanics:**
1. Opens personal 9-slot storage inventory (dropper GUI)
2. Persistent storage across sessions (UUID-based)
3. Double-click detection prevents accidental opens
4. Death penalty: 5 random items drop (effectively ~55% loss rate)

**Particles:** None

**Code Location:** Base.sk

### PASSIVE: Ancient Debris Auto-Smelt
**Activation:** Automatic when mining with gem in offhand
**Mechanics:**
- Mining ancient debris drops 2 netherite scraps instead of debris
- Instant conversion (no smelting needed)

### PASSIVES (T1):
1. **Hero of the Village I** - Better villager trades
2. **Luck I** - Better loot
3. **Auto-Fortune I** - Every 59 seconds on pickaxes/axes
4. **Auto-Looting I** - Every 59 seconds on swords
5. **Double XP** - When picking up XP orbs
6. **Armor Durability Drain** - Damages enemy armor on hit (3s cooldown)
7. **Ancient Debris Conversion** - 2 netherite scraps per debris
8. **Pockets Storage** - 9-slot personal inventory

---

## TIER 2 ABILITIES

### PRIMARY: Rich Rush (Mining Boost)
**Activation:** Right-click with gem in hand (empty air)
**Cooldown:** 9 minutes 59 seconds (4:59 with dragon egg)
**Duration:** 5 minutes

**Mechanics:**
1. Doubles ore drops from all ores for 5 minutes
2. Adds 1 extra drop per ore broken
3. Stacks with Fortune enchantment
4. Silk Touch cancels the bonus
5. Supported ores: Iron, Gold, Diamond, Emerald, Lapis, Redstone, Copper, Coal (all variants)

**Particles:** None

**Code Location:** Base.sk

### SECONDARY: Amplification (Enchantment Boost)
**Activation:** Right-click on entity with gem in hand
**Cooldown:** 9 minutes 59 seconds (4:59 with dragon egg)
**Duration:** 5 minutes (then reverts)
**Range:** 2 blocks (for allies)

**Mechanics:**
1. Upgrades ALL max-level enchantments by 1 level
2. Affects caster + all trusted players within 2 blocks
3. Scans entire inventory
4. After 5 minutes, enchantments revert to original levels
5. Supported: Sharpness, Unbreaking, Protection, Looting, Fortune, Efficiency, Power, and 20+ more

**Particle Formation:**
```skript
set {_ampcircles::circle} to a circle of radius 0.75
set {_ampcircles::smallcircle} to a circle of radius 1
set {_ampcircles::smallcircle1} to a circle of radius 1.5
set {_ampcircles::smallcircle2} to a circle of radius 2
set {_ampcircles::smallcircle3} to a circle of radius 2.5
set {_ampcircles::mediumcircle} to a circle of radius 2
set particle of {_ampcircles::*} to dust particle using dustOption(rgb(0, 166, 44), 1.5) with force
```

**Animation:**
```skript
draw shapes {_ampcircles::smallcircle} at location
wait 1 tick
draw shapes {_ampcircles::smallcircle1} at location
draw shapes {_ampcircles::smallcircle2} at location
wait 1 tick
draw shapes {_ampcircles::smallcircle3} at location
draw shapes {_ampcircles::mediumcircle} at location
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (0, 166, 44) - Bright Green
- **Size:** 1.5
- **Animation:** Quick expansion from 1 to 2.5 blocks over 3 ticks
- **Pattern:** Indicates enchantment upgrade area

**Visual Effect:** Green expansion burst

**Code Location:** Base.sk

### TERTIARY: Unfortunate (Debuff)
**Activation:** Hit enemy with gem in hand
**Cooldown:** 3 minutes 20 seconds (1:40 with dragon egg)
**Duration:** 40 seconds

**Mechanics:**
1. 50% chance to cancel enemy's actions:
   - Attacks
   - Eating
   - Shooting
   - Block placing
2. Anvil sound effect on failed action
3. Green "Unfortunate" boss bar shows duration

**Particle Formation:**
```skript
loop 5 times: # 5 seconds
  loop 10 times: # 1 second
    set {_attacker} to location 0.5 metres above attacker
    set {_victim} to location 0.5 metres above victim
    set {_trail} to a line from {_victim} to {_attacker}
    set particle of {_trail} to dust particle using dustOption(rgb(0, 166, 44), 1.5) with force
    draw shapes {_trail} at {_victim}
    loop 2 times:
      remove 0.125 from bar progress
      wait 1 tick
```

**Particle Details:**
- **Type:** Dust line particles
- **RGB Color:** (0, 166, 44) - Bright Green
- **Size:** 1.5
- **Animation:** Energy beam from victim to attacker
- **Duration:** First 5 seconds of 40-second effect
- **Update Rate:** 10 times per second (50 total draws)

**Visual Effect:** Green curse energy drain beam

**Code Location:** Base.sk

### QUATERNARY: Item Lock (Weapon Disable)
**Activation:** Left-click with gem in hand
**Cooldown:** 3 minutes 20 seconds (1:40 with dragon egg)
**Range:** 2 blocks radius
**Duration:** 30 seconds

**Mechanics:**
1. Locks held item of all enemies within 2 blocks
2. Prevents: attacking, left-click, right-click with locked item
3. Green "Item Lock" boss bar shows duration
4. Does not lock gems (prismarine/amethyst shards)

**Particle Formation:**
```skript
set {_lockcircles::circle} to a circle of radius 0.75
set {_lockcircles::smallcircle} to a circle of radius 1
set {_lockcircles::smallcircle1} to a circle of radius 1.25
set {_lockcircles::smallcircle2} to a circle of radius 1.5
set {_lockcircles::smallcircle3} to a circle of radius 1.75
set {_lockcircles::mediumcircle} to a circle of radius 2
set particle of {_lockcircles::*} to dust particle using dustOption(rgb(0, 166, 44), 1.5) with force
```

**Animation:**
```skript
draw shapes {_lockcircles::circle} at location
draw shapes {_lockcircles::smallcircle} at location
wait 1 tick
draw shapes {_lockcircles::smallcircle1} at location
draw shapes {_lockcircles::smallcircle2} at location
wait 1 tick
draw shapes {_lockcircles::smallcircle3} at location
draw shapes {_lockcircles::mediumcircle} at location
```

**Particle Details:**
- **Type:** Dust particles
- **RGB Color:** (0, 166, 44) - Bright Green
- **Size:** 1.5
- **Animation:** Expands from 0.75 to 2 blocks over 3 ticks
- **Pattern:** Matches 2-block effect range

**Visual Effect:** Green lockdown expansion

**Code Location:** Base.sk

### PASSIVES (T2):
1. **Hero of the Village II** - Even better trades (upgraded from T1)
2. **Luck II** - Even better loot (upgraded from T1)
3. **Auto-Fortune III** - Every 59 seconds (max level)
4. **Auto-Looting III** - Every 59 seconds (max level)
5. **Auto-Mending** - Every 59 seconds on all tools/armor
6. **Double XP** - Same as T1
7. **Armor Durability Drain** - Same as T1
8. **Ancient Debris Conversion** - Same as T1
9. **Ore Bonus (Passive)** - Every 4th ore mined drops extra (25% bonus)
10. **Pockets Storage** - Same 9-slot inventory

### PASSIVE: Ore Bonus System (T2 Only)
**Mechanics:**
- Counter tracks ore breaks across all ore types
- Every 4th ore (when counter = 3) drops bonus ore
- Counter resets after bonus drop
- 25% bonus rate (1 extra per 4 mined)
- Stacks with Fortune
- Independent of Rich Rush ability

---

# PARTICLE COLOR REFERENCE

## Complete RGB Color Palette

**ASTRA:**
- Deep Purple: RGB(106, 11, 184)

**FIRE:**
- Bright Orange: RGB(255, 119, 0)

**FLUX:**
- Cyan/Electric Blue: RGB(94, 215, 255)
- Dark Cyan (Ground Stun): RGB(16, 131, 173)

**LIFE:**
- Pink/Magenta (Heart Lock): RGB(255, 0, 180)
- Pink (Circle of Life): RGB(255, 0, 179)

**PUFF:**
- Pure White: RGB(255, 255, 255)
- Cloud particles (native white, no RGB)

**SPEED:**
- Bright Yellow/Lime: RGB(244, 255, 28)
- Happy villager particles (green sparkles, native)

**STRENGTH:**
- Deep Red: RGB(199, 0, 10)

**WEALTH:**
- Bright Green: RGB(0, 166, 44)

---

# TECHNICAL IMPLEMENTATION NOTES

## Particle System Architecture

### Dust Particles
```skript
dustOption(rgb(R, G, B), size)
```
- Custom RGB color values
- Size parameter (typically 0.9-1.5)
- Force flag for render distance

### Color Transitions
```skript
dustTransition(rgb(R1, G1, B1), rgb(R2, G2, B2), size)
```
- Animates between two colors
- Only used in Flux gem charging
- Not available in vanilla Bukkit

### Spherical Vectors
```skript
spherical vector radius X, yaw Y, pitch Z
```
- Creates 3D circular patterns
- Used for Flux beam cross-sections
- Eliminates manual trigonometry

### Circle Shapes
```skript
set {_circle} to a circle of radius X
set particle of {_circle} to [particle type]
draw shapes {_circle} at [location]
```
- Pre-calculated circle points
- Can be drawn repeatedly
- Efficient for persistent effects

### Line Trails
```skript
set {_trail} to a line from {_loc1} to {_loc2}
set particle of {_trail} to [particle type]
draw shapes {_trail} at {_loc1}
```
- Connects two locations
- Used for energy beams
- Automatically calculates points

## Animation Techniques

### Expanding Circles
1. Create multiple circles with increasing radii
2. Draw 2 circles per tick
3. Wait 1 tick between draws
4. Creates smooth expansion effect
5. Used by: Astra, Fire, Life, Puff, Speed, Strength, Wealth

### Pulsing Lines
1. Calculate line between two points
2. Redraw line repeatedly over duration
3. Update frequency: 10 times per second
4. Used by: Life, Flux, Strength, Wealth

### Particle Trails
1. Spawn particles at player location
2. Use offset vectors for spread
3. Repeat every tick during movement
4. Used by: Puff (cloud trails)

### Beam Construction
1. Calculate beam path direction
2. Iterate along path in small increments (0.3m)
3. At each point, create circular cross-section using spherical vectors
4. Draw particles for each point in cross-section
5. Used by: Flux Ray (60 functions)

### Multi-Layer Effects
1. Draw same shape at multiple Y-levels
2. Creates dome or column effect
3. Used by: Astra Void (6-layer dome)

## Performance Considerations

### Particle Count Optimization
- Most abilities: 3-40 particles per draw
- Flux beam: 8 particles × 25 segments = 200 particles per beam
- Animation loops: Max 100 iterations for beam effects

### Update Frequency
- Expanding circles: 1 tick between draws (20 FPS)
- Line beams: 0.1 second updates (10 FPS)
- Passive trails: Every tick (20 FPS)

### Force Rendering
- All particles use `with force` flag
- Renders particles through blocks
- Ensures visibility at any distance
- Essential for multiplayer

## Cooldown System

### Time Tracking
```skript
{GemMin.%player%}  # Minutes
{GemSec.%player%}  # Seconds

every 1 second:
  remove 1 from {GemSec.%player%}
  if {GemSec.%player%} is -1:
    remove 1 from {GemMin.%player%}
    set {GemSec.%player%} to 59
  if {GemMin.%player%} is -1:
    set {gem.cooldown.%player%} to false
```

### Action Bar Display
- Updates every tick (20 times per second)
- Shows cooldown in MM:SS format
- Multiple abilities tracked simultaneously
- Uses color codes and emojis for visual clarity

## Energy System

### Energy Checks
```skript
if {Bliss.Energy.%player%} > 1:
  # Ability can be used
```

### Energy States
- 0: Broken (no abilities)
- 1: Ruined (abilities disabled)
- 2+: Abilities enabled
- 10: Maximum (Pristine)

### Dragon Egg Modifier
```skript
if {dragonegg.%player%} is not true:
  set cooldown to X minutes
else:
  set cooldown to X/2 minutes
```
- Reduces all cooldowns by ~50%
- Applied universally across all gems

## Boss Bar System

### Creation
```skript
set {Bar::%victim%} to boss bar with id "%player%ability" with title "&fName" with color red with progress 100
add victim to bossbar players of {Bar::%victim%}
```

### Duration Display
```skript
loop 600 times: # 30 seconds
  remove 0.167 from bar progress
  wait 1 tick
```
- 600 ticks = 30 seconds
- Progress: 100 / 600 = 0.167 per tick

### Cleanup
```skript
remove bossbar {Bar::%victim%}
delete {Bar::%victim%}
```

---

## Implementation Comparison: Skript vs Java

### Skript Strengths
1. **Rapid prototyping** - Test particle effects quickly
2. **Advanced particle addons** - Color transitions, spherical vectors, shapes
3. **Hot reload** - No compilation needed
4. **Natural language** - Easy to read and understand

### Skript Weaknesses
1. **Performance** - 10-100x slower than compiled code
2. **Single files** - 10K+ lines, hard to navigate
3. **Type safety** - No compile-time checking
4. **Addon dependencies** - 5+ external plugins required

### Java Implementation
1. **Performance** - Compiled bytecode, efficient
2. **Modular** - Separate files for each gem/system
3. **Type safety** - IDE catches errors
4. **Standalone** - No external dependencies except Paper API

### Recommended Approach
1. Use Skript examples as **visual reference**
2. Understand particle patterns and mechanics
3. **Implement in Java** with proper architecture
4. Optimize with caching and async tasks
5. Achieve similar visual effects with custom math

---

## Summary Statistics

### Total Abilities Across All Gems
- **Tier 1:** 8 primary abilities + 16 passive effects
- **Tier 2:** 24 active abilities + 24 passive effects
- **Total:** 32 active + 40 passive = 72 total abilities

### Particle Type Usage
- **Dust particles:** 7 gems (Astra, Fire, Flux, Life, Puff, Speed, Strength, Wealth)
- **Cloud particles:** 1 gem (Puff)
- **Sonic boom:** 1 gem (Flux beams)
- **Happy villager:** 1 gem (Speed storm)
- **Native particles:** Fire (fireballs), Speed (lightning)

### Cooldown Ranges
- **Shortest:** 30 seconds (Flux Static Burst T1)
- **Longest:** 9 minutes 59 seconds (Wealth abilities T2)
- **Average:** ~2 minutes per ability

### Particle Complexity
- **Simplest:** Fire Crisp (expanding circles)
- **Most Complex:** Flux Ray (60 functions, animated beam with tapering)

### Animation Techniques
1. Expanding circles: 8 gems
2. Line beams: 4 gems (Life, Flux, Strength, Wealth)
3. Particle trails: 1 gem (Puff)
4. Multi-layer: 1 gem (Astra)
5. Lightning strikes: 1 gem (Speed)

---

**End of Complete Gem Analysis**

This document provides comprehensive information about every gem's abilities, particle effects with exact RGB values, animation patterns, cooldowns, and implementation details from the Skript files in `researches/particles example/`.

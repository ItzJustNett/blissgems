# BlissSMP Plugin: Complete Reference Guide

The Bliss SMP runs on a **closed-source custom plugin** that grants every player a random superpowered gem upon joining, creating a competitive survival experience built around **8 gem types**, a unique **energy-based life system**, and a seasonal **Progression system** that gradually unlocks gear and mechanics. This reference covers every documented mechanic, gem power, item, and system as of Season 3 (the longest and current season, which began February 17, 2024).

---

## The energy system governs life and gem power

Energy is the core life mechanic that determines how strong a player's gem is — and whether it works at all. Players start at **Pristine (5 energy)** and lose 1 energy each time they die, while gaining 1 energy each time they kill another player. The system spans **11 energy states** across a 0–10 scale:

| Energy | State | Effect on Gem |
|--------|-------|---------------|
| 10 | Pristine +5 | Maximum. Gem gains **enchantment glint**. Kills drop victim's energy as an Energy Bottle rather than being absorbed directly. |
| 9 | Pristine +4 | Full power |
| 8 | Pristine +3 | Full power |
| 7 | Pristine +2 | Full power |
| 6 | Pristine +1 | Full power |
| **5** | **Pristine** | **Starting energy** — 100% gem power |
| 4 | Scratched | Powers begin weakening |
| 3 | Cracked | Powers weaker, some passives lost |
| 2 | Shattered | Significantly reduced power |
| 1 | Ruined | **All passives disabled** |
| 0 | **Broken** | **All powers and abilities disabled**. Player takes **double damage from gem abilities**. Cannot absorb Energy Bottles. |

In Season 3, there is **no deathban** for dying with a Broken gem — the player simply stays Broken until a Restoration Ritual is performed. This is a major change from Season 2, where dying at Broken resulted in a server ban.

**The Energy Pool** is a server-wide reserve of energy. All energy not currently held by players or bottled exists in this pool (viewable via `/bliss pool`). Damaged gems can be repaired at the Pedestal by consuming pool energy through a Repair Kit (up to 10 energy per ritual). If the pool runs too low, Repair Kits become useless. Broken gems **cannot** be repaired via Repair Kits — they require a full Restoration Ritual, which does **not** draw from the pool.

---

## All eight standard gems and their complete abilities

Each gem has **two tiers**: Tier 1 (default, diamond/rhombus shape) grants passives and a primary power, while Tier 2 (upgraded via an Upgrader item, hexagonal shape) unlocks additional abilities. Tier 2 players gain a **permanent Glowing effect** and their name appears **yellow on the tablist**. Passives only activate when the gem is held in the offhand or mainhand.

### Astra Gem — dimensional stealth and ranged daggers

**Passives (Tier 1):** Soul Capture (stores a recently killed mob for later release), Soul Healing, and ~~Phasing (15% chance to negate incoming damage)~~ — **Phasing was removed** mid-Progression 4 as a balance nerf.

**Abilities (Tier 2):** **Astral Daggers** fire ranged projectiles that deal damage. **Dimensional Drift** summons an invisible horse the player rides while also becoming invisible. **Dimensional Void** nullifies all enemy gem abilities within a radius. **Unbounded** grants temporary spectator mode. **Astral Projection** enables scouting with sub-abilities **Spook** and **Tag**. Astral Projecting to 35k in the Y-direction still counts as "flying" and requires landing when encountering combat.

### Fire Gem — explosive fireballs and area denial

**Passives (Tier 1):** Permanent **Fire Resistance**, **Auto-Smelt** (ores automatically smelt when mined), and **Auto-Enchant Fire Aspect** on melee weapons. Historically, Flame Strike and Fire Shot were combined into a single passive called "Firethorns" before being separated.

**Abilities (Tier 2):** **Fireball** requires charging — the charge meter rises then decays if not released, but **standing on obsidian prevents charge decay**. Cooldown is approximately **60 seconds** (halved to 30s with Dragon Egg). **Cosy Campfire** grants allies **Regeneration IV** within a radius, making it a critical synergy for Flux Gem overcharging. **Crisp** evaporates all water in range (including water placed *after* activation) and replaces surrounding blocks with unbreakable nether blocks. **Meteor Shower** rains fire on a target area.

### Flux Gem — electrical beams and charge mechanics (new in Season 3)

**Passives (Tier 1):** **Flow State** progressively increases speed and attack speed the longer you run or fight, stacking up to 5 levels and resetting when you stop. **Shocking Chance** provides ~15% chance to stun enemies on arrow hits for ~1 second. **Tireless** grants complete immunity to Weakness, Slowness, and Hunger effects — this passive was so powerful that combining it with Tier II Turtle Master potions (negating the slowness penalty while keeping massive damage resistance) caused the server to **temporarily ban Tier II Turtle Master potions**. **Conduction** allows teleporting to nearby copper blocks via Sneak + Left Click. **Charged** enables charging beams to 100% and beyond. The gem also grants **immunity to Charged Creepers**.

**Abilities (Tier 2):** **Flux Beam** fires a powerful ray-traced beam. Uncharged, it deals ~3 hearts through full Protection IV armor. Charged to **100%** using valuable items (Diamond Blocks, enchanted diamond gear, Gold Blocks, Copper Blocks), a direct beam deals **~200 durability damage** to all armor pieces. When used on trusted allies, offensive abilities **restore armor durability** instead of dealing damage. **Flashbang** applies Blindness and Nausea to enemies in radius. **Ground** freezes enemies; when overcharged, also disables jumping. **Kinetic Burst** creates radial knockback with a sonic boom effect.

**Overcharging (200%) mechanic:** After reaching 100% charge, the gem begins overcharging toward 200% for an even more devastating beam capable of **one-shotting groups of players**. During overcharging, the user takes **continuous damage every second**. The Fire Gem's Cosy Campfire (Regen IV) was one of the few viable survival strategies. **Overcharging was disabled in Progression 4** — the warning still appears, but charge cannot exceed 100W. The Flux Gem originally had a Tier 1 ability called **Static Burst** (stored damage taken over 2 minutes, then released at once) that was removed very early in the season.

### Life Gem — health manipulation for allies and enemies

**Passives (Tier 1):** **Auto-Enchant Unbreaking** on tools. The **Double Saturation** passive (increased saturation from eating) was **removed for balance reasons**.

**Abilities (Tier 2):** **Health Drain** siphons health from enemies. **Vitality Vortex** grants effects based on surroundings. **Life Circle** creates a radius that **decreases max health of untrusted players** while **increasing max health of the user and trusted players**. **Heart Lock** temporarily caps an enemy's max health at their current health — if they're at 5 hearts, their max becomes 5 hearts for the duration.

### Puff Gem — aerial mobility and fall damage immunity

**Passives (Tier 1):** Complete **fall damage immunity**, **Auto-Enchant Power** and **Punch** on bows, **Sculk Silence** (immunity to triggering sculk shriekers), and **crop-trample immunity**.

**Abilities (Tier 2):** **Double Jump** allows midair jumps. **Dash** propels the user forward rapidly, dealing damage if passing through an opponent. **Breezy Bash** launches a single enemy skyward then slams them into the ground. **Group Breezy Bash** sends all untrusted players flying away from the user.

### Speed Gem — lightning strikes and velocity boosts

**Passives (Tier 1):** Permanent **Speed I** effect (originally Speed II, later nerfed).

**Abilities (Tier 2):** **Blur** summons successive lightning strikes dealing damage and knockback. **Speed Storm** creates a field that **freezes enemies** while granting allies Speed and Haste. **Terminal Velocity** provides **Speed III + Haste II for 9–10 seconds**. The Speed Gem was **vaulted in Progression 4** due to damage output issues but was later unvaulted.

### Strength Gem — raw combat power and player tracking

**Passives (Tier 1):** Permanent **Strength I** (supposed to become Strength II in Prog 4, but reportedly not fully implemented), **Auto-Enchant Sharpness** (Sharpness II for Tier 1, Sharpness V for Tier 2), and **Bloodthorns** (deal more damage the lower your health).

**Abilities (Tier 2):** **Nullify** strips opponents of potion effects. **Frailer** clears enemy potions and applies **Weakness I for 20 seconds** plus **Withering for 40 seconds**. **Bounty Hunting / Shadow Stalker** consumes an item crafted/stored/stolen by another player to track their location for a limited duration, with a directional indicator. Tracking mechanics have been extensively revised: items with any ownership history (accessible via `/history`) and **player heads** (Progression 4 addition) are now valid inputs. Trusted players can now see the directional indicator. Invisibility potions used to grant complete tracking immunity but were **nerfed in Progression 5** — invisible players can be tracked within a **~2,500-block radius**, and victims receive the hotbar notification much earlier. **Chad Strength** deals bonus damage on every 4th hit (~3.5 hearts).

### Wealth Gem — economic advantage and item control

**Passives (Tier 1):** **Auto-Enchant Mending**, **Fortune**, and **Looting** on tools. Permanent **Luck** and **Hero of the Village** effects. **Durability Chip** deals extra armor damage per strike. **Armor Mend** restores user's armor durability per strike on opponents. **Double Debris** doubles netherite scrap yield from furnaces.

**Abilities (Tier 2):** **Pockets** opens a GUI with **9 extra inventory slots**. **Unfortunate** has a chance to disable opposing players' actions (attacks, block placements, eating) for a duration. **Item Lock** prevents an enemy from using a specific item temporarily. **Amplification** strengthens all enchantments on tools and armor for **45 seconds** with a **3-minute cooldown**. **Rich Rush** increases mob and ore drop rates for approximately **3 minutes** with an approximately **9-minute cooldown**.

---

## Special and event gems beyond the standard eight

**Gold Gem (Season 2 Finale):** Created by collecting **7 Wire Fragments** from meteorites summoned through a scavenger hunt involving the 7 Deadly Sins, orchestrated by "The Prophet." The Gold Gem possessed a devastating beam power (resembling the later Flux Beam) and could **steal the effects and powers of other gem types** upon killing their holder. It could only change hands upon its holder's death. The server would close permanently once all players agreed on a keeper.

**Auratus Gem and Heretic Gem (Season 3, Progression 5):** Two **mythic event gems** introduced during "The Assessment" / Conexion event. The Heretic Gem was initially held by Sharpness (who had no prior knowledge of what the gem did and died ~20 minutes into the fight to Mugm). Dol9hin obtained the Auratus Gem. Ferre later regained the Heretic Gem. Detailed ability documentation for these gems does not exist on the wikis — they are intentionally rare and mysterious.

---

## Every custom item, from gem tools to mythic weapons

### Gem items that interact with the power system

**Upgraders** promote a gem from Tier 1 to Tier 2. During Progression 2, only **3 existed** on the server (dropped by the custom Wither boss alongside 2 Revive Books and 8 Traders). They became craftable in Progression 3 with: 4 Gem Fragments, 1 Wither Skull, 4 Dragon's Breath.

**Traders** randomize a player's gem type. In Season 2, they enabled a combat exploit called **"cycling"** — rapidly switching between gem types to chain multiple abilities. Season 3 removed cycling by adding a long animation between trades and introduced two new trade types: switching gems with a trusted player, and sacrificing energy to choose or improve the odds of rolling a specific type. Season 3 recipe: 4 Dragon's Breath, 4 Diamond Blocks, 1 Sculk Catalyst.

**Energy Bottles** contain 1 energy unit. They drop when a Pristine +5 player kills someone, or when a player uses `/bliss withdraw`. Right-clicking absorbs the energy (blocked if player is at Pristine +5 or Broken). Bottles **expire within 1 hour** with an audible warning sound; all expired energy returns to the Energy Pool automatically.

**Energy Tokens** are a non-expiring currency created by left-clicking an Energy Bottle. They appear light green (versus aqua for bottles) and are used exclusively for custom villager trades — they cannot be converted back to energy.

**Repair Kits** power the Repair Ritual at the Pedestal. They restore damaged gems to higher energy states (capped at Pristine) by depleting the Pool by up to 10. Multiple players can be repaired simultaneously, prioritizing the lowest-energy player. The ritual stops if all reach Pristine, 10 energy is consumed, or the Pedestal is broken. Cannot repair Broken gems. Season 3 recipe: 4 Gem Fragments, 2 Anvils, 2 Netherite Ingots, 1 Netherite Upgrade Template.

**Gem Fragments** are a crafting component used in Repair Kits, Upgraders, Traders, and Restoration Cores. Recipe: 4 Diamonds, 2 Amethyst Clusters, 2 Emeralds, 1 Iron Block.

**Restoration Items** are the only way to fix a Broken gem. The ritual at the Pedestal requires: 1 Restoration Item, 1 Beacon, 1 Block of Ancient Debris, and 5 Energy Bottles. The result returns the gem to Pristine with a **randomly rolled gem type**. Does not consume Pool energy. In Season 2, only one Restoration Item existed on the entire server. Season 3 introduced a **two-step crafting process**: first craft a Restoration Core (4 Gem Fragments, 2 Dragon Heads, 2 Repair Kits, 1 Recovery Compass — appears as a nether star), then combine it with 8 specifically dyed candles to create the Restoration Item.

### Mythic and event items in Season 3

**Dragon Egg** became available after the Progression 3 End Fight. When held, it grants **+10 hearts** (total of 20) and **halves all gem cooldowns** (e.g., Fireball drops from 60s to 30s). It has been a central source of server conflict. Mugm duped the egg and stashed the original.

**Netherite Gear** (1 full set: Helmet, Chestplate, Leggings, Boots, Sword, Axe) was obtainable only through custom villager Energy Token trades at **4 tokens per armor piece** and **4 tokens per weapon**. Crafting netherite was entirely disabled by the plugin. When the plugin broke, Whatmax and Ryanstuff exploited the glitch to craft their own set, creating 4 total known sets. Admins placed **Curse of Vanishing** on the 3 crafted sets; 2 were subsequently destroyed. Mugm later duped the armor, stashing originals and wearing copies.

**Three Maces** were introduced in Progression 4 at a cost of **8 Energy Tokens each** from custom villagers. They have special kit limits: **all damage enchantments are disallowed**, with Wind Burst permitted at varying levels (currently capped at **Wind Burst III**). When all 3 maces are collected, they can be combined to craft the **"Mega-Mace"**, which has **no cooldown and no enchantment limits**.

**Enchanted Obsidian** (Progression 1): 8 pieces released one-by-one across the world by admins. Holders gained the Glowing effect and yellow tablist name. Once all 8 were placed in an Ancient City portal frame, the custom Nether portal activated. Admins enforced a rule requiring holders to **log in for minimum 1 hour daily** to prevent indefinite stalling.

**Villager Souls** resembled enchanted pitcher pod seeds. Dropped by killing custom villagers, they could be right-clicked to respawn the villager in front of the player. Trackable via a compass structure at the Custom Village (x: -1440, z: -440), where colored particle lines (pink, gray, magenta) pointed to each villager/soul location, with each block of line representing a minimum of 500 blocks distance. Disappeared after the Progression 4 event concluded.

**Eye of Nether** served as a substitute crafting ingredient for End Crystals and Ender Chests during early progressions when Ender Pearls were unobtainable. Both Ender Pearls and Eyes of Nether now work in these recipes.

**Season 2 Border Items**: 9 special enchanted items hidden in chests that could individually expand or shrink the world border, or combine in a crafting recipe to permanently alter its dimensions. Players exploited the naming system (guessing special item names to trick the plugin) until admins randomized the names.

---

## Plugin commands, systems, and technical mechanics

### Known commands

| Command | Access | Function |
|---------|--------|----------|
| `/bliss withdraw` | All | Voluntarily removes 1 energy from gem, drops an Energy Bottle |
| `/bliss pool` | All | Displays current server-wide Energy Pool amount |
| `/history` | All | Shows item ownership/crafting history (used for Strength Gem tracking) |
| `/gems` | OP | Gives any gem or Bliss plugin item |
| `/cooldown <player>` | OP | Resets all cooldowns for a player |
| `/disable <player>` | OP | Globally disables gem abilities or disables for specific player |
| `/progression1/2/3` | OP | Starts the respective Progression phase |
| `/setenergy <player>` | OP | Sets a player's energy level and updates gem state |
| `/pedestal <set\|activate\|deactivate\|remove>` | OP | Manages the Pedestal structure for rituals |
| `/random <player\|all>` | OP | Rolls a random gem for a specific player or all online players |

### The Progression system structures Season 3

Season 3 uses a **Progression system** (Progs 1–5) that gates content in a game-progression order: Overworld → Nether → End → Villagers. Each progression modifies kit limits, available items, and mechanics.

**Progression 1:** Players limited to plain diamond armor. No obsidian, no lava, no villagers, no enchanting tables. Enchants only from loot chests. Maximum Protection I. Firework crossbows dominated combat. To advance, players collected 8 Enchanted Obsidian pieces, built the Custom Nether Portal in an Ancient City, and defeated a custom Wither boss.

**Progression 2:** Nether unlocked. The Wither boss dropped **4 Upgraders, 2 Revive Books, and 8 Traders**. Gem items exist but remain uncraftable. Kit cap: Protection III, Sharpness III, Tier 1 potions (Strength/Speed I).

**Progression 3:** End unlocked. Dragon Egg obtainable. Upgraders become craftable. Chorus fruit limited to **16 during combat**. Kit cap: Protection III, **Sharpness V**, Tier 1 potions. This progression alone was **longer than the entirety of Season 2** by 2 days.

**Progression 4:** Villager trades enabled. Chorus fruit limit removed. Infinite restocks allowed (except Turtle Master with Resistance IV, limited to 4 per fight with no restocking). Border expanded from 6k to 12k, later to 30k. Game updated to **Minecraft 1.21**. Three maces and 1 netherite set introduced. Kit cap: Protection III, Sharpness III, **Tier 2 potions** (Strength II). No 200% Flux beams. Speed Gem vaulted then unvaulted.

**Progression 5 (Current):** Heretic and Auratus mythic gems added during The Assessment event. Two "Assassin" Juggernauts introduced. Rule: Astral Projecting to 35k Y still counts as flying and requires landing in combat.

### Season 3 server rules

The server enforces consistent combat rules: no combat logging, no sitting on the respawn screen for over 1 minute, no End Crystals or Respawn Anchors in combat, no TNT minecarts in direct combat (traps allowed), no Riptide tridents, no elytra in combat, no using ice in combat (added after Prog 2), IOUs are server-enforced, no stream sniping without permission, and no abusing gem cooldown glitches. Ender pearl stasis chambers have been conditionally allowed.

### Custom advancements and auto-enchant system

The plugin features **13 custom Minecraft advancements** relating to gems and gem items. The specific names and unlock conditions are documented on the wiki but were not fully extractable from available sources.

The **auto-enchant system** automatically applies gem-specific enchantments to held items: Fire Gem adds Fire Aspect, Wealth Gem adds Mending/Fortune/Looting, Strength Gem adds Sharpness (II for Tier 1, V for Tier 2), Puff Gem adds Power and Punch to bows, and Life Gem adds Unbreaking. These enchantments apply passively while the gem is held and only function above the Ruined energy state.

### Trusted player mechanics

Many abilities distinguish between **trusted and untrusted players**. Trusted players receive beneficial effects from abilities (e.g., Flux Beam repairs armor durability on trusted allies instead of dealing damage, Cosy Campfire heals trusted allies, Life Circle boosts trusted allies' max health). Untrusted players receive the harmful versions. The trust system is foundational to team-based strategies across the server.

---

## How seasons evolved the gem ecosystem

Season 1 started with **6 gems** (Fire, Life, Speed, Strength, Wealth, Puff) and used a heart system similar to Lifesteal SMP rather than energy. Season 2 introduced the **energy-based life system**, added the **Astra Gem** (7 total), and featured deathbans at Broken. Season 3 added the **Flux Gem** (8 standard gems), removed deathbans entirely, reworked every returning gem, and introduced the Progression system that transformed gameplay pacing. Each season has added exactly **1 new powerful gem** to the plugin, with Progression 5 bringing the mythic Auratus and Heretic Gems as event-exclusive additions.

The plugin remains **closed-source and developed exclusively for Bliss**, though third-party recreation plugins exist on platforms like BuiltByBit and Modrinth. These recreations approximate the mechanics but do not perfectly replicate the actual server's balance values, which are frequently adjusted — particularly cooldowns, damage numbers, and kit limits — throughout each Progression cycle.

## Conclusion

The BlissSMP plugin creates a deeply interconnected system where gem choice, energy management, item economy, and the Progression timeline all feed into one another. The energy system's most distinctive feature is its **dual function as both a life counter and a power scaler** — as players lose lives, their abilities weaken proportionally, creating a death spiral that only Repair and Restoration Rituals can reverse. The Progression system's gatekeeping of items and enchants means that the meta shifts dramatically across each phase, forcing adaptation. Key strategic considerations include gem synergies (Fire's Cosy Campfire enabling Flux Beam overcharging), the Wealth Gem's economic dominance in resource gathering, the Strength Gem's evolving tracking mechanics, and the overwhelming power of mythic items like the Dragon Egg and Mega-Mace that reshape the competitive landscape whenever they change hands.
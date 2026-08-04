# BlissGems v4.9.0 - Prismatic Edge & Restoration Update

### ⚔️ New Legendary Item: Prismatic Edge
- **Prismatic Edge (`prismatic_edge`)**: Legendary sword with built-in **Sharpness VII**, **Mending**, and **Unbreaking III**.
- **Prismatic Beam (Right-Click)**: Shoots a 24-block rainbow beam that freezes targets solid, deals 10 HP (5 hearts) damage, and grants Regeneration II to the wielder (120s cooldown).
- **Combo Crits**: Hitting enemies 5 times in a row without getting hit back enters a Combo state where **every hit crits** (1.5x damage) until you take damage.
- Custom texture added to Resource Pack V5.2.
- Clean Minecraft chat text without em-dash rendering issues.

---

### 📖 Restoration Book & Ritual
- **Restoration Book (`restoration_book`)**: Craftable using Gem Fragments, Totems of Undying, Echo Shards, a Nether Star, and a Book.
- **Restoration Ritual**: Right-clicking while holding a BROKEN gem triggers a server-wide weather ritual (thunderstorm & lightning sequence) that re-forges and re-rolls your broken gem back to Pristine state!

---

### 🌩️ Speed Gem: Gale Clouds (Quaternary Ability)
- **Gale Clouds**: Triggered via `Shift + F` (`quaternary` ability).
- Throws 3 cloud projectiles that apply **Slowness II** to enemies and put their **Wind Charges on cooldown**.

---

### 🗡️ Combat Mechanics
- **Broken Gem Punish**: Attacking a player whose gem is **BROKEN** (energy 0) now deals **1.5x bonus damage**.

---

### 🐛 Bug Fixes & Improvements
- **Passive Effect Activation Gate Fix**: Fixed an issue where passives were inactive at Energy Stage 1 (Ruined). Passives are now active for energy stages 1 to 10 and only disable when a gem is completely BROKEN.
- **Blur Ability NaN Crash Fix**: Fixed a server crash (`java.lang.IllegalArgumentException: x not finite`) when normalizing zero-length vectors during Blur strikes.
- **Config Enhancements**: Exposed `abilities.blur` settings in `config.yml`. Added global broadcasts for Repair Kit uses.

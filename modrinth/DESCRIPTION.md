<div align="center">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/banner_main.png" width="100%">

**Every player gets one gem. Every gem has its own powers. Lose your energy and you lose them.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-brightgreen?style=for-the-badge)
![Server](https://img.shields.io/badge/Paper-Folia_not_supported-blue?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-17+-orange?style=for-the-badge)

**Resource pack required.** Download it in our Discord: https://discord.gg/egg4X2yHVu

</div>

---

## How it works

Every player joins with a random gem. The gem gives passives while it is in your hand or hotbar, and powers you trigger with clicks and keybinds.

Gems run on **energy**. You start at Pristine (5). Kill a player and you gain one, die and you lose one. As energy drops the gem gets weaker: first the powers fade, then the passives, and at 0 the gem is **Broken**. A Broken gem can only be reforged with a Restoration Ritual on the Pedestal.

Each gem has two tiers. Tier 1 gives the passives and the first power. An **Upgrader** raises it to Tier 2 and unlocks the rest.

---

<div align="center">
<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/header_gems.png" width="100%">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_astra.png" width="49%"> <img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_fire.png" width="49%">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_flux.png" width="49%"> <img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_life.png" width="49%">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_puff.png" width="49%"> <img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_speed.png" width="49%">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_strength.png" width="49%"> <img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_wealth.png" width="49%">
</div>

---

<div align="center">
<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/header_gold.png" width="100%">

<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/gem_gold.png" width="70%">
</div>

The Gold Gem is one of a kind. Its holder harvests the gem souls of the players they defeat and channels their powers, one soul at a time. Gather all eight and the gem awakens. Its own power, the Sundering Beam, charges in plain sight and hits for massive damage.

---

<div align="center">
<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/header_events.png" width="100%">
</div>

**The Pedestal.** A ritual structure at spawn. Throw a Repair Kit onto it to start a Repair Ritual, or a Restoration Book while your gem is Broken to reforge it. Both need Energy Bottles thrown in to finish, and breaking the beacon during a ritual ends it.

**The Villager Event.** A disc plays the way to a lost village. Three villagers trade there, but their souls are scattered: hunt the souls, return them to the village compass, and survive the Last Raid. Their one-of-one trades open only then.

**The Golden Dream.** Gather the seven wire fragments of the Gold Gem's core and the dream pulls you in: a walk through the void, then a grey world of memories.

---

<div align="center">
<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/header_items.png" width="100%">
</div>

| Item | What it does |
|---|---|
| **Trader** | Swaps your gem for a random different one |
| **Upgrader** | Raises your gem to Tier 2 (single use) |
| **Energy Bottle** | +1 energy. Withdraw your own with `/bliss withdraw` |
| **Repair Kit** | Starts a Repair Ritual on the Pedestal |
| **Restoration Book** | Reforges a Broken gem through the Pedestal ritual |

---

<div align="center">
<img src="https://raw.githubusercontent.com/ItzJustNett/blissgems/refs/heads/bliss/V5.1.5/modrinth/images/header_commands.png" width="100%">
</div>

**Players**
- `/bliss gui` – your gem, energy and cooldowns
- `/bliss trust <player>` / `untrust` / `trusted` – your powers never hit trusted players
- `/bliss withdraw` – bottle your energy
- `/startcharging` / `/stopcharging` – Flux Tier 2 beam charge
- `/news` – server news, respawn timers and repair windows

**Admins** (`blissgems.admin`)
- `/bliss give <player> <gem> [tier]`, `/bliss giveitem`, `/bliss energy`, `/bliss reroll`, `/bliss reload`
- `/blissevent` – the Villager Event (compass, villagers, phases)
- `/goldendream` – the Golden Dream (ritual, worlds, memories)
- `/fixhearts`, `/fixedhearts` – reset stuck max-health

Every cooldown, damage value and duration lives in `config.yml`.

---

### Credits

Gem designs and item text are based on **BlissPlugin by Blood**, used with his permission, which itself builds on the original **Bliss SMP Skript by rar**.

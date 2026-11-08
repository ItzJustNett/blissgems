# BlissGems - Trailer Script (~90s)

Format: 16:9, 1080p60. Shaders on (Complementary/BSL), Replay Mod for cinematic shots.
Music: one epic/electronic track, drop at 0:20 and at 1:05.
VO = voiceover. TEXT = on-screen title.

---

## 0:00 - 0:08 | Cold open

**Shot:** Black screen. Heartbeat sound. Fade in: player in a dark cave, a purple gem floating
in the hotbar. Slow push-in on the gem item (Astra) in the off hand.

**VO:** "One gem. One life. Everything to lose."

**TEXT:** `BLISSGEMS`

---

## 0:08 - 0:20 | The hook: first join

**Shot:** Player joins a fresh world. Gem roll animation, chat shows the received gem.
Quick cuts of 8 gem icons spinning past (Astra, Fire, Flux, Life, Puff, Speed, Strength, Wealth).

**VO:** "Every player gets a random gem. Eight gems. Each with its own powers."

**TEXT:** `8 GEMS. 1 RANDOM ROLL.`

---

## 0:20 - 0:55 | Gem montage (music drop)

~4 seconds per gem, one signature ability each. Gem name in its color, bottom-left.

| Time | Gem | Shot | TEXT |
|---|---|---|---|
| 0:20 | **Fire** | Charge bar fills in action bar, Charged Fireball blows a hole in a base wall. Cut: Meteor Shower over a field. | `FIRE - Meteor Shower` |
| 0:24 | **Flux** | Charge to 100%, cyan beam rips through an armored player, armor durability drops. | `FLUX - Flux Beam` |
| 0:28 | **Astra** | 3 purple daggers fan out. Then Dimensional Void dome: enemy tries ability, nothing happens. | `ASTRA - Dimensional Void` |
| 0:32 | **Puff** | Dash + Breezy Bash straight up, double jump at the peak, land from build height with no damage. | `PUFF - No fall damage. Ever.` |
| 0:36 | **Speed** | Speed Storm, sparks trail, player outruns a horse. Cut: Sloth's Sedative freezes a group. | `SPEED - Speed Storm` |
| 0:40 | **Strength** | Chad Strength buff, Bloodthorns AoE knocks 3 players back. | `STRENGTH - Bloodthorns` |
| 0:44 | **Life** | Pink Circle of Life: owner regens, enemies inside wither. | `LIFE - Circle of Life` |
| 0:48 | **Wealth** | Rich Rush mining with Haste III, then `/bliss pockets` opens extra inventory. | `WEALTH - Rich Rush` |

**0:52 - 0:55:** Fast 0.5s flashes of all 8 particle colors in sequence, ends on white flash.

---

## 0:55 - 1:10 | Energy system (the stakes)

**Shot:** PvP duel in the rain. Loser dies, energy bar on screen ticks down (5 -> 4).
Gem lore changes: `Pristine` -> `Scratched` -> `Cracked` (quick cuts).
Final cut: gem at `Broken`, abilities greyed out, player looks at it.

**VO:** "Kill to gain energy. Die and your gem cracks. At zero, it's broken."

**TEXT:** `KILL = +1 ENERGY` / `DIE = -1 ENERGY`

**Shot:** Winner hits 10 energy, gem gets enchant glint. Kills now drop Energy Bottles.

**TEXT:** `PRISTINE +5`

---

## 1:10 - 1:22 | Progression (second drop)

Quick cuts, ~2s each:
1. **Upgrader:** crafting it, gem goes Tier 1 -> Tier 2, player gets Glowing, name turns yellow in tab.
2. **Repair Kit at the Pedestal:** damaged gem restored from the server energy pool.
3. **Restoration Ritual:** broken gem brought back, big particle ritual.
4. **Rarity roll:** chat shows `LEGENDARY`, then a `MUTATED` lore line.
5. **Tier 3 + `/bliss imbue`:** donor gem consumed, passives merged.

**VO:** "Upgrade it. Repair it. Restore it. Mutate it."

**TEXT:** `TIER 1 -> TIER 2 -> TIER 3`

---

## 1:22 - 1:30 | Outro

**Shot:** Wide drone shot over a battle with 6+ players, all 8 gem particle colors flying.
Slow-mo on a Meteor Shower hit, freeze frame.

**VO:** "Choose nothing. Earn everything."

**TEXT:** `BLISSGEMS`
`Paper / Spigot 1.21`
`Download on Modrinth` (+ link/QR)

---

## Shot list / prep checklist

- [ ] Test world: flat arena, a small base to blow up, rain + night preset
- [ ] 6-8 accounts/bots for group fights (Bloodthorns, Sedative, Void, final battle)
- [ ] Set energy manually per scene (5, 4, 3, 0, 10) for the energy section
- [ ] Cooldowns to 0 in config while recording, revert after
- [ ] Resource pack on (gem models), GUI scale 2, hide HUD except action bar shots
- [ ] Record each gem ability 3-4 takes from different angles

## Notes

- Keep VO under 15 words per line, the trailer is music-driven.
- If no voiceover: use only TEXT lines, they carry the story alone.
- Ability names match `ABILITIES.md`; if anything is renamed, update TEXT cards.

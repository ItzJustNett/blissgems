# Changelog

## BlissGems 4.9.8

### Added

**The harvest ceremony.** The blow that would kill a gem holder is now caught before it lands.
Both players are pinned where they stand and made untouchable, the victim's gem tears loose and
rises turning in the air between them, both cameras are dragged onto it with a slow sway, and
each screen washes over in that gem's colour. When the gem finishes rising it goes into the Gold
Gem and the death goes through, still credited to the holder. Kills the ceremony never sees — a
fall finishing the fight, `/kill`, or `gold.harvest-ceremony.enabled: false` — harvest the old
way with no animation. Timing, rise height and the screen tint are all configurable.

**Stolen passives actually fire.** Harvesting a gem used to hand over only its potion-based
passives; everything driven by an event — Puff's double jump, Wealth's durability chip and armor
mend, Strength's bloodthorns, Astra's, Life's and Flux's on-hit passives — silently did nothing.
All of them now run for the Gold Gem's holder, and they stack: four harvested souls means four
sets of passives at once. Each runs at the tier its gem was taken at, so a soul torn off a Tier 1
victim keeps Tier 1 numbers no matter how awake the gem carrying it is.

**Two more ability inputs.** `AbilitySlot` gained Quinary and Senary, bound by default to
left-click and shift+left-click. Only the Gold Gem defines them — it uses them for the channelled
soul's tertiary and quaternary, so all four of a soul's abilities are reachable instead of two,
with the Sundering Beam and the soul menu still on F and shift+F. Every other gem leaves both
slots undefined, and left-click keeps its vanilla behaviour for them. Also available as
`/bliss ability:quinary` and `ability:senary`.

**Every craft moved into `recipes.yml`.** Shape, ingredients, output amount and an on/off switch
for each of the nine recipes, no code change needed. Ingredients are either a plain material or
`blissgems:<item_id>` for an exact custom-item match — which is how the Gold Gem summon refuses
to accept seven ordinary lightning rods. The old `crafting.enabled` and `gold.summon.recipe-enabled`
switches still work.

**The Gold Gem is a one-time event.** With `gold.summon.once-per-server` on, the summon craft
works exactly once per server; afterwards the result blanks out in the crafting grid. The summon
is announced server-wide and recorded in `gold.yml`.

**Gilded armour.** Carrying the Gold Gem puts a gold trim on the holder's armour. Pieces the
player already trimmed themselves are left alone, and the plugin's own gilding is marked so
removing it can never strip someone's smithing-table work.

### Changed

**Dying costs the Gold Gem everything.** Every stolen gem now goes back to the player it was
taken from — straight into their offhand if they're online, queued to their playerdata and handed
over on their next join if they aren't — and the holder is left with a Broken gem that does
nothing until a Restoration Book reforges it. Both halves are switchable
(`gold.death.return-souls`, `gold.death.break-gem`).

**A restoration ritual can no longer be interrupted.** The caster is untouchable and rooted to
the spot for the fifteen seconds it runs, but keeps full control of their head — standing still
for a quarter of a minute was otherwise an open invitation.

**The cooldown bar shows every harvested soul.** The channelled one opens out into the abilities
it can actually cast right now; the rest collapse to their gem icon and their primary's cooldown,
so it's readable at a glance which soul is worth switching to mid-fight.

**`gems.droppable-on-death` is documented in `config.yml`.** The setting already worked; it was
only ever readable from the source.

---

## BlissGems 4.9.1

### Changed

**Conduction is now `/bliss conduction`.** The sneak + left-click trigger is gone — the copper
teleport runs from the command instead, so it can't fire by accident while mining. It still only
targets copper *blocks* (never copper ore, deepslate copper ore, or raw copper blocks), still
respects the `abilities.cooldowns.flux-conduction` cooldown and the `abilities.flux-conduction.range`
search radius, and now tells you when there's no copper in range instead of failing silently.
Requires the Flux Gem as your active gem.

---

## BlissGems 4.9.0

### Added

**Prismatic Edge Legendary Sword.** Introduced a new legendary netherite sword (`prismatic_edge`) with pre-applied Sharpness VII, Mending, and Unbreaking III.
- **Prismatic Beam (Right-Click):** Fires a 24-block rainbow beam dealing 10 HP (5 hearts) damage, freezing targets solid, and granting Regeneration II to wielder (120s cooldown).
- **Combo Crits:** Landing 5 consecutive hits without being struck back activates guaranteed critical hits (1.5x damage multiplier) on all subsequent hits until you take damage.
- Custom texture added to Resource Pack V5.2. Em-dashes in messages and lore formatted for clean Minecraft chat rendering.

**Restoration Book & Ritual.** Introduced the `restoration_book` crafted with Echo Shards, Totems of Undying, a Nether Star, Gem Fragments, and a Book. Right-clicking with a BROKEN gem triggers a server-wide Restoration Ritual (thunderstorm sequence) that re-rolls the broken gem back into a Pristine state.

**Gale Clouds for Speed Gem (Quaternary Ability).** Shift + F with Tier 2 Speed Gem grants 3 throwable Gale Cloud projectiles that inflict Slowness II on impact and put enemy Wind Charges on cooldown.

**Broken Gem Damage Bonus.** Attackers holding a active gem deal 1.5x extra damage against targets holding a BROKEN gem (energy 0).

### Fixed & Changed

**Fixed Passive Effect Activation Gate.** Passives are now active at energy stage 1 (Ruined) and above. They are only disabled when a gem is completely BROKEN (energy 0).

**Fixed NaN Vector Normalization Crash in Blur Ability.** Prevented Paper server crashes caused by zero-length vector normalization when targets stand on the exact strike origin.

**Exposed Blur Ability Config.** Added `abilities.blur` settings in `config.yml` (knockback strength, strike count, charge timeout).

---

## BlissGems 4.8.0 / BlissMythics 1.5.0

### Added

**Blur clones now wear your actual skin, not just your head.** With ProtocolLib installed
(soft dependency — nothing changes if it isn't), the clone that rides the bolt down is a
packet-only fake player carrying your real skin, full body. Without ProtocolLib it falls back
to the old armor-stand-with-skull clone, whose head texture is also more reliable now — it
reads your full skin profile instead of a bare player reference, which could silently fail to
resolve. Blur also grants 5 stored strikes now instead of 3.

**Shadow Stalker draws a beam straight to your target.** A thin red particle line now points
from you to whoever you're tracking, redrawn a few times a second. It's sent only to you, the
same way the tracking arrow already was, so it doesn't give either player's position away to
anyone standing nearby.

**Auratus slam locks out wind charges near the crater.** Landing a slam denies wind charge use
to anyone (other than you) within `auratus.slam.no-windcharge-radius` blocks of the impact point
for `auratus.slam.no-windcharge-ms` — they fizzle instead of firing.

### Changed

**Auratus slam now needs real height, and launches you where you grappled.** Ground slams used
to trigger off any sneak-landing regardless of how far you fell — you now need
`auratus.slam.min-height` blocks of fall above the landing spot (10 by default), which in
practice means windcharging up after the yank to clear it. The launch out of the crater no
longer uses whatever direction you happen to be looking: it follows the grapple that brought you
there instead — straight up if it barely moved you sideways, or your original heading with more
height (`auratus.slam.launch-force`) if it carried you somewhere. Victim knockup on both the slam
and Bloodlink is stronger and configurable (`auratus.slam-knockup`, `heretic.bloodlink-knockup`).

**Bloodlink's dive is now an actual glide.** The forced-down impulse is unchanged, but the fall
itself now runs through vanilla elytra physics (`setGliding`) instead of a single rigid velocity
vector, so it reads as a glide down rather than a drop.

**Heretic Bloodsaws and Auratus chains break cobwebs they run into**, instead of stopping dead
against them like solid terrain.

### Fixed

**Circle of Life's particles weren't reliably showing.** The heart, sculk-soul and happy-villager
particles were missing the force-visibility flag that the dust particles already had, so they
were silently dropped for anyone with reduced particle settings or standing a bit further away.
All of the circle's particles are forced now.

## BlissGems 4.7.5 / BlissMythics 1.4.2

### Changed

**Blur strikes with a clone.** The bolt still comes down where you're aiming, but the blow is
delivered by a copy of you that lands with it — wearing your skin, armour and weapon. It winds
up, swings, and dissolves a second later. Damage, radius, knockback and trusted-player skips are
unchanged, and kills still credit you. The clone can't be hit, looted or pushed around.

**Heretic: Bloodlink is aimed in all three dimensions.** The hop only ever carried you sideways —
the launch height was fixed and so was the dive, so anything above or below you was unreachable
and distant targets fell short. The whole arc is now solved against the game's air physics toward
whatever is under your crosshair, and the dive begins at the top of the arc rather than on a fixed
tick, re-aiming at whatever distance is left. Across the tested range it lands within about a
block of the point you aimed at, uphill or down. Aiming at open sky still gives the old forward
hop.

**Auratus: chains can't be spam-fired.** A chain that connects refunds its charge, so anyone
who kept hitting something never actually spent one and could fire nonstop. The refund stays,
but there's now a two-second floor between shots — `auratus.chain.min-interval-ms`.

### Fixed

**Conduction teleported you underground.** It matched any block whose name contained COPPER,
which includes copper ore, deepslate copper ore and raw copper blocks. With no real copper
nearby it would find ore in a cave within range and drop you into it. Copper blocks only now.

**Puff's launch-on-hit had no cooldown whatsoever** — every single melee hit sent the target
flying. It's on a 15 second cooldown now (`abilities.cooldowns.puff-launch`), and it respects
`/bliss nocdtoggle` like everything else.

**Puff's sculk-shrieker immunity only applied from the offhand.** Puff gets played as a main-hand
weapon just as often, and every other Puff passive already checked both hands. Shriekers stay
quiet with the gem in either hand now. Tier 1 still has no immunity unless you enable it.

**Auratus chains ignored `/bliss nocdtoggle`.** Chain charges are tracked inside the mythics
addon rather than the ability-cooldown system, so the exemption never reached them — which is
why it seemed to work on some Auratus abilities but not others. It's honoured now.

## BlissGems 4.7.4 / BlissMythics 1.4.1

### Fixed

**Your config now actually updates.** New settings were never being written into an existing
`config.yml`. The auto-repair checked whether a key was missing using a config object that had
the JAR's defaults attached to it, so every key always looked present and the repair silently
did nothing on every update since it was added. It now reads the file on disk. On first start
after updating you'll get a `config.yml.backup` and a log line for each key that gets added —
including the Puff sculk-immunity settings that appeared to have gone missing.

**`single-gem-only: false` had no effect.** Turning it off was supposed to let players carry
several gems, but the rule was hardcoded in four separate places and the config value was never
read at all — a newly picked-up gem would delete the one you already had. Picking up, moving,
dying and changing worlds all respect the setting now.

**Auratus: you couldn't grapple onto the sky anchor.** Two problems stacked up. The anchor window
lasts 5 seconds but chains recharge in 8, so a parry regularly handed you an anchor you had no
charge left to reach — anchor launches are free now. And the chain's entity raytrace fired first,
which caught the player you had just parried, since Chained Judgment leaves them hanging directly
overhead. A live anchor window plus a steep upward aim now takes the shot outright.

**Heretic: Bloodlinking looked like it did nothing.** A link needs at least two players caught in
one slam before damage can be shared between them. Hitting one player, or none, now says so
instead of failing silently. The requirement itself is unchanged.

### Added

**`/bliss nocdtoggle [player]`** — flips ability cooldowns off for a player, and off again when
you run it a second time. Covers mythic gems too. Enabling it clears any cooldowns already
running, and nothing is recorded while it's on, so switching it back off leaves a clean slate.
Requires `blissgems.admin`. Not persisted — it resets when the server restarts. Energy costs
still apply.

**`auto-enchant.tier1-enabled`** — off by default, which keeps auto-enchant a tier 2 perk as
before. Switch it on and tier 1 gems auto-enchant at reduced levels: Efficiency III, Fortune II,
Looting II, Fire Aspect I, Feather Falling II, Power III, Punch I, Unbreaking II. Strength still
applies its Sharpness at both tiers regardless of this setting.

### Notes

Tier 1 Puff has no sculk-shrieker immunity — that's intentional and unchanged. If you want it,
set `passives.puff.tier1.sculk-immunity: true` once the config-repair fix above has added the key
to your file.

Bonus hearts that survive a server crash are stale max-health attribute modifiers.
`/fixhearts <player>` clears them, and they're also cleaned up when a player rejoins.

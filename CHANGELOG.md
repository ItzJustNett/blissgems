# Changelog

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

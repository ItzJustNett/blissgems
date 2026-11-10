# Changelog

## BlissGems 5.2.0

The biggest release since 5.0: Binding Vows, Progressions (Season 3), Energy/Dragon Games, the Awakened Gold Gem, Assassins, Wisps, the Mega Mace and real advancements, plus every ability cooldown matched to the Bliss SMP.

**Config:** existing servers get the new cooldowns automatically (config migration `2026-10-bliss-smp-cooldowns`); a value an admin changed by hand is left alone.

**Config:** the Awakened Gold Gem numbers move too (migration `2026-10-awakened-gold`); the Energy Games shop prices are gone (games.energy.revive-cost is the energy price of /bliss games revive).

**Resource pack:** needs the updated pack (gem icons in the action bar, Prismatic Edge model, Gold Gem sockets, gem shards, Auratus/Heretic models, Gem Trader crosses, the Bliss sounds).

### Added
- **Bliss-style action bar** for every gem: `[1st] Ready!  [gem icon] Ready!  [2nd] Ready!`; the gem icon in the middle carries the extra abilities (Flux shows one per ability). Fire no longer looks different from the rest.
- **Awakened Gold Gem:** 8 souls only harvest-awaken the gem (it still reads Dormant, its text glitched); the end of a Golden Dream (or /bliss goldgem awaken <player>) fully awakens a gem holding all 8. Then: passives Ichor, Midas Touch, Prophecy (+10 hearts), and /bliss goldcycle steps through the souls and two movesets - **I:** Snap / Golden Drill, Gilded Wrath, Rayler (3 charges, blinks behind the target) / Gilded Viola (the triple beam, 3 h); **II:** Prophet's Ladder (pulls enemies into the beam, Unfortunate) / Gilded Blitz (yank, strips Regeneration/Resistance/Absorption), Gilded Wrath, Mark of the Beast (2 charges). Snap skips mythic and Binding Vow gems. Numbers under gold.awakened (migration `2026-10-awakened-gold`).
- Gold Gem lore: Dormant (glitched passives/ability/powers) until fully awakened, then "The Pinnacle of Bliss" with the awakened kit; no harvested-souls list or channelled soul's lore on the item.
- /bliss goldgem fill <player> all [tier] fills all 8 souls (Tier 2 by default).
- **Gold Gem sockets:** the gem shows exactly which gems' souls it holds (256 combinations).
- **Gem shards:** a harvested gem drops its own shard (Fire Shard, Astra Shard, ...) instead of a generic Gem Fragment. /bliss giveitem <player> <gem>_shard (in its suggestions).
- **Gem Trader picker:** left-click a Gem Trader to choose the pool; every gem taken out costs 1 energy (trader.pick-energy-per-gem). Right-click still rolls any gem.
- **Advancements:** the 13 achievements are real advancements now (own tab, toast, chat line). No extra plugin needed.
- **Mega Mace:** the Heavy, Tempered and Overcharged maces in a crafting grid forge it; no cooldown, no enchantment limits. mega-mace.max-crafts caps how many the server ever forges (-1 = no limit; the count is in mega_mace.yml).
- **Assassins:** /bliss assassin <player> <auratus|heretic|off> (netherite with Curse of Vanishing, 20 hearts, stocked ender chest, the mythic gem).
- **Wisps:** /bliss wisp <prophet|manifestation|balance|off> to become one (chat is spoken as the wisp), /bliss wispsay to speak as one.
- abilities.require-main-hand: abilities only from the main hand; passives (double jump) still work from both.
- Memory NPCs in the Golden Dream wear a random kit (memory_npcs.yml kits) and attack players within aggro-range.
- **Progressions** (Bliss Season 3, off by default, progression.enabled): stages 1-5 seal/open the Nether and End, set per-stage enchant caps, potion and item limits and banned abilities, Enchanted Obsidian opens the Nether in Progression 1, a custom Wither drops Upgraders/Revive Books/Traders on the first Nether visit, the Dragon Egg gives +10 hearts and halved cooldowns from Progression 3. /bliss progression <status|set <1-5>|1-5|next|prev> - any phase, either direction, with a confirmation.
- **Binding Vows:** /bliss vow <player> <gem|quakitus|off>. Nemvevo-style: a gem's Tier 2 powers with no gem, used from an empty hand (action bar in the gem's colour). "BINDING VOW CREATED" on creation.
- **Quakitus's Binding Vow Fire Gem** (/bliss vow <player> quakitus): only Fire Resistance and the fireball, which burns for 5-6 hearts through armor, blows up bigger (abilities.fire-vow.explosion-multiplier) in purple and black, marks the target and kills read "flew too close to the sun". The fireball is purple while charging and in flight. Same name and lore as the Fire Gem, with only the fireball under powers; its action bar shows only the fireball. Can't be traded, never drops to Tier 1 on death.
- **EMP Binding Vow** (/bliss vow <player> emp), a vow of its own: /bliss emp, sneak + F with both hands empty, or sneak + right-click a block with an empty hand. A pink sphere for 10 s, every mythic disabled for 30 s (powers and passives: Auto Crit, Blood Hardening, Feathered Fall, Hauling Strike, ...), then the holder loses 5 hearts for 30 s and their gem is off for 60 s (abilities.fire-vow).
- Binding Vows: F stands in for an empty-hand right click in the air (the client never sends that), and a fist on a block mines it instead of firing the vow.
- **Dragon Games shop** (/bliss shop, points): 2 Hearts, Enchanted Golden Apple, Restock Shulker, Totem (75), 2 Gem Traders, 64 Cobwebs (30), 64 Golden Apples, 64 XP bottles, Player Tracker (15), Strength I (blaze powder), Speed I (sugar), Fire Resistance (magma cream) (5) - right-click to use (games.shop.effect-seconds); prices under games.shop.dragon. The Restock Shulker is Bliss's: three rows of gapples, strength, speed, fire res, webs and xp.
- Dragon Games points values: each player is worth a random tier (games.dragon.worth-tiers) or what /bliss games worth <player> <points> sets; their killer earns it. Anyone in the games can /bliss games revive <player> for that player's value. Off-hand shields are banned.
- **Blood Moon** (/bliss games bloodmoon [minutes|off]): a timed red night (games.blood-moon-minutes, 90) with a "The Blood Moon" bar, red sky and moon with the shaderpack, no sleeping; zombies in Protection IV netherite with Sharpness V swords, skeletons with Power V / Punch II / Flame bows, strong spiders, charged creepers.
- Progression titles: "Phase N" with the phase's name (progression.stages.N.name: The Creation of Everything, The Nether, The Dragon Games, The Lost Souls, The End of Everything) when a phase starts and on /bliss progression.
- Golden Dream memories carry fully enchanted kits and supplies (gapples, healing / strength / speed potions, a totem), block with shields (an axe knocks the shield down), strafe and crit-jump, and use a gem power (fire, puff, speed, strength or life). Their loot is what they carried (memory_npcs.yml loot-mode) plus their head (only the Memory head, no extra "'s Head"); dropping 15 heads (memory_npcs.yml heads-needed) at the memory world's spawn brings the great thunder and wakes the dreamers. Memories wear maxed-out diamond only (old shipped kits are replaced).
- Enchanted Obsidian has the enchantment glint, glows when dropped or placed, and is in /bliss giveitem's suggestions.
- Wisps have no player any more: other players don't see the wisp's body, name tag or tab entry, only its light and name. New light shapes: the Prophet's gold whirl, Balance's flower of life, Manifestation's orbits.
- A mythic holder killed by a player loses the mythic (Auratus, Heretic) to their killer.
- Mythic gems (Auratus, Heretic) are droppable like any item: Q, chests, and they fall where their holder dies (to a player: straight to the killer). Dropping one gives back the gem it replaced - also when the mythic came from /bliss give, which now stores the player's gem instead of deleting it. Inside the Golden Dream the stored gem waits until the dreamer wakes.
- Gold Gem: Awakened action bar `[snap] Ready!  [gem] Gilded Wrath  [rayler] charges` with Bliss icons; Snap and Rayler icons in the lore.
- Bliss particle textures in the resource pack.
- **Energy Games / Dragon Games:** /bliss games ... (one life, kill quota or points, gem bans, events, Dragon Egg for the winner). /bliss shop is the Dragon Games shop only (the Energy Games have none); admins/ops can always open it. /bliss games points <player> [n|+n|-n] shows or sets Dragon Games points.
- Astra's action bar shows the captured soul (or Empty) in the middle at Tier 1 and Tier 2.
- Gem Trader picker is one row like Pockets; a gem taken out of the pool shows the gem with a red cross.
- Honored One: a yellow halo over the head and under the feet and two white beams circling the back; /bliss honored toggles it, staff /bliss honored set <player>|clear.
- Golden Dream walkway uses the shaderpack's GoldenDome too.
- Golden Dream memory-world copy-main: the copy is taken after a full flush to disk (it used to miss recent changes), includes the main nether and end, and runs off the main thread so a big map can't freeze the server; dreamers keep walking the pocket until it is ready.
- New Prismatic Edge model.
- API: GemDefinition.minTier (mythics skip Tier 1), GemCosmetics.register (addons get the built-in tooltip look).

### Changed
- **Cooldowns match the Bliss SMP** (abilities.cooldowns):

  | Gem | Ability | Old | New |
  |---|---|---|---|
  | Puff | Dash | 12s | 40s |
  | Puff | Breezy Bash | 45s | 75s |
  | Speed | Blur | 45s | 90s |
  | Speed | Speed Storm | 90s | 20s |
  | Strength | Frailer / Nullify | 120s | 240s |
  | Strength | Chad Strength | 10s | 150s |
  | Strength | Shadow Stalker | 180s | 300s |
  | Wealth | Rich Rush | 540s | 420s |
  | Wealth | Amplification | 300s | 420s |
  | Wealth | Item Lock / Unfortunate | 90s / 150s | 200s |
  | Fire | Fireball | 35s | 75s |
  | Fire | Meteor Shower | 90s | 75s |
  | Fire | Crisp | 60s | 42s |
  | Fire | Cozy Campfire | 90s | 600s |
  | Flux | Energy Beam | 150s | 30s |
  | Life | Circle of Life | 120s | 180s |
  | Life | Heart Drainer | 60s | 75s |
  | Astra | Dimensional Daggers | 75s | 90s |
  | Astra | Dimensional Drift | 60s | 45s |
  | Astra | Astral Projection | 120s | 300s |
  | Astra | Astral Void | 120s | 150s |

  Unchanged, already matching: Kinetic Burst and Flux Ground (30s), Heart Lock (75s), Vitality Vortex (45s).
- Puff Dash is a flat, fast dash (abilities.puff-dash.speed / ticks) instead of a lunge, hitting everyone it passes through once, and eases out instead of stopping dead.
- Puff: the self-launch "Breezy Bash" is gone. Breezy Bash is one ability now: aimed at a player it flings them up and slams them down (abilities.puff-breezy-bash), aimed at nobody it flings you and every enemy around you up (abilities.puff-group-bash). The double jump is a single air jump with a tiny dash (passives.puff.double-jump-dash) and sits in the middle of the action bar.
- **Tier 1 = the ability only:** a Tier 1 gem has just its ability (the middle one: Crisp, Dimensional Drift, Vitality Vortex, Static Burst, Thunder Step, Shadow Stalker, Pockets); every input fires it and the powers need Tier 2 (Puff keeps its Tier 1 kit). The action bar shows only the gem crystal and the ability.
- Action bar: the middle icon is each gem's own upright crystal; Tier 1 shows only `[Tier 1 crystal] Ready!` for its ability (Astra: the held soul, Puff: `[dash]  [double jump]`).
- Assassins wear their mythic's trims: Auratus gold Flow / Bolt / Eye / Spire, Heretic redstone Silence.
- Resource pack: the Bliss sounds - the Lost Souls recording, the Wanderer's disc message (the coordinates also spell out in a title) and the second message when you reach them (villager-event.arrival-radius), the Last Raid horn and portal hum, the Bliss villager voices and thunder, and the Auratus chain and slam sounds.
- Villager event: the villagers trade during the Lost Souls hunt and stop at the Last Raid (phase 3) or Progression 5; after the raid they're gone.
- Astra holds 1 soul (passives.astra.soul-capacity) and the action bar shows which mob it is.
- golden-dream.memory-world defaults to copy-main: the memory is a copy of the server's overworld.

### Fixed
- Auratus and Heretic showed each other's (or the wrong) texture: the gem's energy level shifted their model number like a normal gem's.
- The Lost Souls titles ran 3 s longer than the recording; each line now starts with its voice line.
- Gold Gem souls taken at Tier 1 follow the Tier 1 rule too (only the ability).
- Golden Dream shaders only switched on while the world was on day 0 (the server adds the world's day to a fixed player time).
- Mace and Energy Games villagers lost their profession and trades without a job block.
- Config migrations can now change the same value twice across releases (Flux Beam 240s -> 150s -> 30s).
- Puff could triple jump.
- /bliss games eliminate / revive did nothing (and said nothing) for players who weren't on the participant list or weren't out; /bliss shop now says why it is closed.
- End sky visuals threw an error every tick on 1.21.9+ (Dragon Breath particle needs a power value now).
- Speed's packet clone never worked on 1.21+ and always fell back to the armor stand; it uses the current player info / spawn packets now.
- Circle of Life errored when a player inside it changed worlds.
- Awakened Gold Gem kept the powers of the soul it was channelling (Astra, Flux, ...): waking it switches to Moveset I, and the soul menu now offers Moveset I and II.
- Midas Touch and Prophecy (+10 hearts) never worked when their timer started on an odd tick; same for the Wisps' lights.
- Speed Blur struck the ground behind a player you aimed at, so it rarely hit; it strikes the entity in the crosshair now.
- Enchanted Obsidian only made its holder glow with Progressions on.
- Gem shards say what they craft (they work as Gem Fragments: Repair Kit, Restoration Book, Prismatic Edge).

### BlissMythics
- Auratus and Heretic are Tier 2 only (GemDefinition.minTier).
- Auratus: a ground slam hits for 4 hearts (auratus.slam-damage 12 -> 8; servers on the old default move over), the chain stays at 3.
- Fixed: Auratus and Heretic gems showed the wrong (purple) texture; the resource pack maps them on the nautilus shell now.
- Bliss-style action bar for both, no gem icon: Heretic `[bloodsaws] charges  Bloodstorm  [bloodlink] cooldown`, Auratus `[perforators] chains  [aegis] cooldown`.
- Mythics are worked like in Bliss: left click with the gem in the main hand is the first ability, right click the second.
- Heretic: **Bloodstorm** comes from throwing both Bloodsaws at once (heretic.bloodstorm-window-ms), not from Bloodlink: a blood fountain (red double helix, dark spray) where everyone inside bleeds; own cooldown (heretic.cooldowns.bloodstorm, 90s). The Bloodlink hop tips into a divebomb that your aim steers (heretic.divebomb-steer), speeding up to heretic.divebomb-speed.
- Auratus: the thrown Echoing Aegis is slower (auratus.aegis-throw-speed / -ticks) and drags one chain (it drew three); the first enemy it reaches takes aegis-chain-damage and Slowness III (aegis-chain-slow-ticks). Flying chains have a hook tip.

## BlissGems 5.1.7

**Resource pack:** https://github.com/ItzJustNett/blissgems/releases/download/resourcepack-latest/BlissGems-Resourcepack.zip (updated on every change - get the new one, the old pack has no textures for the new items or the menu banner).

### Balance
- **Fire:** Fireball cooldown 45 → 35 s. Cozy Campfire 3 damage/s (was 2), radius 5 (was 4), heals 1 HP/s (was 0.4), cooldown 90 s (was 120). Crisp cooldown 60 s (was 90), lasts 20 s (was 15). Meteor Shower 10 damage (was 8), cooldown 90 s (was 120), lasts 10 s (was 8).
- **Flux:** Flux Beam cooldown 150 s (was 240), base damage 14 (was 10). Ground stuns 4 s (was 3). Flashbang 6 s (was 5). Kinetic Burst knockback 2.6 (was 2.0), radius 6 (was 5). Shocking arrows 25% chance (was 15%), Tier 2 damage 4 (was 3).
- **Speed:** Speed Storm cooldown 90 s (was 135), 3 damage (was 2), lasts 12 s (was 10). Blur 3.5 damage per strike (was 2.5). Terminal Velocity lasts 15 s (was 10), cooldown 50 s (was 60). 4th ability cooldown 45 s (was 60).
- Existing servers get these new values automatically on startup, except values an admin changed themselves.

### Added
- **New /bliss menu:** the BLISS banner on top, Tier 1 gems on the left, Tier 2 on the right, the Gold Gem in the middle (click it: the Gold Gem, all 7 Wire Fragments and the Fragment Core), Auratus and Heretic (links the addon when it isn't installed) and a link to more gems. The old admin panel is /bliss admin.
- **Gems are now nautilus shells** so Bedrock players can hold them in the off hand. Old gems convert automatically when a player joins.
- Gem names and descriptions use the original Bliss design again, with the gem's real energy level ("(Pristine)", "(Cracked)", ...) on top.
- Chat messages use colour gradients (MiniMessage in config.yml messages; old & colour codes still work).
- Textures for Wire Fragments 1-7 and the Fragment Core.
- Auto-update from Modrinth (auto-update in config.yml).
- /bliss cooldowns <player> for admins.

### Fixed
- Life Gem made players nearly unkillable: its absorption shield refilled every second. It now comes back 30 s after it breaks (passives.life.absorption-regen-seconds).
- Gem Upgraders never went below 2 charges (infinite upgrades).
- Energy Bottles could revive a Broken gem.
- Passives used the wrong tier for gems in the hotbar (a Tier 2 Strength gem gave Strength I).
- Astra daggers' armour piercing stacked through hit invulnerability.
- Wealth Amplify: boosted enchants (e.g. Protection V) stayed on gear put in a chest, dropped or lost on death.
- Fragment Core and Villager Soul showed the Energy Bottle texture.
- Dimensional Drift's cooldown wasn't shown in the action bar.
- Ability boss bars are now only for Bedrock players.
- /blissevent villager set picks the villager you look at.
- /bliss ability shows the default keybinds.
- A gem can't be used to craft (nautilus shells make conduits).

## BlissGems 5.0.1

### Changed
- Flux Beam is now ~2.5x stronger: base damage 4 → 10 HP (up to 30 HP at full charge), and max armor durability damage 100 → 250.

## BlissGems 5.0.0

### Fixed
- Ancient Debris double passive now properly drops extra debris when mined, and both drops are automatically smelted into netherite scrap when AutoSmelt is active.
- Unfortunate action fail chance reduced from 100% to a balanced 50%, removing oppressive movement, jump, and interact locks. Added a 50% chance to fail Totems of Undying while under the effect.
- Fixed an issue where gem abilities (Unfortunate, Item Lock, Puff bash, Bloodthorns, and durability chip) affected trusted allies.
- Fixed a bug where targets affected by Flux Ground became invulnerable for several seconds. Targets now receive damage normally and their hotbar is shuffled.
- Fixed Gold Gem absorbed souls being unable to use command-based abilities (/pockets, /amplify, /souls, /release, /charge, /conduction, /autosmelt).
- Fixed ClassCastException when saving Gold Gem data and running /bliss goldgem fill.
- Fixed Astra Gem Soul Capture and Dimensional Void triggering simultaneously on sneak left-click. Added sneak right-click mob capture.
- Fixed Wealth Amplification enchantment boosts persisting permanently on items after the ability ends.
- Fixed Astra Gem Drift and Nullify cooldowns not appearing in the action bar display.
- Gold Gem Sundering Beam damage adjusted from 1000 to 24 HP, allowing high-tier defense setups (Turtle Master + enchanted golden apples) to survive.
- Gold Harvest ceremony no longer triggers if the victim holds an active Totem of Undying, allowing the totem to pop normally.
- Fixed Flux Charging Station failing to open or charge when holding the gem in offhand or via absorbed Flux.

### Added
- AutoSmelt is now toggleable for both Fire Gem (all tiers) and Wealth Gem (Tier 2+) via /bliss autosmelt.
- Added direct commands to trigger gem abilities: /bliss primary, /bliss secondary, /bliss tertiary, and /bliss quaternary (also available via /bliss ability <slot>).
- Added dedicated commands to open the gem viewer: /bliss viewer, /bliss gemviewer, /bliss gui, /bliss menu.

---

## BlissGems 4.9.9 / BlissMythics 1.5.5

### Fixed

**Heart Drainer no longer hits trusted players.** The Life Gem's primary was the one Life ability
with no trust check — Circle of Life and Heart Lock both had one — so it withered and drained
allies. It now refuses a trusted target the way Heart Lock does.

**A fresh Gold Gem no longer inherits the last one's souls.** Each Gold Gem carries its own
instance id, but the record of which gem a holder's harvested souls belonged to lived only in
memory. Any relog or restart wiped it, and the stored souls were then adopted onto whatever Gold
Gem the player next picked up — so clearing a filled gem and handing out a new one gave back an
already-awakened gem. The instance is now persisted to `playerdata/<uuid>.yml` as `gold.instance`
and dropped on logout. Existing saves have no instance recorded, so each holder's next gem is
adopted once and tracked correctly from then on.

**`/bliss give <player> gold 2` explains itself.** The Gold Gem has no Tier 2 — its second-tier
abilities come from the tier of the *soul* it channels — so asking for one built an item id that
was never registered and failed with a bare "Failed to give gem!". It now clamps to Tier 1 and
says where secondary abilities actually come from.

**`gems.droppable-on-death` matches properly.** Entries were compared case-sensitively against the
plain gem id, so `- Auratus` or `- auratus_gem_t1` silently did nothing while `- auratus` worked.
Matching now ignores case and accepts either form, and any entry naming no known gem is reported
in the console one tick after startup, once addon gems have registered.

**A mythic listed in `droppable-on-death` no longer drops twice.** BlissMythics adds mythic gems to
the death drops itself; with the gem also left in the drops by BlissGems' config, both copies
landed. BlissMythics now skips a gem that is already dropping.

### Added

**Gold Gem soul commands.** `/bliss goldgem remove <player> <soul>` takes one soul back out,
`clear <player>` empties the gem to dormant, and `list <player>` prints what it holds and which
soul is selected. Removing the active soul falls back to another one automatically. These join the
existing `fill`, and all four tab-complete.

**Ability keybind defaults live in `config.yml`.** A new `ability-bindings` section sets which
input drives which slot for players who haven't rebound anything themselves, with a separate
`bedrock-defaults` for Floodgate players. Set an input to `none` to leave it alone — useful for
`left_click`, which otherwise drives the Gold Gem's quinary slot. Per-player rebinding via
`/bliss set_ability` is unchanged and still wins over these defaults.

### Changed

**The Gold Gem gilds armour with the Flow trim** instead of Sentry. Still configurable at
`gold.armour-trim.pattern`, which now also documents `FLOW` and `BOLT`.

**`/bliss` help lists the keybind commands.** `/bliss ability` and `/bliss set_ability` existed but
appeared in neither the help output nor tab-completion, so nobody found them.

---

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

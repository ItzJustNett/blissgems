# Pack additions

Everything here is already merged into **BlissGems Resourcepack V5.3** — this folder is the
source of truth for what the plugin expects, so a future pack rebuild can be re-merged from it.

## Gold Gem item art

The plugin gives the Gold Gem `minecraft:prismarine_crystals` with **CustomModelData 1009**
(`CustomItemManager.registerItem("gold_gem_t1", ...)`). It has no energy wear states, but it
does have **one model per harvested soul**: the plugin sets `1009 + souls`, so the gem asks
for 1009 while dormant and 1017 once all eight souls are in it
(`GoldGemManager.BASE_MODEL_DATA`).

| File | Goes to |
| --- | --- |
| `assets/blissgems/textures/item/gold_gem.png` | your pack, same path |
| `assets/blissgems/models/item/gold_gem.json` | your pack, same path |
| `assets/blissgems/models/item/gold_gem_1.json` … `gold_gem_8.json` | your pack, same path |
| `assets/minecraft/items/prismarine_crystals.json` | your pack, same path |

All nine models carry the same `display` block as the normal gems (copied from
`fluxgemtier1.json`), so the Gold Gem stands upright in hand, in the GUI and in item frames
like the rest of them. Keep it on any model you redraw.

**The soul models are placeholders**: all eight currently point `layer0` at the base
`blissgems:item/gold_gem` texture, so the gem simply keeps its dormant look until the art
exists. Draw `gold_gem_1.png` … `gold_gem_8.png` (the centre picking up the colours of the
gems eaten so far) into `textures/item/` and repoint each model's `layer0` at its own
texture. Nothing in the plugin changes.

The art lives under `textures/item/` (not `textures/custom/`) because that is the pack's
convention: every item texture is `blissgems:item/...`, and `models/custom/` only holds the
Blockbench prop models — parry, chain, auratus_smash — which also read their textures from
`item/`.

`prismarine_crystals.json` is the 1.21.4+ item definition (`range_dispatch` on
`custom_model_data`), the same shape as the pack's `echo_shard.json`. If your pack already has
that file, add only the `1009` entry to its `entries` list.

`gold_gem_source_108px.png` is the background-stripped source art at full resolution, kept in
case you want to re-export at a different size. It is not used by the pack.

## Action bar icons

16x16 glyphs in the same style as the existing gem ability icons (`icons/astra1.png` etc.),
mapped through `assets/minecraft/font/default.json`:

| File | Char | Drawn by |
| --- | --- | --- |
| `assets/blissgems/textures/icons/gold_gem.png` | `U+E020` | Gold Gem action bar — soul counter |
| `assets/blissgems/textures/icons/gold_beam.png` | `U+E021` | Gold Gem action bar — Sundering Beam |
| `assets/blissgems/textures/icons/gem_lock.png` | `U+A42C` | `GemLockManager.LOCK_GLYPH` (gem-locked bar and chat) |

Each one needs a provider in `assets/minecraft/font/default.json`:

```json
{ "type": "bitmap", "file": "blissgems:icons/gold_gem.png",  "ascent": 8, "height": 16, "chars": [""] },
{ "type": "bitmap", "file": "blissgems:icons/gold_beam.png", "ascent": 8, "height": 16, "chars": [""] },
{ "type": "bitmap", "file": "blissgems:icons/gem_lock.png",  "ascent": 8, "height": 16, "chars": ["ꐬ"] }
```

The channelled soul's own abilities reuse the existing per-gem icons (`U+E010`–`U+E01F`), so
no new art is needed for them.

## Still without art

`wire_fragment` (LIGHTNING_ROD, CMD 4006) and `fragment_core` (NETHER_STAR, CMD 4007) render
as their base items. To give them models, add `lightning_rod.json` / a `4007` entry to
`nether_star.json` under `assets/minecraft/items/` the same way as above.

# Pack additions

Everything here is already merged into **BlissGems Resourcepack V5.3** — this folder is the
source of truth for what the plugin expects, so a future pack rebuild can be re-merged from it.

## Gold Gem item art

The plugin gives the Gold Gem `minecraft:prismarine_crystals` with **CustomModelData 1009**
(`CustomItemManager.registerItem("gold_gem_t1", ...)`). Unlike the normal gems it has no
energy wear states, so 1009 is the only model it ever asks for.

| File | Goes to |
| --- | --- |
| `assets/blissgems/textures/item/gold_gem.png` | your pack, same path |
| `assets/blissgems/models/item/gold_gem.json` | your pack, same path |
| `assets/minecraft/items/prismarine_crystals.json` | your pack, same path |

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

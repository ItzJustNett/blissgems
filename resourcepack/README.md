# BlissGems resource pack

This folder **is** the complete pack (it used to hold only additions on top of a zip). Every push
zips it on GitHub:

- latest: `https://github.com/ItzJustNett/blissgems/releases/download/resourcepack-latest/BlissGems-Resourcepack.zip`
- each release also carries `BlissGems-Resourcepack.zip`

Local zip: `cd resourcepack && zip -qr ../BlissGems-Resourcepack.zip pack.mcmeta pack.png assets`

What the plugin expects from it:

- **Gems** are `minecraft:nautilus_shell` (Bedrock lets players hold those off hand):
  `assets/minecraft/items/nautilus_shell.json`, custom_model_data `1001-1008` (T1), `2001-2008` (T2),
  `+20/+30/+40` for the Pristine looks, `1009-1017` the Gold Gem (dormant + one per harvested soul).
  The old `echo_shard.json` / `prismarine_crystals.json` stay so not-yet-converted gems still render.
- **Gold Gem event:** Wire Fragments 1-7 are `copper_ingot` 7001-7007, the Fragment Core `nether_star` 7000.
- **/bliss banner:** U+E200 + U+E202 (two halves - font glyphs must fit 256 px) with spaces U+E201 (-7)
  and U+E203 (-1), at the end of `assets/minecraft/font/default.json`.
- Unit test `GemModelsTest` fails the build if a gem model number is missing from `nautilus_shell.json`.

"""
Builds the resource pack the test server sends: the shipped pack zip, the repo's resourcepack/
files laid over it, plus a 1.21.1 compatibility layer. The bot client is 1.21.1, which ignores
assets/minecraft/items/*.json (1.21.4+ item definitions), so every custom_model_data
range_dispatch there is also written as a 1.21.1 model with predicate overrides.

usage: build_pack.py <base.zip | -> <repo resourcepack dir> <out.zip>   ("-": no base, the dir is the whole pack)
"""
import json
import os
import sys
import zipfile

base_zip, overlay_dir, out_zip = sys.argv[1:4]
files = {}
if base_zip != "-":
    with zipfile.ZipFile(base_zip) as z:
        for name in z.namelist():
            if not name.endswith("/"):
                files[name] = z.read(name)
if os.path.isdir(overlay_dir):
    for root, _, names in os.walk(overlay_dir):
        for n in names:
            path = os.path.join(root, n)
            rel = os.path.relpath(path, overlay_dir).replace(os.sep, "/")
            if rel.startswith("assets/") or rel in ("pack.mcmeta", "pack.png"):
                files[rel] = open(path, "rb").read()

# vanilla parents for the base items the pack retextures
HANDHELD = ("_sword", "_axe", "_pickaxe", "_shovel", "_hoe", "mace", "trident", "stick")
BLOCK_ITEMS = {"beacon": "minecraft:block/beacon"}


def vanilla_model(item):
    if item in BLOCK_ITEMS:
        return {"parent": BLOCK_ITEMS[item]}
    parent = "minecraft:item/handheld" if item.endswith(HANDHELD) else "minecraft:item/generated"
    return {"parent": parent, "textures": {"layer0": f"minecraft:item/{item}"}}


def entries(model):
    if model.get("type") == "range_dispatch" and model.get("property") == "custom_model_data":
        for e in model.get("entries", []):
            inner = e.get("model", {})
            if inner.get("type") == "model":
                yield e["threshold"], inner["model"]


made = 0
for name in list(files):
    if not (name.startswith("assets/minecraft/items/") and name.endswith(".json")):
        continue
    item = name.rsplit("/", 1)[1][:-5]
    try:
        definition = json.loads(files[name])
    except ValueError:
        continue
    overrides = sorted(entries(definition.get("model", {})))
    if not overrides:
        continue
    legacy = vanilla_model(item)
    legacy["overrides"] = [{"predicate": {"custom_model_data": t}, "model": m} for t, m in overrides]
    files[f"assets/minecraft/models/item/{item}.json"] = json.dumps(legacy, indent=1).encode()
    made += 1

with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as z:
    for name in sorted(files):
        z.writestr(name, files[name])
print(f"pack: {len(files)} files, {made} items with a 1.21.1 model -> {out_zip}")

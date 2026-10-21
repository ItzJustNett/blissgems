"""Puts every tooltip screenshot of a run on one image (for a quick look): contact_sheet.py <results dir> <out.png>"""
import glob
import os
import sys

from PIL import Image, ImageDraw

res, out = sys.argv[1], sys.argv[2]
files = sorted(glob.glob(os.path.join(res, "items", "tooltips", "tooltip-*.png")))
W, H, COLS = 560, 600, 6
sheet = Image.new("RGB", (W * COLS, (H + 24) * ((len(files) + COLS - 1) // COLS)), (24, 24, 28))
draw = ImageDraw.Draw(sheet)
for i, f in enumerate(files):
    img = Image.open(f).convert("RGB")
    # the tooltip sits right of the inventory panel at 1920x1080, GUI scale 2
    crop = img.crop((790, 370, 1720, 1080)) if img.width >= 1920 else img
    crop.thumbnail((W, H))
    x, y = (i % COLS) * W, (i // COLS) * (H + 24)
    sheet.paste(crop, (x, y + 24))
    draw.text((x + 6, y + 6), os.path.basename(f)[8:-4], fill=(230, 230, 230))
sheet.save(out)
print(f"{len(files)} tooltips -> {out}")

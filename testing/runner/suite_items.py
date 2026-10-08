"""
Every custom BlissGems item: a screenshot of the full inventory, a screenshot of each item's
tooltip, and the tooltip text compared word for word with goldens/tooltips.json (taken from the
version the tooltips were approved on). The screenshots go to the visual review (the agent).
"""
import json
import os
import re
import time

from .harness import Fail, check, test

GOLDENS = os.path.join(os.path.dirname(__file__), "..", "goldens")
ITEM_SOURCE = "/repo/src/main/java/dev/xoperr/blissgems/utils/CustomItemManager.java"


def all_item_ids():
    """Every item the plugin registers, read from the source so new items are covered automatically."""
    src = open(ITEM_SOURCE).read()
    seen = []
    for m in re.finditer(r'registerItem\("([a-z0-9_]+)"', src):
        if m.group(1) not in seen:
            seen.append(m.group(1))
    return seen


def fill(ctx, ids):
    """Gives the items one by one into an empty inventory; returns {slot: item id} for what arrived."""
    w = ctx.world
    w.reset_player(ctx.tester)
    expected = {}
    taken = set()
    for item_id in ids:
        out = w.give_item(ctx.tester, item_id)
        time.sleep(0.25)
        now = {it["slot"] for it in ctx.tester.items()}
        new = sorted(now - taken)
        if not new:
            ctx.note(f"{item_id} did not arrive in the inventory: {out.strip()[:150]}")
            continue
        expected[new[0]] = item_id
        taken |= now
    time.sleep(1.0)
    return expected


@test("items", "inventory-and-tooltips")
def inventory_and_tooltips(ctx):
    ids = all_item_ids()
    ctx.note(f"{len(ids)} registered items: {', '.join(ids)}")
    pages = [ids[i:i + 36] for i in range(0, len(ids), 36)]
    golden_path = os.path.join(GOLDENS, "tooltips.json")
    golden = json.load(open(golden_path)) if os.path.exists(golden_path) else {}
    current = {}
    problems = []
    # one player may hold one gem; for this showcase the anti-dupe check is off (restored afterwards)
    ctx.world.set_config({"gems.anti-dupe.enabled": False})
    ctx.tester.hud(True)  # F1: no chat or hotbar over the inventory; screens still draw
    for page_no, page in enumerate(pages, 1):
        expected = fill(ctx, page)
        # gems show their owner's live level line; energy 5 = "(Pristine)". /fixgems restyles every
        # gem in the inventory the way joining the server does.
        ctx.world.cmd(f"bliss energy {ctx.tester.name} set 5")
        ctx.world.cmd(f"fixgems {ctx.tester.name}")
        time.sleep(1.0)
        got = {it["slot"]: it for it in ctx.tester.items()}
        missing = [expected[s] for s in expected if s not in got]
        if missing:
            problems.append(f"never arrived in the inventory (removed by the plugin?): {missing}")
        ctx.tester.open_inventory()
        time.sleep(1.0)
        ctx.tester.hover(-1)
        ctx.shot(f"inventory-page{page_no}", ctx.tester.screenshot(), folder="items")
        for slot, item_id in expected.items():
            it = got.get(slot)
            if it is None:
                continue
            current[item_id] = {"base": it["id"], "customModelData": it.get("customModelData"),
                                "name": it["name"], "tooltip": it["tooltip"]}
            ctx.shot(f"tooltip-{item_id}", ctx.tester.hover(slot), folder="items/tooltips")
        ctx.tester.close_screen()
    ctx.tester.hud(False)

    os.makedirs(os.path.join(ctx.out, "items"), exist_ok=True)
    json.dump(current, open(os.path.join(ctx.out, "items", "tooltips.json"), "w"), indent=1, ensure_ascii=False)
    if ctx.update_goldens:
        json.dump(current, open(golden_path, "w"), indent=1, ensure_ascii=False)
        ctx.note("goldens/tooltips.json updated from this run")
    else:
        if not golden:
            problems.append("no goldens/tooltips.json yet - run with --update-goldens on an approved version")
        for item_id, want in golden.items():
            have = current.get(item_id)
            if have is None:
                problems.append(f"{item_id}: in the goldens but not given / not shown")
                continue
            for field in ("base", "customModelData", "name", "tooltip"):
                if have.get(field) != want.get(field):
                    problems.append(f"{item_id}: {field} changed\n    was: {want.get(field)}\n    now: {have.get(field)}")
        for item_id in current:
            if item_id not in golden:
                problems.append(f"{item_id}: new item, not in the goldens yet")
    if problems:
        raise Fail(f"{len(problems)} tooltip difference(s):\n" + "\n".join(problems))

"""
The /bliss gem catalogue (a 4-row chest under the BLISS banner, "|" = blue glass):
  T1 T1 | .     GOLD   .     | T2 T2
  T1 T1 | .     .      .     | T2 T2
  T1 T1 | AURATUS .  HERETIC | T2 T2
  T1 T1 | .   MORE GEMS  .   | T2 T2
The Gold Gem opens the Gold Gem page; Auratus/Heretic link the BlissMythics addon when it is not
installed (it isn't on the test server); the diamond block links the BlissGems Expansion.
"""
import time

from .harness import check, test

GEMS = ["astra", "fire", "flux", "life", "puff", "speed", "strength", "wealth"]
LEFT = [0, 1, 9, 10, 18, 19, 27, 28]
RIGHT = [7, 8, 16, 17, 25, 26, 34, 35]
MYTHICS_URL = "modrinth.com/plugin/auratus-hertic-addon"
EXPANSION_URL = "modrinth.com/plugin/blissgems-expansion"


def open_menu(ctx):
    ctx.tester.chat("/bliss")
    time.sleep(1.5)
    return {it["slot"]: it for it in ctx.tester.items_full().get("screen", [])}


@test("menu", "bliss-menu-layout")
def bliss_menu(ctx):
    ctx.world.reset_player(ctx.tester)
    screen = open_menu(ctx)
    ctx.tester.hud(True)
    ctx.tester.hover(-1)
    ctx.shot("bliss-menu", ctx.tester.screenshot(), folder="menu")
    for slot in (4, 31, 21, LEFT[6], RIGHT[6]):
        if slot in screen:
            ctx.shot(f"bliss-menu-tooltip-slot{slot}", ctx.tester.hover(slot, container=True), folder="menu")
    ctx.tester.hud(False)
    problems = []
    for n, gem in enumerate(GEMS, 1):
        for slot, tier, model in ((LEFT[n - 1], 1, 1000 + n + 40), (RIGHT[n - 1], 2, 2000 + n + 40)):
            it = screen.get(slot)
            if it is None:
                problems.append(f"{gem} T{tier} missing (slot {slot})")
            elif gem not in it["name"].lower() or it["id"] != "minecraft:nautilus_shell" or it.get("customModelData") != model:
                problems.append(f"slot {slot}: wanted {gem} T{tier} nautilus shell model {model}, got "
                                f"{it['name']!r} {it['id']} {it.get('customModelData')}")
    expect = {4: ("minecraft:nautilus_shell", 1009), 21: ("minecraft:amethyst_shard", 5003),
              23: ("minecraft:amethyst_shard", 5001), 31: ("minecraft:diamond_block", None)}
    for slot in (2, 11, 20, 29, 6, 15, 24, 33):
        expect[slot] = ("minecraft:blue_stained_glass_pane", None)
    for slot, (item, model) in expect.items():
        it = screen.get(slot)
        if it is None or it["id"] != item or (model and it.get("customModelData") != model):
            problems.append(f"slot {slot}: wanted {item} {model or ''}, got {it and (it['id'], it.get('customModelData'))}")
    check(not problems, "; ".join(problems))


@test("menu", "gold-gem-page-and-back")
def gold_page(ctx):
    ctx.world.reset_player(ctx.tester)
    open_menu(ctx)
    ctx.tester.click(4)
    time.sleep(1.0)
    page = {it["slot"]: it for it in ctx.tester.items_full().get("screen", [])}
    ctx.tester.hud(True)
    ctx.tester.hover(-1)
    ctx.shot("gold-gem-page", ctx.tester.screenshot(), folder="menu")
    ctx.tester.hud(False)
    wires = [page.get(10 + i, {}).get("customModelData") for i in range(7)]
    check(page.get(4, {}).get("customModelData") == 1009, f"no Gold Gem on top of the page: {page.get(4)}")
    check(wires == [7001, 7002, 7003, 7004, 7005, 7006, 7007], f"wire fragments 1-7 expected in slots 10-16, got {wires}")
    check(page.get(22, {}).get("customModelData") == 7000, f"no Fragment Core in slot 22: {page.get(22)}")
    ctx.tester.click(18)  # back
    time.sleep(1.0)
    back = {it["slot"]: it for it in ctx.tester.items_full().get("screen", [])}
    check(len(back) >= 16, f"Back did not return to the gem menu ({len(back)} items)")


@test("menu", "links-and-nothing-can-be-taken")
def links(ctx):
    ctx.world.reset_player(ctx.tester)
    for slot, url in ((21, MYTHICS_URL), (23, MYTHICS_URL), (31, EXPANSION_URL)):
        open_menu(ctx)
        ctx.tester.mark()
        ctx.tester.click(slot)
        time.sleep(1.0)
        chat = ctx.tester.chat_since_mark()
        check(any(url in line for line in chat), f"clicking slot {slot} should post {url}; chat: {chat[-3:]}")
    open_menu(ctx)
    for slot in (0, 2, 35, 20, 8):   # gems and glass only (Gold/Auratus/Heretic/More navigate away)
        ctx.tester.click(slot)
        time.sleep(0.3)
    ctx.tester.close_screen()
    time.sleep(0.5)
    check(not ctx.tester.items(), f"clicking the menu put items into the inventory: {ctx.tester.items()}")

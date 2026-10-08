"""Bugs that were fixed once and must stay fixed."""
import re
import time

from .harness import check, test


@test("regressions", "drift-cooldown-in-action-bar")
def drift_cooldown(ctx):
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "astra", 2)
    ctx.tester.select(0)
    ctx.tester.press("attack")      # slot 3 = Dimensional Drift
    time.sleep(1.5)
    ctx.tester.mark()
    time.sleep(1.5)
    bars = ctx.tester.actionbar_since_mark()
    ctx.note(f"action bar: {bars[-3:]}")
    check(any(re.search(r"\(\s*\d+s", b) for b in bars), "Drift's cooldown is not shown in the action bar")


@test("regressions", "cooldown-bossbars-only-for-bedrock-names")
def bossbars(ctx):
    w = ctx.world
    for bot, should_see in ((ctx.tester, False), (ctx.victim, True)):
        w.reset_player(bot, (0.5, -60, 0.5) if bot is ctx.tester else (8.5, -60, 0.5))
        w.give_gem(bot, "speed", 2)
        bot.select(0)
        bot.press("attack")         # slot 3 = Terminal Velocity, a timed ability
        time.sleep(1.5)
        bars = bot.state().get("bossBars", [])
        ctx.note(f"{bot.name}: boss bars {bars}")
        if should_see:
            check(bars, f"{bot.name} (Bedrock-style name) should see the ability boss bar")
        else:
            check(not bars, f"{bot.name} (Java name) should not get ability boss bars: {bars}")


@test("regressions", "ability-list-shows-default-keybinds")
def default_binds(ctx):
    ctx.world.reset_player(ctx.tester)
    ctx.tester.mark()
    ctx.tester.chat("/bliss ability list")
    time.sleep(1.0)
    lines = ctx.tester.chat_since_mark()
    check(any("(default:" in l for l in lines), f"/bliss ability list shows no defaults: {lines[:8]}")


@test("regressions", "anti-dupe-removes-a-second-gem")
def anti_dupe(ctx):
    """With single-gem-only, a second gem in the inventory (a dupe) must be deleted within a few seconds."""
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 2)
    w.give_item(ctx.tester, "speed_gem_t2")
    interval = (w.get_config("gems.anti-dupe.check-interval-ticks") or 40) / 20.0
    time.sleep(interval + 2.0)
    gems = [it for it in ctx.tester.items() if "gem" in (it.get("name") or "").lower() or it["id"] in
            ("minecraft:echo_shard", "minecraft:amethyst_shard", "minecraft:prismarine_crystals")]
    ctx.note(f"gems left after {interval + 2:.0f}s: {[(g['slot'], g['name']) for g in gems]}")
    check(len(gems) == 1, f"expected exactly one gem left after the anti-dupe check, found {len(gems)}")


@test("regressions", "gems-are-nautilus-shells-and-old-gems-convert")
def nautilus_gems(ctx):
    """Gems are nautilus shells (Bedrock off hand); a gem made before the switch (an echo shard) converts."""
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 2)
    items = ctx.tester.items()
    check(items and items[0]["id"] == "minecraft:nautilus_shell", f"a new gem should be a nautilus shell: {items[:1]}")
    w.cmd(f"clear {ctx.tester.name}")
    # an old-style gem: echo shard carrying the plugin's item id
    w.cmd(f'give {ctx.tester.name} minecraft:echo_shard[custom_model_data={{floats:[2002f]}},'
          f'custom_data={{PublicBukkitValues:{{"blissgems:item_id":"fire_gem_t2"}}}}]')
    time.sleep(0.5)
    w.cmd(f"bliss energy {ctx.tester.name} set 6")   # an energy change refreshes (and converts) gems
    time.sleep(1.0)
    items = ctx.tester.items()
    ctx.note(f"after the refresh: {[(i['id'], i['name']) for i in items]}")
    check(items and items[0]["id"] == "minecraft:nautilus_shell", "the old echo-shard gem was not converted")


@test("regressions", "controls-fire-the-same-ability-as-the-gem-handler")
def controls_match_handlers(ctx):
    """The controls (BlissCommand.handleAbility*) and each gem's onPrimary..onQuaternary must name the
    same ability for every slot - two tables that drift apart send a key to the wrong ability."""
    import re
    base = "/repo/src/main/java/dev/xoperr/blissgems/"
    src = open(base + "commands/BlissCommand.java").read()

    def body(name):
        m = re.search(r"private void " + name + r"\(CommandSender sender, String\[\] args\)\s*\{", src)
        i, d = m.end(), 1
        while d:
            d += {"{": 1, "}": -1}.get(src[i], 0)
            i += 1
        return src[m.end():i]

    controls = {}
    for slot, fn in (("Primary", "Main"), ("Secondary", "Secondary"), ("Tertiary", "Tertiary"), ("Quaternary", "Quaternary")):
        for gem, call in re.findall(r"case (\w+): \{\s*this\.plugin\.get\w+Abilities\(\)\.(\w+)\(", body("handleAbility" + fn)):
            controls[(gem.lower(), slot)] = call
    wrong = []
    for g in ("Astra", "Fire", "Flux", "Life", "Puff", "Speed", "Strength", "Wealth"):
        s = open(base + f"abilities/{g}Abilities.java").read()
        for slot in ("Primary", "Secondary", "Tertiary", "Quaternary"):
            m = re.search(r"public void on" + slot + r"\(Player player, int tier\) \{\s*this\.(\w+)\(", s)
            h, c = (m.group(1) if m else None), controls.get((g.lower(), slot))
            if not (c == h or (c and h and (c.lower() in h.lower() or h.lower() in c.lower()))):
                wrong.append(f"{g} {slot}: controls fire {c}, the gem handler says {h}")
    check(not wrong, "; ".join(wrong))

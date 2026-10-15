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


def _lore_line(items, starts):
    for it in items:
        for line in it.get("tooltip", []):
            if line.strip().startswith(starts):
                return line.strip()
    return None


@test("regressions", "upgrader-is-consumed")
def upgrader_consumed(ctx):
    """An Upgrader is single use: it used to keep charges, so one upgrader upgraded several gems."""
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 1)
    w.give_item(ctx.tester, "gem_upgrader")
    time.sleep(0.5)
    slot = next(it["slot"] for it in ctx.tester.items() if "upgrad" in it["name"].lower())
    ctx.tester.select(slot)
    ctx.tester.press("use")
    time.sleep(1.2)
    items = ctx.tester.items()
    ctx.note(f"after the upgrade: {[(i['slot'], i['name'], i.get('customModelData')) for i in items]}")
    check(any((i.get("customModelData") or 0) // 1000 == 2 for i in items), "the gem was not upgraded to Tier 2")
    check(not any("upgrad" in i["name"].lower() for i in items), "the upgrader is still in the inventory")


@test("regressions", "tier2-death-reverts-to-tier1")
def tier2_death(ctx):
    """Dying with a Tier 2 gem kept it Tier 2 while the upgrader dropped - a free upgrader per death."""
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 2)
    w.cmd(f"kill {ctx.tester.name}")
    time.sleep(3.0)
    items = ctx.tester.items()
    ctx.note(f"after respawn: {[(i['slot'], i['name'], i.get('customModelData')) for i in items]}")
    check(any((i.get("customModelData") or 0) // 1000 == 1 for i in items), "the gem did not come back as Tier 1")
    check(not any((i.get("customModelData") or 0) // 1000 == 2 for i in items), "the gem is still Tier 2 after death")


@test("regressions", "energy-bottle-does-not-revive-a-broken-gem")
def bottle_broken(ctx):
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 2)
    w.cmd(f"bliss energy {ctx.tester.name} set 0")
    w.give_item(ctx.tester, "energy_bottle")
    time.sleep(0.5)
    slot = next(it["slot"] for it in ctx.tester.items() if "minecraft:ghast_tear" == it["id"])
    ctx.tester.mark()
    ctx.tester.select(slot)
    ctx.tester.press("use")
    time.sleep(1.0)
    level = next((it["tooltip"][1].strip() for it in ctx.tester.items() if it["id"] == "minecraft:nautilus_shell"), None)
    ctx.note(f"gem level line after the bottle: {level!r}; chat {ctx.tester.chat_since_mark()[-2:]}")
    check(level is not None and "Broken" in level, f"a Broken gem was revived by an Energy Bottle (level now {level!r})")


@test("regressions", "passives-use-the-tier-of-a-hotbar-gem")
def hotbar_tier(ctx):
    """Strength T2 in the hotbar (not held) gave Strength I: the passive read the tier from the off hand only."""
    w = ctx.world
    # T1 and T2 ship with the same level; make T2 different so the test can tell them apart
    w.set_config({"passives.strength.tier2.strength-level": 2})
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "strength", 2)
    ctx.tester.select(4)   # the gem stays in hotbar slot 0, not in hand
    time.sleep(3.0)
    eff = ctx.tester.effects().get("minecraft:strength")
    ctx.note(f"strength effect: {eff}; tier2 strength-level set to 2 for this test")
    check(eff is not None, "no Strength at all from a T2 Strength gem in the hotbar")
    check(eff["amplifier"] == 2, f"Strength amplifier {eff['amplifier']} from a T2 gem in the hotbar, expected 2 (tier2 level)")

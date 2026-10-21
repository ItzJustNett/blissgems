"""
Every gem ability, through the real controls (Java default bindings):
  slot 1 = right click, 2 = shift + right click, 3 = hit (left click), 4 = shift + hit.

For each ability there are up to three tests:
  <gem>-<n>-works   the ability fires and does what it should (damage, effects, movement, ...),
                    and its cooldown / active time equals what config.yml says
  <gem>-<n>-config  the cooldown (or duration / damage) is changed in config.yml + /bliss reload,
                    and the new value shows up in-game - catches values hardcoded in Java
  <gem>-<n>-tier1   (Tier 2 abilities) a Tier 1 gem refuses it with "requires Tier 2"
"""
import math
import time

from .harness import Fail, Skip, check, test
from .world import TARGET

NEAR = (0.5, -60, 4.5)   # inside every area ability's radius, out of melee reach (3 blocks)
INPUT = {1: ("use", False), 2: ("use", True), 3: ("attack", False), 4: ("attack", True)}


def trigger(ctx, slot):
    key, sneak = INPUT[slot]
    ctx.tester.press(key, sneak=sneak)


def chat_has(ctx, text, bot=None):
    lines = (bot or ctx.tester).chat_since_mark()
    return any(text.lower() in l.lower() for l in lines), lines


def pos(bot):
    s = bot.state()
    return s["x"], s["y"], s["z"]


def dist(a, b):
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


# ---------------------------------------------------------------- per-ability expectations

def expect_effects(*ids, who="tester"):
    def f(ctx, before):
        bot = ctx.tester if who == "tester" else ctx.victim
        have = set(bot.effects())
        missing = [i for i in ids if i not in have]
        check(not missing, f"{who} is missing effects {missing}; has {sorted(have)}")
    return f


def expect_target_effects(*ids):
    def f(ctx, before):
        have = ctx.world.target_effects()
        missing = [i for i in ids if i not in have]
        if missing:
            ctx.note(f"raw: {ctx.world.last_raw}")
        check(not missing, f"target is missing effects {missing}; has {sorted(have)}")
    return f


def expect_target_damage(config_path, default, tolerance=3.0):
    """The target lost at least the configured amount of health (and not wildly more)."""
    def f(ctx, before):
        amount = ctx.world.get_config(config_path)
        amount = default if amount is None else float(amount)
        hp = ctx.world.target_health()
        lost = before["target_hp"] - hp
        ctx.note(f"target lost {lost:.1f} HP; {config_path} = {amount}")
        check(lost >= amount - 0.01, f"target lost {lost:.1f} HP, expected at least {amount} ({config_path})")
        check(lost <= amount + tolerance, f"target lost {lost:.1f} HP, expected about {amount} ({config_path})")
    return f


def expect_target_hurt():
    def f(ctx, before):
        hp = ctx.world.target_health()
        check(hp is not None and hp < before["target_hp"], f"target was not damaged ({before['target_hp']} -> {hp})")
    return f


def expect_moved(who="tester", at_least=1.5, up=False):
    """Samples the position for 2 s and takes the largest displacement (a launch can peak and land)."""
    def f(ctx, before):
        bot = ctx.tester if who == "tester" else ctx.victim
        start = before["pos_" + who]
        best, samples = 0.0, []
        end = time.time() + 2.0
        while time.time() < end:
            s = bot.state()
            now = (s["x"], s["y"], s["z"])
            d = (now[1] - start[1]) if up else dist(now, start)
            samples.append(f"{d:.2f}(vy {s.get('vy', 0):.2f})")
            best = max(best, d)
            time.sleep(0.2)
        ctx.note(f"{who} displacement samples: {' '.join(samples)}")
        what = "rose" if up else "moved"
        check(best >= at_least, f"{who} {what} at most {best:.2f} blocks, expected {at_least}+")
    return f


def expect_chat(text, who="tester"):
    def f(ctx, before):
        ok, lines = chat_has(ctx, text, ctx.tester if who == "tester" else ctx.victim)
        check(ok, f"{who} never got a message containing {text!r}; got {lines[-6:]}")
    return f


def expect_state(field, check_fn, label, who="tester"):
    def f(ctx, before):
        bot = ctx.tester if who == "tester" else ctx.victim
        value = bot.state().get(field)
        check(check_fn(value, before), f"{who} {field} = {value}: {label}")
    return f


def expect_entities(selector, at_least):
    def f(ctx, before):
        out = ctx.world.cmd(f"execute if entity {selector}")
        import re
        m = re.search(r"count:? (\d+)", out)
        n = int(m.group(1)) if m else 0
        check(n >= at_least, f"expected {at_least}+ of {selector}, found {n}")
    return f


# Each ability: gem, slot, key, name (in the activation text), tier (minimum), target,
# timer = ("cooldown", path) | ("active", path) - what /bliss cooldowns must show right after use,
# expect = checks after use, setup / before / after = extra steps, cfg = a config change whose
# effect is checked instead of the timer (path, value, check-after).
A = []


def ability(gem, slot, key, name, tier=2, target=None, timer=None, expect=(), setup=None, uses=1,
            gap=0.4, wait=1.2, cfg=None, after=None, pitch=0, note=None):
    A.append(dict(gem=gem, slot=slot, key=key, name=name, tier=tier, target=target, timer=timer,
                  expect=list(expect), setup=setup, uses=uses, gap=gap, wait=wait, cfg=cfg, after=after,
                  pitch=pitch, note=note))


def cd(key):
    return ("cooldown", f"abilities.cooldowns.{key}")


def act(path):
    return ("active", path)


def end_projection(ctx):
    ctx.world.cmd(f"gamemode survival {ctx.tester.name}")


def give_victim_effects(ctx):
    ctx.world.cmd("effect give @e[tag=bt] minecraft:speed 120 1")
    ctx.world.cmd("effect give @e[tag=bt] minecraft:strength 120 0")


def victim_holds_item(ctx):
    ctx.world.cmd(f"item replace entity {ctx.victim.name} weapon.mainhand with minecraft:diamond_sword")


def hurt_victim(ctx):
    ctx.world.cmd(f"damage {ctx.victim.name} 8 minecraft:generic")


def offline_uuid_ints(name):
    """The UUID an offline-mode server gives a name, as the int array NBT uses."""
    import hashlib, struct
    b = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    b[6] = (b[6] & 0x0F) | 0x30
    b[8] = (b[8] & 0x3F) | 0x80
    return ",".join(str(x) for x in struct.unpack(">4i", bytes(b)))


def tester_holds_victim_head(ctx):
    # Shadow Stalker reads the head from the MAIN hand, where the gem has to be to fire it - so the
    # head goes in the off hand, as a player would try. (See the test note: this cannot work.)
    ctx.world.cmd(f"item replace entity {ctx.tester.name} weapon.offhand with minecraft:player_head"
                  f"[profile={{name:\"{ctx.victim.name}\",id:[I;{offline_uuid_ints(ctx.victim.name)}]}}]")


def flux_watts(ctx):
    ctx.world.cmd(f"bliss setwatts {ctx.tester.name} 1000")


# --- Astra
ability("astra", 1, "astra-daggers", "Astral Daggers", tier=1, uses=6, gap=0.5,
        timer=cd("astra-daggers"),
        note="1st click conjures 5 daggers, the next 5 launch them; the cooldown starts after the last")
ability("astra", 2, "astra-projection", "Astral Projection",
        expect=[expect_state("gameMode", lambda v, b: v == "spectator", "should be spectator while projecting")],
        timer=act("abilities.durations.astra-projection"), after=end_projection)
ability("astra", 3, "astra-drift", "Dimensional Drift",
        expect=[expect_effects("minecraft:invisibility"), expect_moved(at_least=2.0)], timer=cd("astra-drift"))
ability("astra", 4, "astra-void", "Dimensional Void", target="victim-near",
        expect=[expect_chat("nullified", who="victim")], timer=act("abilities.durations.astra-void"))
# --- Fire
ability("fire", 1, "fire-fireball", "fire", tier=1, uses=2, gap=1.5,
        expect=[expect_chat("Charging fireball")], timer=cd("fire-fireball"),
        note="1st click starts charging, the 2nd fires")
ability("fire", 2, "fire-campfire", "Campfire", target="mob", pitch=15,
        expect=[expect_chat("Placed Campfire")], timer=act("abilities.durations.fire-campfire"), wait=2.5,
        cfg=("abilities.damage.fire-campfire", 9.0, expect_target_hurt()))
ability("fire", 3, "fire-crisp", "Crisp", expect=[expect_chat("Crisp!")], timer=act("abilities.durations.fire-crisp"))
ability("fire", 4, "fire-meteor-shower", "Meteor Shower", target="mob", pitch=15,
        expect=[expect_chat("Meteor Shower!")], timer=act("abilities.durations.fire-meteor-shower"))
# --- Flux
ability("flux", 1, "flux-beam", "Flux Beam", tier=1, setup=flux_watts,
        expect=[expect_chat("Charging Flux Beam")], timer=None,
        note="only checks that charging starts; the beam fires once charged past 10%")
ability("flux", 2, "flux-ground", "Ground", target="mob",
        expect=[expect_target_effects("minecraft:slowness", "minecraft:mining_fatigue", "minecraft:weakness")],
        timer=act("abilities.durations.flux-ground-freeze"))
ability("flux", 3, "flux-flashbang", "Flashbang", target="victim-near",
        expect=[expect_effects("minecraft:blindness", "minecraft:nausea", who="victim")],
        timer=act("abilities.durations.flux-flashbang"))
ability("flux", 4, "flux-kinetic-burst", "Kinetic Burst", target="victim-near",
        expect=[expect_moved(who="victim", at_least=1.0)], timer=act("abilities.durations.flux-kinetic-burst"))
# --- Life
ability("life", 1, "life-drainer", "Heart Drainer", tier=1, target="mob",
        expect=[expect_target_damage("abilities.life-drainer.steal-hp", 4.0), expect_target_effects("minecraft:wither")],
        timer=act("abilities.durations.life-drainer"),
        cfg=("abilities.life-drainer.steal-hp", 11.0, expect_target_damage("abilities.life-drainer.steal-hp", 11.0)))
ability("life", 2, "life-circle-of-life", "Circle of Life",
        expect=[expect_state("maxHealth", lambda v, b: v and v >= b["max_hp"] + 2, "max health should go up")],
        timer=act("abilities.durations.life-circle"))
ability("life", 3, "life-vitality-vortex", "Vitality Vortex",
        expect=[expect_effects("minecraft:regeneration")], timer=act("abilities.durations.life-vitality-vortex"),
        note="superflat is plains: Regeneration II + Saturation")
ability("life", 4, "life-heart-lock", "Heart Lock", target="victim", setup=hurt_victim,
        expect=[expect_state("maxHealth", lambda v, b: v is not None and v <= 13, "max health should lock to current health", who="victim")],
        timer=act("abilities.durations.life-heart-lock"))
# --- Puff
ability("puff", 1, "puff-dash", "Dash", tier=1, expect=[expect_moved(at_least=2.0)], timer=cd("puff-dash"))
ability("puff", 2, "puff-breezy-bash", "Breezy Bash", expect=[expect_moved(at_least=1.0, up=True)], timer=cd("puff-breezy-bash"))
ability("puff", 3, "puff-group-bash", "Group Breezy Bash", target="victim-near",
        expect=[expect_moved(who="victim", at_least=1.0)], timer=cd("puff-group-bash"))
ability("puff", 4, "puff-updraft", "Updraft", target="victim-near",
        expect=[expect_chat("updraft flings you", who="victim"), expect_moved(who="victim", at_least=1.0, up=True)],
        timer=cd("puff-updraft"))
# --- Speed
ability("speed", 1, "speed-blur", "Blur", tier=1, timer=act("abilities.blur.charge-timeout-seconds"),
        note="Blur gives strike charges for abilities.blur.charge-timeout-seconds; the cooldown starts after")
ability("speed", 2, "speed-storm", "Speed Storm", target="victim-near",
        expect=[expect_effects("minecraft:speed", "minecraft:haste"), expect_effects("minecraft:slowness", who="victim")],
        timer=act("abilities.durations.speed-storm"))
ability("speed", 3, "speed-terminal", "Terminal Velocity",
        expect=[expect_effects("minecraft:speed", "minecraft:haste")], timer=act("abilities.durations.terminal-velocity"))
ability("speed", 4, "speed-gale-clouds", "Lightning", target="victim",
        expect=[expect_state("health", lambda v, b: v is not None and v <= b["victim_hp"] - 5.9, "should lose 3 hearts", who="victim")],
        timer=None, note="Lightning Backstab: teleports behind the target, 3 hearts true damage (hardcoded). "
        "Known plugin bug: the controls fire Gale Clouds on slot 4, the gem's own handler says Lightning Backstab")
# --- Strength
ability("strength", 1, "strength-chad", "Chad Strength", timer=act("abilities.strength-chad.duration"),
        expect=[expect_chat("hits are empowered")])
ability("strength", 2, "strength-frailer", "Frailer", target="mob",
        expect=[expect_target_effects("minecraft:weakness", "minecraft:wither", "minecraft:slowness")],
        timer=cd("strength-frailer"))
ability("strength", 3, "strength-shadow-stalker", "Shadow Stalker", target="victim", setup=tester_holds_victim_head,
        expect=[expect_chat("Now tracking"), expect_chat("being hunted", who="victim")],
        timer=act("abilities.strength-shadow-stalker.duration"),
        note="Known plugin bug: the ability reads the target's head from the main hand, but it only fires with the gem in the main hand")
ability("strength", 4, "strength-nullify", "Nullify", target="mob", setup=give_victim_effects,
        expect=[lambda ctx, b: check("minecraft:speed" not in ctx.world.target_effects(), "target still has Speed after Nullify")],
        timer=cd("strength-nullify"))
# --- Wealth
ability("wealth", 1, "wealth-unfortunate", "Unfortunate", target="victim",
        expect=[expect_chat("afflicted with Unfortunate", who="victim")], timer=act("abilities.durations.wealth-unfortunate"))
ability("wealth", 2, "wealth-rich-rush", "Rich Rush",
        expect=[expect_effects("minecraft:haste", "minecraft:luck")], timer=act("abilities.durations.wealth-rich-rush"))
ability("wealth", 3, "wealth-item-lock", "Item Lock", target="victim", setup=victim_holds_item,
        expect=[expect_chat("has been locked", who="victim")], timer=act("abilities.durations.wealth-item-lock"))
ability("wealth", 4, "wealth-amplification", "Amplification",
        timer=act("abilities.durations.wealth-amplification"))


# ---------------------------------------------------------------- running one ability

def arrange(ctx, a, tier):
    w = ctx.world
    w.reset_player(ctx.tester, pitch=a["pitch"])
    if a["target"] == "mob":
        w.spawn_target()
    elif a["target"] == "victim":
        w.place_victim(TARGET)
    elif a["target"] == "victim-near":
        w.place_victim(NEAR)
    if a["target"] not in ("victim", "victim-near"):
        w.reset_player(ctx.victim, (20.5, -60, -20.5))  # behind the tester, out of every ray and radius
    w.give_gem(ctx.tester, a["gem"], tier)
    ctx.tester.select(0)
    if a["setup"]:
        a["setup"](ctx)
    time.sleep(0.5)


def snapshot(ctx):
    t = ctx.tester.state()
    v = ctx.victim.state()
    return {"pos_tester": (t["x"], t["y"], t["z"]), "pos_victim": (v["x"], v["y"], v["z"]),
            "max_hp": t.get("maxHealth"), "victim_hp": v.get("health"),
            "target_hp": ctx.world.target_health()}


def use(ctx, a, tier):
    arrange(ctx, a, tier)
    held = ctx.tester.state().get("mainHand")
    check(held and held != "minecraft:air", f"no gem in the main hand after /bliss give (holding {held})")
    before = snapshot(ctx)
    ctx.tester.mark()
    ctx.victim.mark()
    for i in range(a["uses"]):
        trigger(ctx, a["slot"])
        time.sleep(a["gap"])
    time.sleep(a["wait"])
    return before


def check_timer(ctx, a):
    if not a["timer"]:
        return
    kind, path = a["timer"]
    want = ctx.world.get_config(path)
    cds = ctx.world.cooldowns(ctx.tester)
    got = cds.get(a["key"], {}).get(kind)
    ctx.note(f"/bliss cooldowns: {cds.get(a['key'])}; {path} = {want}")
    check(got is not None, f"no {kind} timer for {a['key']} after using it (config {path} = {want}); timers: {cds}")
    if want is not None:
        # read ~1-3 s after use, so the remaining time is a little under the configured value
        check(int(want) - 4 <= got <= int(want), f"{kind} for {a['key']} is {got}s, config {path} says {want}s")


def make_tests(a):
    label = f"{a['gem']}-{a['slot']}"

    def works(ctx):
        if a["note"]:
            ctx.note(a["note"])
        before = use(ctx, a, 2)
        try:
            for e in a["expect"]:
                e(ctx, before)
            check_timer(ctx, a)
        except Fail:
            ctx.note(f"tester chat: {ctx.tester.chat_since_mark()[-8:]}")
            ctx.note(f"victim chat: {ctx.victim.chat_since_mark()[-8:]}")
            raise
        finally:
            ctx.shot(f"{label}-{a['key']}", ctx.tester.screenshot(), folder="abilities")
            if a["after"]:
                a["after"](ctx)

    def config(ctx):
        if a["note"] and "Known plugin bug" in a["note"]:
            raise Skip("blocked by the known bug in the -works test")
        if a["cfg"]:
            path, value, after_check = a["cfg"]
            ctx.world.set_config({path: value})
            before = use(ctx, a, 2)
            try:
                after_check(ctx, before)
            except Fail:
                ctx.note(f"tester chat: {ctx.tester.chat_since_mark()[-8:]}")
                raise
        elif a["timer"]:
            kind, path = a["timer"]
            new = 37 if kind == "cooldown" else 7
            ctx.world.set_config({path: new})
            use(ctx, a, 2)
            check_timer(ctx, a)
        else:
            raise Skip("no configurable value that can be read back in-game")
        if a["after"]:
            a["after"](ctx)

    def tier1(ctx):
        use(ctx, a, 1)
        ok, lines = chat_has(ctx, "Tier 2")
        cds = ctx.world.cooldowns(ctx.tester)
        check(ok, f"a Tier 1 gem should refuse {a['key']} with 'requires Tier 2'; chat: {lines[-5:]}")
        check(a["key"] not in cds, f"{a['key']} started a timer on a Tier 1 gem: {cds}")
        if a["after"]:
            a["after"](ctx)

    # config first: it runs with a short duration, so nothing is still active for the normal test
    test("abilities-config", f"{label}-{a['key']}-config")(config)
    test("abilities", f"{label}-{a['key']}-works")(works)
    if a["tier"] == 2:
        test("abilities-tier", f"{label}-{a['key']}-tier1")(tier1)


for _a in A:
    make_tests(_a)

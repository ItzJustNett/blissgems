"""Passives (gem in the off hand), each checked against its config value and again after changing it."""
import time

from .harness import check, test


def offhand_gem(ctx, gem, tier):
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, gem, tier)
    w.cmd(f"item replace entity {ctx.tester.name} weapon.offhand from entity {ctx.tester.name} hotbar.0")
    w.cmd(f"item replace entity {ctx.tester.name} hotbar.0 with minecraft:air")
    time.sleep(2.5)  # passives tick every second


def effect_level_test(gem, effect, path):
    def run(ctx, change):
        if change:
            ctx.world.set_config({path: 3})
        want = int(ctx.world.get_config(path))
        offhand_gem(ctx, gem, 2)
        eff = ctx.tester.effects().get(effect)
        ctx.note(f"{effect}: {eff}; {path} = {want}")
        check(eff is not None, f"no {effect} from the {gem} passive")
        check(eff["amplifier"] == want, f"{effect} amplifier {eff['amplifier']}, config {path} says {want}")

    test("passives", f"{gem}-{effect.split(':')[1]}")(lambda ctx: run(ctx, False))
    test("passives-config", f"{gem}-{effect.split(':')[1]}-config")(lambda ctx: run(ctx, True))


effect_level_test("speed", "minecraft:speed", "passives.speed.tier2.speed-level")
effect_level_test("strength", "minecraft:strength", "passives.strength.tier2.strength-level")
effect_level_test("wealth", "minecraft:luck", "passives.wealth.tier2.luck-level")


@test("passives", "fire-fire-resistance")
def fire_resistance(ctx):
    offhand_gem(ctx, "fire", 2)
    check("minecraft:fire_resistance" in ctx.tester.effects(), "no Fire Resistance from the fire passive")


def life_heal(ctx, amount_hearts):
    if amount_hearts is not None:
        ctx.world.set_config({"passives.life.tier2.heal-amount": amount_hearts})
    hearts = float(ctx.world.get_config("passives.life.tier2.heal-amount"))
    interval = int(ctx.world.get_config("passives.life.tier2.heal-interval")) / 20.0
    offhand_gem(ctx, "life", 2)
    ctx.world.cmd(f"damage {ctx.tester.name} 10 minecraft:generic")
    time.sleep(0.5)
    start = ctx.tester.state()["health"]
    time.sleep(interval * 2 + 0.5)
    end = ctx.tester.state()["health"]
    healed = end - start
    ctx.note(f"healed {healed:.2f} HP over {interval * 2 + 0.5:.1f}s; config {hearts} hearts every {interval}s")
    # two heal ticks in that window: 2 x hearts x 2 HP (one more or less depending on timing)
    check(hearts * 2 * 1 - 0.01 <= healed <= hearts * 2 * 3 + 0.01,
          f"healed {healed:.2f} HP, expected about {hearts * 2 * 2:.1f} (2 x {hearts} hearts)")


test("passives", "life-heal")(lambda ctx: life_heal(ctx, None))
test("passives-config", "life-heal-config")(lambda ctx: life_heal(ctx, 2.0))


@test("regressions", "life-shield-does-not-refill-every-second")
def life_shield(ctx):
    """5.1.0 bug: the Life absorption shield was re-applied every second, so Life players could not die."""
    ctx.world.set_config({"passives.life.tier2.absorption-per-block": 2})
    offhand_gem(ctx, "life", 2)
    time.sleep(1.5)
    full = ctx.tester.state()["absorption"]
    check(full > 0, "the Life shield never appeared (absorption-per-block = 2)")
    ctx.world.cmd(f"damage {ctx.tester.name} {int(full)} minecraft:generic")
    time.sleep(3.0)
    now = ctx.tester.state()["absorption"]
    ctx.note(f"shield {full} -> hit for {int(full)} -> {now} three seconds later")
    check(now < full, f"the shield refilled to {now} within 3 s of breaking (regen cooldown should apply)")

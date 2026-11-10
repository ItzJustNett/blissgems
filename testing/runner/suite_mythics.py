"""
BlissMythics (Heretic, Auratus) on top of BlissGems. The addon is only on the test server when the
run is started with WITH_MYTHICS=1 (testing/run-all.sh reads it); without it these tests skip.
"""
import time

from .harness import Skip, check, test


def require_mythics(ctx):
    if "BlissMythics" not in ctx.world.cmd("plugins"):
        raise Skip("BlissMythics is not on the test server (run with WITH_MYTHICS=1)")


def slot_of(bot, name):
    """The slot of the first item whose name contains this (gems all share one material)."""
    return next((it["slot"] for it in bot.items() if name in (it.get("name") or "").lower()), None)


@test("mythics", "given-mythic-stores-gem-and-drop-returns-it")
def give_and_drop(ctx):
    """/bliss give of a mythic stores the player's gem instead of deleting it; dropping the mythic hands it back."""
    require_mythics(ctx)
    w = ctx.world
    w.reset_player(ctx.tester)
    w.give_gem(ctx.tester, "fire", 2)
    w.give_gem(ctx.tester, "heretic", 2)
    time.sleep(1.5)
    ctx.note(f"after the give: {[(i['slot'], i['id'], i.get('name')) for i in ctx.tester.items()]}")
    heretic = slot_of(ctx.tester, "heretic")
    check(heretic is not None, "no Heretic gem after /bliss give")
    check(slot_of(ctx.tester, "fire") is None, "the Fire gem is still in the inventory next to the mythic")
    check(heretic < 9, f"Heretic is not in the hotbar (slot {heretic})")
    ctx.tester.select(heretic)
    time.sleep(0.3)
    ctx.tester.press("drop")
    time.sleep(1.5)
    items = ctx.tester.items()
    ctx.note(f"after the drop: {[(i['slot'], i['id'], i.get('name')) for i in items]}")
    check(slot_of(ctx.tester, "heretic") is None, "Heretic is still held after dropping it")
    check(slot_of(ctx.tester, "fire") is not None, "the Fire gem did not come back after the mythic was dropped")


@test("mythics", "emp-switches-mythic-passives-off")
def emp_passives(ctx):
    """While an EMP is down, Heretic's Auto Crit (x1.5 melee damage) must not apply."""
    require_mythics(ctx)
    w = ctx.world
    w.set_config({"abilities.fire-vow.emp-effect-seconds": 10, "abilities.fire-vow.heart-cost-seconds": 1,
                  "abilities.fire-vow.gem-disabled-seconds": 1})
    w.reset_player(ctx.tester)
    w.reset_player(ctx.victim, (8.5, -60, 0.5))
    w.cmd(f"bliss vow {ctx.victim.name} emp")
    w.give_gem(ctx.tester, "heretic", 2)
    time.sleep(1.0)
    # a bare fist: a click with the gem in hand is an ability input, not a plain punch
    check(slot_of(ctx.tester, "heretic") != 4, "Heretic landed in hotbar slot 4")
    ctx.tester.select(4)
    time.sleep(1.0)

    # one target for both punches: a freshly killed one keeps soaking up the client's clicks for a second
    w.spawn_target((0.5, -60, 2.5))

    def punch():
        before = w.target_health()
        ctx.tester.press("attack")
        time.sleep(1.0)
        return before - w.target_health()

    try:
        normal = punch()
        ctx.victim.chat("/bliss emp")
        time.sleep(1.5)
        jammed = punch()
        ctx.note(f"punch with Heretic: {normal} damage; during the EMP: {jammed}")
        check(abs(normal - 1.5) < 0.1, f"a fist with Heretic should hit for 1.5 (1 x Auto Crit), did {normal}")
        check(abs(jammed - 1.0) < 0.1, f"during the EMP a fist should hit for a plain 1.0, did {jammed}")
    finally:
        w.cmd(f"bliss vow {ctx.victim.name} off")
        time.sleep(12.0)   # the EMP, its heart cost and the gem lock run out

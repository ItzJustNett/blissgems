"""Test registry and result recording. A test is a function(ctx) that raises Fail/Skip or returns."""
import os
import time
import traceback


class Fail(AssertionError):
    pass


class Skip(Exception):
    pass


def check(cond, message):
    if not cond:
        raise Fail(message)


class Result:
    def __init__(self, suite, name):
        self.suite = suite
        self.name = name
        self.status = "pass"
        self.message = ""
        self.details = []
        self.shots = []      # (label, relative png path) - for the visual review
        self.seconds = 0.0

    def as_dict(self):
        return {"suite": self.suite, "name": self.name, "status": self.status, "message": self.message,
                "details": self.details, "shots": [{"label": l, "path": p} for l, p in self.shots],
                "seconds": round(self.seconds, 2)}


class Ctx:
    """What a test gets: bots, rcon, world helpers, and a place to put details and screenshots."""

    def __init__(self, out_dir, world, tester, victim, rcon, update_goldens):
        self.out = out_dir
        self.world = world
        self.tester = tester
        self.victim = victim
        self.rcon = rcon
        self.update_goldens = update_goldens
        self.result = None

    def note(self, text):
        self.result.details.append(str(text))

    def shot(self, label, png_bytes, folder="shots"):
        safe = "".join(c if c.isalnum() or c in "-_." else "_" for c in label)
        rel = f"{folder}/{safe}.png"
        os.makedirs(os.path.join(self.out, folder), exist_ok=True)
        with open(os.path.join(self.out, rel), "wb") as f:
            f.write(png_bytes)
        self.result.shots.append((label, rel))
        return rel


TESTS = []


def test(suite, name=None):
    def deco(fn):
        TESTS.append((suite, name or fn.__name__, fn))
        return fn
    return deco


def run_all(ctx, only=None, log=print):
    results = []
    for suite, name, fn in TESTS:
        if only and suite not in only and f"{suite}/{name}" not in only:
            continue
        r = Result(suite, name)
        ctx.result = r
        start = time.time()
        try:
            fn(ctx)
        except Skip as e:
            r.status, r.message = "skip", str(e)
        except Fail as e:
            r.status, r.message = "fail", str(e)
        except Exception as e:  # a broken test or a crash in the bot/server is an error, not a pass
            r.status, r.message = "error", f"{type(e).__name__}: {e}"
            r.details.append(traceback.format_exc())
        finally:
            r.seconds = time.time() - start
            try:
                ctx.world.after_test()
            except Exception as e:
                r.details.append(f"cleanup failed: {e}")
        log(f"  [{r.status.upper():5}] {suite}/{name} ({r.seconds:.1f}s) {r.message}")
        results.append(r)
    return results

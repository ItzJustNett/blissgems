"""Test runner entry point (inside the container): python3 -m runner.main --out /out [--only suite,...]"""
import argparse
import glob
import json
import os
import re
import sys
import time

from . import harness, report
from . import suite_abilities, suite_items, suite_passives, suite_regressions  # noqa: F401 (register tests)
from .bot import Bot
from .rcon import Rcon
from .world import World


def unit_results(out):
    """Turns the surefire reports from the build into results, so the report has everything."""
    results = []
    for path in sorted(glob.glob(os.path.join(out, "surefire", "*.txt"))):
        text = open(path).read()
        m = re.search(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)", text)
        if not m:
            continue
        run, fails, errs, skipped = map(int, m.groups())
        name = os.path.basename(path)[:-4].rsplit(".", 1)[-1]
        status = "fail" if fails or errs else "pass"
        results.append({"suite": "unit", "name": f"{name} ({run} tests)", "status": status,
                        "message": text[-3000:] if status == "fail" else "", "details": [], "shots": [], "seconds": 0})
    return results


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--pack-url", required=True)
    ap.add_argument("--only", default="", help="comma-separated suites or suite/test names")
    ap.add_argument("--update-goldens", action="store_true")
    args = ap.parse_args()
    out = args.out
    only = set(filter(None, args.only.split(",")))

    rcon = Rcon()
    tester = Bot("Tester", os.environ["BOT_A_PORT"])
    victim = Bot(".Victim", os.environ["BOT_B_PORT"])
    meta = {"version": re.search(r"<version>([^<]+)</version>", open("/repo/pom.xml").read()).group(1),
            "server": os.environ.get("MC_VERSION", "1.21.11"), "time": time.strftime("%F %T")}
    world = World(rcon, tester, victim)
    try:
        print("waiting for the bot clients to load...", flush=True)
        for b in (tester, victim):
            b.wait_api()
        address = f"127.0.0.1:{os.environ['SERVER_PORT']}"
        for b in (tester, victim):
            b.connect(address, args.pack_url)
            print(f"  {b.name} joined", flush=True)
        time.sleep(5)  # resource pack download + world load
        world.prepare_server()
    except Exception as e:
        report.write(out, unit_results(out) + [{"suite": "setup", "name": "bots join the server", "status": "error",
                     "message": f"{type(e).__name__}: {e}", "details": [], "shots": [], "seconds": 0}], meta)
        print(f"setup failed: {e}", flush=True)
        sys.exit(1)
    ctx = harness.Ctx(out, world, tester, victim, rcon, args.update_goldens)
    print(f"running {len(harness.TESTS)} in-game tests", flush=True)
    results = [r.as_dict() for r in harness.run_all(ctx, only or None, log=lambda s: print(s, flush=True))]

    server_log = open(os.path.join(out, "server.log"), errors="replace").read()
    exceptions = [l for l in server_log.splitlines() if "[BlissGems]" in l and ("Exception" in l or "SEVERE" in l or "ERROR" in l)]
    results.append({"suite": "server", "name": "no BlissGems errors in the server log",
                    "status": "fail" if exceptions else "pass", "message": "\n".join(exceptions[:30]),
                    "details": [], "shots": [], "seconds": 0})

    data = report.write(out, unit_results(out) + results, meta)
    s = data["summary"]
    print(f"done: {s}", flush=True)
    sys.exit(0 if not s.get("fail") and not s.get("error") else 1)


if __name__ == "__main__":
    main()

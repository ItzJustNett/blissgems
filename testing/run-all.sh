#!/usr/bin/env bash
# Runs the whole BlissGems test suite in one Docker container and writes testing/results/.
#
#   testing/run-all.sh                    everything
#   testing/run-all.sh --only items       one suite (items, abilities, abilities-config,
#                                         abilities-tier, passives, passives-config, regressions)
#   testing/run-all.sh --update-goldens   take this run's tooltips as the new expected ones
#
# A full pass writes testing/tested.json (version, source fingerprint, counts) with the visual
# review and the player check still "pending"; testing/approve.sh records those afterwards.
set -euo pipefail
cd "$(dirname "$0")"
REPO="$(cd .. && pwd)"

./deps.sh
echo "building the test image (cached after the first time)..."
docker build --network host -q -t blissgems-test . >/dev/null

rm -rf results && mkdir -p results
echo "running (server log, bot logs and the report go to testing/results/)..."
set +e
docker run --rm --network host --name blissgems-test \
  -v "$REPO:/repo:ro" -v "$PWD/.cache:/cache:ro" -v "$PWD/results:/out" -v blissgems-test-m2:/root/.m2 \
  blissgems-test "$@"
code=$?
set -e

python3 - "$code" "$@" <<'PY'
import json, sys
code = int(sys.argv[1]); partial = any(a.startswith("--only") for a in sys.argv[2:])
try:
    data = json.load(open("results/results.json"))
except Exception:
    print("no results.json - the run did not get far; see testing/results/container.log"); sys.exit(0)
s = data["summary"]
print(f"\nresult: {s.get('pass',0)} passed, {s.get('fail',0)} failed, {s.get('error',0)} errors, {s.get('skip',0)} skipped")
for r in data["results"]:
    if r["status"] in ("fail", "error"):
        print(f"  {r['status'].upper()} {r['suite']}/{r['name']}: {r['message'].splitlines()[0] if r['message'] else ''}")
print("report: testing/results/report.html")
if partial:
    print("(partial run: tested.json not written)")
PY

if [ "$code" = 0 ] && ! printf '%s\n' "$@" | grep -q -- '--only'; then
  python3 - "$REPO" <<'PY'
import json, re, subprocess, sys, time
repo = sys.argv[1]
data = json.load(open("results/results.json"))
lock = json.load(open("deps.lock"))
version = re.search(r"<version>([^<]+)</version>", open(f"{repo}/pom.xml").read()).group(1)
source = subprocess.check_output([sys.executable, "tools/source_hash.py", repo], text=True).strip()
json.dump({"version": version, "sourceHash": source, "mcVersion": lock["mcVersion"],
           "summary": data["summary"], "testedAt": time.strftime("%F %T"),
           "visualReview": "pending", "playerCheck": "pending"}, open("tested.json", "w"), indent=2)
print(f"testing/tested.json written for {version} (visual review + player check pending)")
PY
fi
exit "$code"

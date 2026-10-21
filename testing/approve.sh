#!/usr/bin/env bash
# Records the two manual gates in testing/tested.json after a passing run:
#   testing/approve.sh visual   the bliss-tester agent found the screenshots OK
#   testing/approve.sh player   the user joined the test server and said it is OK
# The release workflow only publishes when both are "approved" and the code is unchanged.
set -euo pipefail
cd "$(dirname "$0")"
[ -f tested.json ] || { echo "no tested.json - run testing/run-all.sh first"; exit 1; }
case "${1:-}" in
  visual) field=visualReview ;;
  player) field=playerCheck ;;
  *) echo "usage: approve.sh visual|player"; exit 1 ;;
esac
current="$(python3 tools/source_hash.py ..)"
python3 - "$field" "$current" <<'PY'
import json, sys, time
field, current = sys.argv[1], sys.argv[2]
t = json.load(open("tested.json"))
if t["sourceHash"] != current:
    sys.exit("the code changed since the tests ran - run testing/run-all.sh again")
t[field] = "approved"
t[field + "At"] = time.strftime("%F %T")
json.dump(t, open("tested.json", "w"), indent=2)
print(f"{field} approved")
PY

"""
Release gate (run by .github/workflows/build.yml before publishing a release): the build must have
passed testing/run-all.sh locally, been reviewed (agent + player), and not changed since.
"""
import json
import os
import re
import subprocess
import sys

root = sys.argv[1] if len(sys.argv) > 1 else "."
path = os.path.join(root, "testing", "tested.json")
if not os.path.exists(path):
    sys.exit("testing/tested.json is missing: run testing/run-all.sh (see the bliss-test skill) before releasing")
t = json.load(open(path))
version = re.search(r"<version>([^<]+)</version>", open(os.path.join(root, "pom.xml")).read()).group(1)
source = subprocess.check_output([sys.executable, os.path.join(root, "testing", "tools", "source_hash.py"), root], text=True).strip()
problems = []
if t.get("version") != version:
    problems.append(f"tested.json is for {t.get('version')}, pom.xml is {version}")
if t.get("sourceHash") != source:
    problems.append("the code changed after the tests ran (source fingerprint differs)")
s = t.get("summary", {})
if s.get("fail") or s.get("error"):
    problems.append(f"the recorded run had failures: {s}")
for gate in ("visualReview", "playerCheck"):
    if t.get(gate) != "approved":
        problems.append(f"{gate} is {t.get(gate)!r}, not approved")
if problems:
    sys.exit("Release blocked:\n- " + "\n- ".join(problems))
print(f"tested.json OK: {version}, tested {t.get('testedAt')}, {s}")

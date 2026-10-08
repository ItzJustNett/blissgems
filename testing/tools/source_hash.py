"""
Fingerprint of everything that goes into the plugin jar (pom.xml + src/). run-all.sh stores it in
testing/tested.json after a passing run; the release workflow recomputes it and refuses to
publish if the code changed since the tests ran. Same result locally and in CI.
"""
import hashlib
import os
import sys

root = sys.argv[1] if len(sys.argv) > 1 else "."
paths = ["pom.xml"]
for base, _, names in os.walk(os.path.join(root, "src")):
    for n in names:
        paths.append(os.path.relpath(os.path.join(base, n), root))
h = hashlib.sha256()
for p in sorted(x.replace(os.sep, "/") for x in paths):
    h.update(p.encode() + b"\0")
    with open(os.path.join(root, p), "rb") as f:
        h.update(hashlib.sha256(f.read()).digest())
print(h.hexdigest())

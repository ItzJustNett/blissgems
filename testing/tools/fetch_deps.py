"""Downloads what deps.lock pins into the cache dir and verifies every sha256. Idempotent."""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import urllib.request

UA = {"User-Agent": "blissgems-tests/1.0 (github.com/ItzJustNett/blissgems)"}
lock_path, cache = sys.argv[1], sys.argv[2]
lock = json.load(open(lock_path))


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch(dep, dest_dir):
    os.makedirs(dest_dir, exist_ok=True)
    dest = os.path.join(dest_dir, dep["name"])
    if os.path.exists(dest) and sha256(dest) == dep["sha256"]:
        return
    print(f"  downloading {dep['name']} ({dep.get('version', '')})")
    with urllib.request.urlopen(urllib.request.Request(dep["url"], headers=UA), timeout=300) as r, open(dest + ".part", "wb") as out:
        shutil.copyfileobj(r, out)
    got = sha256(dest + ".part")
    if got != dep["sha256"]:
        os.remove(dest + ".part")
        sys.exit(f"sha256 mismatch for {dep['name']}: expected {dep['sha256']}, got {got}")
    os.replace(dest + ".part", dest)


print("Fetching test dependencies into", cache)
for dep in lock["server"].values():
    fetch(dep, os.path.join(cache, "server"))
fetch(lock["client"]["fabricApi"], os.path.join(cache, "client"))

# auk-control mod source at the pinned commit (built inside the image)
auk = lock["client"]["aukcontrol"]
src_dir = os.path.join(cache, "aukcontrol-src")
local = os.environ.get("AUKCONTROL_SRC", os.path.expanduser("~/projects/auk/auk-control-fabric-mod"))
have = subprocess.run(["git", "-C", src_dir, "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip() if os.path.isdir(src_dir) else ""
if have != auk["commit"]:
    shutil.rmtree(src_dir, ignore_errors=True)
    os.makedirs(src_dir)
    subprocess.run(["git", "-C", src_dir, "init", "-q"], check=True)
    # GitHub first; a local checkout when the commit is not pushed yet
    ok = subprocess.run(["git", "-C", src_dir, "fetch", "-q", "--depth", "1", auk["repo"], auk["commit"]], capture_output=True).returncode == 0
    if not ok and os.path.isdir(local):
        ok = subprocess.run(["git", "-C", src_dir, "fetch", "-q", os.path.abspath(local), auk["commit"]], capture_output=True).returncode == 0
    if not ok:
        sys.exit(f"auk-control commit {auk['commit']} not found on GitHub or in {local} (set AUKCONTROL_SRC)")
    subprocess.run(["git", "-C", src_dir, "checkout", "-q", "FETCH_HEAD"], check=True)
    print("  auk-control mod source at", auk["commit"][:10])

# resource pack: the shipped zip, with the repo's resourcepack/ files laid over it
rp = lock["resourcePack"]
pack_src = os.environ.get("BLISS_PACK", os.path.expanduser("~/Downloads/" + rp["zip"]))
if not os.path.exists(pack_src):
    sys.exit(f"resource pack not found: {pack_src} (set BLISS_PACK)")
if sha256(pack_src) != rp["sha256"]:
    sys.exit(f"resource pack {pack_src} does not match deps.lock (run ./deps.sh --update after changing it)")
os.makedirs(os.path.join(cache, "pack"), exist_ok=True)
shutil.copyfile(pack_src, os.path.join(cache, "pack", "base.zip"))
print("Dependencies ready.")

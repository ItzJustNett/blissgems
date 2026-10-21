"""Looks up the newest builds of every test dependency and prints a deps.lock (JSON) to stdout."""
import hashlib
import json
import os
import subprocess
import urllib.parse
import urllib.request

UA = {"User-Agent": "blissgems-tests/1.0 (github.com/ItzJustNett/blissgems)"}
MC = os.environ.get("MC_VERSION", "1.21.11")
CLIENT_MC = "1.21.1"  # the bot client (auk-control mod) only exists for 1.21.1


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def paper():
    builds = get(f"https://fill.papermc.io/v3/projects/paper/versions/{MC}/builds")
    stable = [b for b in builds if b.get("channel") == "STABLE"] or builds
    b = max(stable, key=lambda b: b["id"])
    d = b["downloads"]["server:default"]
    return {"name": f"paper-{MC}-{b['id']}.jar", "url": d["url"], "sha256": d["checksums"]["sha256"],
            "version": f"{MC}-{b['id']}"}


def modrinth(project, loader, game_version=None, file_name=None):
    q = {"loaders": json.dumps([loader])}
    if game_version:
        q["game_versions"] = json.dumps([game_version])
    versions = get(f"https://api.modrinth.com/v2/project/{project}/version?" + urllib.parse.urlencode(q))
    releases = [v for v in versions if v["version_type"] == "release"] or versions
    v = releases[0]
    f = next(f for f in v["files"] if f["primary"])
    with urllib.request.urlopen(urllib.request.Request(f["url"], headers=UA), timeout=120) as r:
        sha = hashlib.sha256(r.read()).hexdigest()
    return {"name": file_name or f["filename"], "url": f["url"], "sha256": sha, "version": v["version_number"]}


def aukcontrol():
    # the mod is built from source inside the image; pin the commit the tests were written against
    src = os.environ.get("AUKCONTROL_SRC", os.path.expanduser("~/projects/auk/auk-control-fabric-mod"))
    commit = subprocess.check_output(["git", "-C", src, "rev-parse", "HEAD"], text=True).strip()
    return {"repo": "https://github.com/ItzJustNett/auk-control-fabric-mod", "commit": commit}


lock = {
    "mcVersion": MC,
    "clientMcVersion": CLIENT_MC,
    "server": {
        "paper": paper(),
        "viaversion": modrinth("viaversion", "paper", file_name="ViaVersion.jar"),
        "viabackwards": modrinth("viabackwards", "paper", file_name="ViaBackwards.jar"),
    },
    "client": {
        "fabricApi": modrinth("fabric-api", "fabric", CLIENT_MC, file_name="fabric-api.jar"),
        "aukcontrol": aukcontrol(),
    },
    "resourcePack": {"inRepo": "resourcepack"},
}
print(json.dumps(lock, indent=2))

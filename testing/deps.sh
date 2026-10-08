#!/usr/bin/env bash
# Fetches everything the test container needs into testing/.cache, pinned by deps.lock:
# Paper, ViaVersion, ViaBackwards (server side), Fabric API (bot client), the auk-control mod
# source, and the resource pack. Every download is checked against the sha256 in deps.lock.
#
#   ./deps.sh            use the pinned versions in deps.lock
#   ./deps.sh --update   look up the newest builds, rewrite deps.lock, then fetch them
#
# MC_VERSION (default 1.21.11) picks the server version when updating.
set -euo pipefail
cd "$(dirname "$0")"
CACHE=.cache
mkdir -p "$CACHE"
MC_VERSION="${MC_VERSION:-1.21.11}"

if [ "${1:-}" = "--update" ]; then
  MC_VERSION="$MC_VERSION" python3 tools/resolve_deps.py > deps.lock.new
  mv deps.lock.new deps.lock
  echo "deps.lock updated"
fi

python3 tools/fetch_deps.py deps.lock "$CACHE"

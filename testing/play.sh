#!/usr/bin/env bash
# Starts the tested build on a local server for you to join: 127.0.0.1:25565 (Minecraft 1.21.11),
# with the BlissGems resource pack. Same container image as the tests. Stop with:
#   docker stop blissgems-play
#   PLAY_OP=<your name> testing/play.sh   makes you op
set -euo pipefail
cd "$(dirname "$0")"
REPO="$(cd .. && pwd)"
PORT="${PLAY_PORT:-25565}"
if ss -ltn 2>/dev/null | grep -q ":$PORT "; then
  echo "port $PORT is busy (another server running?) - stop it or set PLAY_PORT"; exit 1
fi
./deps.sh >/dev/null
docker build --network host -q -t blissgems-test . >/dev/null
docker rm -f blissgems-play >/dev/null 2>&1 || true
mkdir -p results-play
docker run -d --rm --network host --name blissgems-play -e SERVER_PORT="$PORT" -e PLAY_OP="${PLAY_OP:-}" \
  -v "$REPO:/repo:ro" -v "$PWD/.cache:/cache:ro" -v "$PWD/results-play:/out" -v blissgems-test-m2:/root/.m2 \
  blissgems-test --play >/dev/null
echo "starting... (log: testing/results-play/server.log)"
for _ in $(seq 1 240); do
  grep -q "PLAY SERVER READY" results-play/container.log 2>/dev/null && { echo "ready: join 127.0.0.1:$PORT"; exit 0; }
  grep -q "FAILED" results-play/container.log 2>/dev/null && { echo "build failed - see testing/results-play/maven.log"; exit 1; }
  sleep 1
done
echo "server did not come up in 4 minutes - see testing/results-play/"; exit 1

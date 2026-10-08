#!/usr/bin/env bash
# Runs inside the test container. /repo is the BlissGems checkout (read-only), /out gets the
# results. Steps: build the plugin (unit tests run as part of the build), build the resource
# pack, start Paper and two bot clients, run the in-game tests, stop everything.
set -euo pipefail
OUT=/out
W=/work
mkdir -p "$OUT" "$W"
: > "$OUT/container.log"
log() { echo "[$(date +%T)] $*" | tee -a "$OUT/container.log"; }

SERVER_PORT="${SERVER_PORT:-25699}"
RCON_PORT=25698
PACK_PORT=25680
BOT_A_PORT=25681   # "Tester" - the Java player who uses the gems
BOT_B_PORT=25682   # ".Victim" - a Bedrock-style name (Floodgate "." prefix), the target in PvP tests
RCON_PASSWORD="$(head -c 18 /dev/urandom | base64 | tr -dc A-Za-z0-9)"
export SERVER_PORT RCON_PORT RCON_PASSWORD BOT_A_PORT BOT_B_PORT

cleanup() {
  log "stopping"
  python3 /repo/testing/runner/rcon.py stop >/dev/null 2>&1 || true
  for _ in $(seq 1 30); do pgrep -f paper.jar >/dev/null || break; sleep 1; done
  pkill -f paper.jar 2>/dev/null || true
  pkill -f portablemc 2>/dev/null || true
  pkill -f "net.fabricmc" 2>/dev/null || true
  pkill Xvfb 2>/dev/null || true
  pkill -f "http.server" 2>/dev/null || true
}
trap cleanup EXIT

# --- 1. build the plugin; the unit tests (config wiring etc.) run here and fail the build ---
log "building BlissGems (unit tests included)"
rm -rf "$W/src" && mkdir -p "$W/src"
cp -r /repo/pom.xml /repo/src /repo/resourcepack "$W/src/"
if ! (cd "$W/src" && mvn -B -q package > "$OUT/maven.log" 2>&1); then
  mkdir -p "$OUT/surefire" && cp "$W"/src/target/surefire-reports/*.txt "$OUT/surefire/" 2>/dev/null || true
  log "BUILD OR UNIT TESTS FAILED - see maven.log"
  python3 /repo/testing/runner/report.py --unit-failed "$OUT"
  exit 2
fi
mkdir -p "$OUT/surefire" && cp "$W"/src/target/surefire-reports/*.txt "$OUT/surefire/" 2>/dev/null || true
PLUGIN_JAR="$(ls "$W"/src/target/BlissGems-*.jar | grep -v original | head -1)"
log "built $(basename "$PLUGIN_JAR")"

# --- 2. resource pack (resourcepack/ + a 1.21.1 layer for the bot client), served on localhost ---
mkdir -p "$W/pack/rp"
python3 /repo/testing/tools/build_pack.py - /repo/resourcepack "$W/pack/rp/pack.zip" | tee -a "$OUT/container.log"
PACK_SHA1="$(sha1sum "$W/pack/rp/pack.zip" | cut -d' ' -f1)"
(cd "$W/pack" && python3 -m http.server "$PACK_PORT" --bind 127.0.0.1 >/dev/null 2>&1 &)

# --- 3. Paper server ---
log "starting Paper"
rm -rf "$W/server" && cp -r /opt/paper "$W/server"
mkdir -p "$W/server/plugins" "$W/server/config"
cp "$PLUGIN_JAR" "$W/server/plugins/BlissGems.jar"
cp /opt/deps/ViaVersion.jar /opt/deps/ViaBackwards.jar "$W/server/plugins/"
echo "eula=true" > "$W/server/eula.txt"
sed -e "s/@SERVER_PORT@/$SERVER_PORT/" -e "s/@RCON_PORT@/$RCON_PORT/" -e "s/@RCON_PASSWORD@/$RCON_PASSWORD/" \
    -e "s#@PACK_URL@#http://127.0.0.1:$PACK_PORT/rp/pack.zip#" -e "s/@PACK_SHA1@/$PACK_SHA1/" \
    /repo/testing/container/server.properties > "$W/server/server.properties"
cp /repo/testing/container/paper-global.yml "$W/server/config/paper-global.yml"
(cd "$W/server" && exec java -Xms1G -Xmx2G -jar paper.jar --nogui > "$OUT/server.log" 2>&1 &)
for i in $(seq 1 180); do
  grep -q 'Done (' "$OUT/server.log" 2>/dev/null && break
  sleep 1
  [ "$i" = 180 ] && { log "server did not start in 180 s"; exit 3; }
done
log "server up"

# --play: just the server, for a person to join and look around (testing/play.sh)
if [ "${1:-}" = "--play" ]; then
  python3 /repo/testing/runner/rcon.py "op ${PLAY_OP:-}" >/dev/null 2>&1 || true
  log "PLAY SERVER READY on 127.0.0.1:$SERVER_PORT - stop with: docker stop blissgems-play"
  trap - EXIT
  tail -f "$OUT/server.log" & wait
fi

# --- 4. two bot clients on one virtual display ---
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99
Xvfb :99 -screen 0 1920x1080x24 -nolisten tcp >/dev/null 2>&1 &
start_bot() {  # name port
  local dir="$W/bot-$2"
  mkdir -p "$dir/mods"
  cp /opt/botmods/*.jar "$dir/mods/"
  # no music/sounds, no narrator, no realms nag
  printf 'soundCategory_master:0.0\nnarrator:0\nonboardAccessibility:false\nskipMultiplayerWarning:true\npauseOnLostFocus:false\n' > "$dir/options.txt"
  (DISPLAY=:99 AUK_MOD_PORT="$2" exec python3 -m portablemc --main-dir /opt/mc --work-dir "$dir" start fabric:1.21.1:0.16.10 \
     --jvm "$(command -v java)" --jvm-args="-Xmx1536M -XX:+UseG1GC" -u "$1" --resolution 1920x1080 \
     > "$OUT/bot-$1.log" 2>&1 &)
}
start_bot Tester "$BOT_A_PORT"
start_bot .Victim "$BOT_B_PORT"

# --- 5. the in-game tests ---
log "running in-game tests"
set +e
(cd /repo/testing && python3 -m runner.main --out "$OUT" --pack-url "http://127.0.0.1:$PACK_PORT/rp/" "$@")
code=$?
set -e
log "runner exit code $code"
exit $code

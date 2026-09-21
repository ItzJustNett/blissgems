#!/usr/bin/env bash
# Gracefully stops MinecraftServerForTests by sending "stop" over its console pipe
# (same effect as typing "stop" at the console — saves the world). Never kill -9.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_DIR="$REPO_ROOT/MinecraftServerForTests"
FIFO="$SERVER_DIR/console.in"
LOG="$SERVER_DIR/logs/run.log"

is_alive() {
    timeout 2 bash -c "echo '' > '$FIFO'" 2>/dev/null
}

if [ ! -p "$FIFO" ] || ! is_alive; then
    echo "Server is not running."
    exit 0
fi

echo "stop" > "$FIFO"
echo "Sent stop command, waiting for shutdown..."

STABLE=0
PREV_SIZE=-1
for i in $(seq 1 30); do
    sleep 1
    CUR_SIZE=$(stat -c%s "$LOG" 2>/dev/null || echo -1)
    if [ "$CUR_SIZE" = "$PREV_SIZE" ]; then
        STABLE=$((STABLE + 1))
    else
        STABLE=0
    fi
    PREV_SIZE=$CUR_SIZE

    if [ "$STABLE" -ge 3 ] && ! is_alive; then
        echo "Server stopped after ${i}s."
        exit 0
    fi
done

echo "Server hasn't confirmed shutdown after 30s. Not force-killing — check $LOG manually."
exit 1

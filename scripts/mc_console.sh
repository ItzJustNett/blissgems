#!/usr/bin/env bash
# Sends a console command to the running MinecraftServerForTests instance via its
# named-pipe stdin (see start-server.sh for why this exists instead of RCON).
# Usage: mc_console.sh <command...>
#   scripts/mc_console.sh say Plugin updated: fixed astra dagger cooldown
#   scripts/mc_console.sh stop
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIFO="$REPO_ROOT/MinecraftServerForTests/console.in"

if [ $# -eq 0 ]; then
    echo "usage: mc_console.sh <command...>" >&2
    exit 1
fi

if [ ! -p "$FIFO" ] || ! timeout 3 bash -c "cat > '$FIFO'" <<< "$*" 2>/dev/null; then
    echo "Server is not running (console pipe not accepting input)." >&2
    exit 1
fi
echo "Sent: $*"

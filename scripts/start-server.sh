#!/usr/bin/env bash
# Runs MinecraftServerForTests in the FOREGROUND, with its console stdin wired to a
# named pipe (console.in) so other, separate tool calls can send it commands later.
#
# Two environment quirks this works around:
# 1. Self-backgrounding (`... &`, even with nohup/disown) does NOT survive here — the
#    sandboxed shell that launched it tears down the whole process group as soon as the
#    launching call returns. Invoke this script via a real background-task mechanism
#    (Claude's Bash tool with run_in_background:true) instead; exec-ing in the
#    foreground means the process this script becomes IS the tracked background task.
# 2. Separate tool calls run in isolated network namespaces, so RCON (a TCP connection)
#    from a later call can't reach a server started in an earlier call. The filesystem
#    IS shared across calls, so a named pipe for console stdin works where RCON can't.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVER_DIR="$REPO_ROOT/MinecraftServerForTests"
FIFO="$SERVER_DIR/console.in"

if [ -p "$FIFO" ] && timeout 2 bash -c "echo '' > '$FIFO'" 2>/dev/null; then
    echo "Server already running (console is responding)."
    exit 0
fi

[ -p "$FIFO" ] || mkfifo "$FIFO"
mkdir -p "$SERVER_DIR/logs"
cd "$SERVER_DIR"

exec 3<>"$FIFO"
exec ./start.sh 0<&3 3<&-

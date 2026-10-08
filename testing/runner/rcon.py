"""Minimal RCON client for the test server (localhost only). CLI: python3 rcon.py <command...>"""
import os
import re
import socket
import struct
import sys

COLOR = re.compile(r"§.")


class Rcon:
    def __init__(self, host="127.0.0.1", port=None, password=None):
        self.addr = (host, int(port or os.environ["RCON_PORT"]))
        self.password = password or os.environ["RCON_PASSWORD"]
        self.sock = None
        self.next_id = 1

    def _send(self, kind, body):
        rid = self.next_id
        self.next_id += 1
        data = struct.pack("<ii", rid, kind) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)
        return rid

    def _recv(self):
        def read(n):
            buf = b""
            while len(buf) < n:
                chunk = self.sock.recv(n - len(buf))
                if not chunk:
                    raise ConnectionError("RCON connection closed")
                buf += chunk
            return buf
        (length,) = struct.unpack("<i", read(4))
        payload = read(length)
        rid, kind = struct.unpack("<ii", payload[:8])
        return rid, kind, payload[8:-2].decode("utf-8", "replace")

    def connect(self):
        self.sock = socket.create_connection(self.addr, timeout=15)
        rid = self._send(3, self.password)
        got, _, _ = self._recv()
        if got == -1 or got != rid:
            raise PermissionError("RCON login refused")

    def cmd(self, command):
        """Runs a console command; returns its output with colour codes removed."""
        if self.sock is None:
            self.connect()
        try:
            self._send(2, command)
            _, _, body = self._recv()
        except (OSError, ConnectionError):
            self.sock = None
            self.connect()
            self._send(2, command)
            _, _, body = self._recv()
        return COLOR.sub("", body)


if __name__ == "__main__":
    print(Rcon().cmd(" ".join(sys.argv[1:])))

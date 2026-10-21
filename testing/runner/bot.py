"""Drives one headless client through the auk-control mod's HTTP API (127.0.0.1 only)."""
import json
import time
import urllib.error
import urllib.request


class BotError(RuntimeError):
    pass


class Bot:
    def __init__(self, name, port):
        self.name = name
        self.base = f"http://127.0.0.1:{port}"
        self.msg_seq = 0

    def _call(self, path, body=None, timeout=30.0, raw=False):
        data = None if body is None else json.dumps(body).encode()
        req = urllib.request.Request(self.base + path, data=data, method="POST" if data is not None else "GET",
                                     headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=timeout) as r:
                out = r.read()
        except urllib.error.HTTPError as e:
            raise BotError(f"{self.name} {path}: HTTP {e.code} {e.read()[:200]!r}") from None
        except (urllib.error.URLError, OSError) as e:
            raise BotError(f"{self.name} {path}: {e}") from None
        return out if raw else json.loads(out)

    # --- lifecycle ---
    def wait_api(self, timeout=300):
        end = time.time() + timeout
        while time.time() < end:
            try:
                s = self.state()
                # title screen up and resources loaded: no world, some screen showing
                if not s.get("connected") and s.get("screen"):
                    return s
            except BotError:
                pass
            time.sleep(2)
        raise BotError(f"{self.name}: client did not come up in {timeout}s")

    def connect(self, address, pack_url, timeout=120):
        self._call("/connect", {"address": address, "resourcePackUrl": pack_url}, timeout=200)
        end = time.time() + timeout
        while time.time() < end:
            s = self.state()
            if s.get("connected") and s.get("x") is not None:
                return s
            time.sleep(1)
        raise BotError(f"{self.name}: did not join the server in {timeout}s")

    # --- reading ---
    def state(self):
        return self._call("/state")

    def items(self):
        return self._call("/items")["items"]

    def items_full(self):
        """{"items": [...player inventory...], "screen": [...open container...], "screenTitle": "..."}"""
        return self._call("/items")

    def messages(self, since=None):
        r = self._call(f"/messages?since={self.msg_seq if since is None else since}")
        return r["messages"]

    def mark(self):
        """Remember where the message log is now; read_since_mark() returns only newer messages."""
        self.msg_seq = self._call("/messages?since=0")["last"]

    def chat_since_mark(self):
        return [m["text"] for m in self.messages() if not m["overlay"]]

    def actionbar_since_mark(self):
        return [m["text"] for m in self.messages() if m["overlay"]]

    def effects(self):
        return {e["id"]: e for e in self.state().get("effects", [])}

    # --- acting ---
    def chat(self, text):
        self._call("/chat", {"message": text})

    def look(self, yaw, pitch):
        self._call("/look", {"yaw": yaw, "pitch": pitch})

    def select(self, slot):
        self._call("/input", {"slot": slot, "ms": 50})

    def press(self, key, sneak=False, hold_ms=300):
        """One tap of use / attack / swapHands, optionally while sneaking (sneak goes down first)."""
        body = {"press": [key], "sneak": sneak, "ms": hold_ms}
        if sneak:
            body["pressAfterMs"] = 200
            body["ms"] = max(hold_ms, 400)
        self._call("/input", body)

    def open_inventory(self):
        self._call("/inventory/open", {})

    def close_screen(self):
        self._call("/screen/close", {})

    def hover(self, slot, container=False):
        return self._call("/hover", {"slot": slot, "container": container}, raw=True)

    def click(self, slot, container=True, right=False):
        self._call("/click", {"slot": slot, "container": container, "right": right})

    def screenshot(self):
        return self._call("/screenshot", {}, raw=True)

    def hud(self, hidden):
        self._call("/hud", {"hidden": hidden})

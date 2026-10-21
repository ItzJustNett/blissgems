"""Server-side helpers for tests: reset players, give gems, read cooldowns, edit config, targets."""
import copy
import os
import re
import time

import yaml

ARENA = (0.5, -60, 0.5)          # superflat surface; the tester stands here facing +z (yaw 0)
TARGET = (0.5, -60, 6.5)         # 6 blocks ahead: in ability range, out of melee reach
CONFIG = "/work/server/plugins/BlissGems/config.yml"


class World:
    def __init__(self, rcon, tester, victim):
        self.rcon = rcon
        self.tester = tester
        self.victim = victim
        self.pristine_config = None
        self.config_dirty = False

    def cmd(self, command):
        return self.rcon.cmd(command)

    # --- setup / reset ---
    def prepare_server(self):
        for rule in ("doDaylightCycle false", "doWeatherCycle false", "doMobSpawning false", "keepInventory true",
                     "doImmediateRespawn true", "announceAdvancements false", "doFireTick false",
                     "advance_time false", "advance_weather false", "spawn_mobs false", "keep_inventory true",
                     "immediate_respawn true", "fire_spread_radius_around_player 0",
                     "naturalRegeneration false", "natural_health_regeneration false"):
            self.cmd(f"gamerule {rule}")  # pre- and post-1.21.11 names; the wrong ones just fail
        self.cmd("time set noon")
        self.reset_arena()
        self.cmd("weather clear")
        for name in (self.tester.name, self.victim.name):
            self.cmd(f"op {name}")
        with open(CONFIG) as f:
            self.pristine_config = f.read()

    def reset_player(self, bot, pos=ARENA, yaw=0, pitch=0):
        n = bot.name
        for c in (f"clear {n}", f"effect clear {n}", f"gamemode survival {n}", f"bliss clearcds {n}",
                  f"attribute {n} minecraft:max_health base reset", f"bliss energy {n} set 10",
                  f"tp {n} {pos[0]} {pos[1]} {pos[2]} {yaw} {pitch}"):
            self.cmd(c)
        self.cmd(f"effect give {n} minecraft:instant_health 1 10 true")
        self.cmd(f"effect give {n} minecraft:saturation 1 10 true")
        time.sleep(0.6)
        self.cmd(f"effect clear {n}")
        bot.look(yaw, pitch)

    def reset_arena(self):
        """Puts the superflat arena back (meteors and Crisp change the ground) and removes every entity."""
        self.cmd("kill @e[type=!minecraft:player]")
        for y0, y1 in ((-60, -51), (-50, -41)):
            self.cmd(f"fill -20 {y0} -20 20 {y1} 20 minecraft:air")
        self.cmd("fill -20 -63 -20 20 -62 20 minecraft:dirt")
        self.cmd("fill -20 -61 -20 20 -61 20 minecraft:grass_block")

    def wait_abilities_over(self, limit=20):
        """Timed abilities keep running inside the plugin after a test (Crisp, Void, Projection...);
        the next test would hit "already active". Wait until both bots have no active timer left."""
        end = time.time() + limit
        while time.time() < end:
            if not any("active" in t for b in (self.tester, self.victim) for t in self.cooldowns(b).values()):
                return
            time.sleep(1)

    def after_test(self):
        self.wait_abilities_over()
        self.cmd(f"gamemode survival {self.tester.name}")
        self.reset_arena()
        for b in (self.tester, self.victim):
            try:
                b.close_screen()
            except Exception:
                pass
        if self.config_dirty:
            self.restore_config()

    # --- gems ---
    def give_gem(self, bot, gem, tier):
        out = self.cmd(f"bliss give {bot.name} {gem} {tier}")
        time.sleep(0.4)
        return out

    def give_item(self, bot, item_id, amount=1):
        return self.cmd(f"bliss giveitem {bot.name} {item_id} {amount}")

    def cooldowns(self, bot):
        """{ability_key: {"cooldown": seconds, "active": seconds}} from /bliss cooldowns."""
        out = self.cmd(f"bliss cooldowns {bot.name}")
        result = {}
        for line in out.splitlines():
            m = re.match(r"\s*([a-z0-9-]+) (.+)$", line.strip())
            if not m or "cooldowns:" in line:
                continue
            vals = dict(re.findall(r"(cooldown|active) (\d+)", m.group(2)))
            if vals:
                result[m.group(1)] = {k: int(v) for k, v in vals.items()}
        return result

    # --- config ---
    def set_config(self, changes):
        """Edits the live config.yml (dotted paths) and runs /bliss reload. Restored after the test."""
        with open(CONFIG) as f:
            data = yaml.safe_load(f)
        for path, value in changes.items():
            node = data
            parts = path.split(".")
            for p in parts[:-1]:
                node = node.setdefault(p, {})
            node[parts[-1]] = value
        with open(CONFIG, "w") as f:
            yaml.safe_dump(data, f, sort_keys=False)
        self.config_dirty = True
        return self.cmd("bliss reload")

    def get_config(self, path):
        data = yaml.safe_load(open(CONFIG))
        for p in path.split("."):
            if not isinstance(data, dict) or p not in data:
                return None
            data = data[p]
        return data

    def restore_config(self):
        with open(CONFIG, "w") as f:
            f.write(self.pristine_config)
        self.cmd("bliss reload")
        self.config_dirty = False

    # --- targets ---
    def spawn_target(self, pos=TARGET, health=1000):
        """A pillager with no AI and lots of health, facing the tester; tagged bt."""
        self.cmd("kill @e[tag=bt]")
        self.cmd(f"summon minecraft:pillager {pos[0]} {pos[1]} {pos[2]} "
                 f"{{NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"bt\"],Rotation:[180f,0f],"
                 f"attributes:[{{id:\"minecraft:max_health\",base:{health}}},{{id:\"minecraft:knockback_resistance\",base:1}}],"
                 f"Health:{health}f}}")
        time.sleep(0.3)

    def target_health(self):
        out = self.cmd("data get entity @e[tag=bt,limit=1] Health")
        m = re.search(r"(-?[\d.]+)f", out)
        return float(m.group(1)) if m else None

    COMMON_EFFECTS = ("speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health", "instant_damage",
                      "jump_boost", "nausea", "regeneration", "resistance", "fire_resistance", "water_breathing",
                      "invisibility", "blindness", "night_vision", "hunger", "weakness", "poison", "wither",
                      "health_boost", "absorption", "saturation", "glowing", "levitation", "luck", "unluck",
                      "slow_falling", "darkness")

    def target_effects(self):
        """Effects on the test target, asked one by one: `data get` cuts long output short with '...'."""
        have = set()
        for e in self.COMMON_EFFECTS:
            out = self.cmd(f'data get entity @e[tag=bt,limit=1] active_effects[{{id:"minecraft:{e}"}}].id')
            if "following entity data" in out:
                have.add(f"minecraft:{e}")
        self.last_raw = f"{sorted(have)}"
        return have

    def target_pos(self):
        out = self.cmd("data get entity @e[tag=bt,limit=1] Pos")
        nums = re.findall(r"(-?[\d.]+)d", out)
        return tuple(float(x) for x in nums[:3]) if len(nums) >= 3 else None

    def place_victim(self, pos=TARGET):
        self.reset_player(self.victim, pos, yaw=180)

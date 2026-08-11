# BlissGems v4.9.9 / BlissMythics v1.5.5 - Fixes & Controls

### 💗 Life Gem Stops Hurting Your Friends
- **Heart Drainer no longer works on trusted players.** It was the only Life ability missing a trust check — Circle of Life and Heart Lock already had one — so the primary happily withered and drained your own team.

---

### 🟡 Gold Gem Fixes
- **A new Gold Gem is actually new.** Clearing a filled gem and handing out another used to give back an already-awakened one: the link between a gem and its harvested souls only lived in memory and was lost on every relog or restart, after which the old souls latched onto whatever gem you picked up next. That link is now saved to disk.
- **`/bliss give <player> gold 2` tells you what's going on** instead of failing with "Failed to give gem!". The Gold Gem has no Tier 2 — its secondary abilities come from the **tier of the soul it's channelling**, so fill a soul at Tier 2 and select it.
- **The gilded armour trim is now Flow**, not Sentry. Change it with `gold.armour-trim.pattern` (any vanilla pattern, incl. `FLOW` and `BOLT`).

---

### 🛠️ New Gold Gem Admin Commands
- `/bliss goldgem remove <player> <soul>` — take a single soul back out.
- `/bliss goldgem clear <player>` — empty the gem back to dormant.
- `/bliss goldgem list <player>` — see every soul it holds, its tier, and which one is active.
- Joins the existing `/bliss goldgem fill <player> <soul> <tier>`. All four tab-complete, and removing the active soul auto-selects another.

---

### ⌨️ Keybinds You Can Actually Configure
- New **`ability-bindings`** section in `config.yml` sets the server-wide defaults for which input fires which ability slot, plus a separate **`bedrock-defaults`** for Floodgate players.
- Don't want left-click spent on the Gold Gem's extra slots? Set `left_click: none` and it keeps its vanilla behaviour.
- Players still override these for themselves with **`/bliss set_ability <slot> <input>`** — which, along with `/bliss ability`, now actually appears in the help text and tab-completion.

---

### ☠️ Mythic Death Drops
- **`gems.droppable-on-death` matching is fixed.** It was case-sensitive and exact-match, so `- Auratus` or `- auratus_gem_t1` silently did nothing. It now ignores case and accepts either the plain gem id or the full item id.
- **Typos are reported.** Any entry naming no known gem is logged to the console at startup instead of quietly never dropping.
- **No more double drops.** BlissMythics adds mythic gems to the drops itself, so a mythic listed in `droppable-on-death` dropped twice. It now skips a gem that's already dropping.

---

*BlissMythics 1.5.5 is required alongside BlissGems 4.9.9 for the mythic drop fix.*

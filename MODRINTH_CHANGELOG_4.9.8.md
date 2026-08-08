# BlissGems v4.9.8 - The Harvest Update

### ✨ Harvest Ceremony
- The blow that would **kill a gem holder is caught before it lands**. Both players are frozen in place and untouchable.
- The victim's gem **tears loose and rises spinning in the air** between the two of you.
- **Both cameras are pulled onto it** with a slow drifting sway — neither player can look away.
- **Both screens wash over in the stolen gem's colour** for the whole thing.
- When the gem finishes rising it goes into the Gold Gem and the kill goes through, still credited to the holder.
- Kills the ceremony never sees (a fall finishing the fight, `/kill`) still harvest the old way.
- Fully configurable: `gold.harvest-ceremony.enabled`, `duration-ticks`, `rise-height`, `screen-tint`.

---

### 🔮 Stolen Gems Finally Work Properly
- **Every passive of a harvested gem now fires.** Puff's double jump, Wealth's durability chip and armor mend, Strength's bloodthorns, Astra/Life/Flux on-hit passives — all of them were silently dead before.
- **Passives stack.** Four harvested souls = four full sets of passives at once.
- Each soul runs at **the tier it was taken at** — a gem torn off a Tier 1 victim keeps Tier 1 numbers.

---

### 🎮 Two New Keybinds - Use All Four Abilities
- New ability slots **Quinary** and **Senary**, bound to **Left-Click** and **Shift + Left-Click** by default.
- The Gold Gem uses them for the channelled soul's **tertiary and quaternary** — so you now get **all 4 abilities of a stolen gem instead of 2**.
- **F** and **Shift + F** still hold the Sundering Beam and the soul menu.
- Other gems don't define these slots, so left-click keeps working normally for them.
- Also reachable via `/bliss ability:quinary` and `/bliss ability:senary`, and rebindable like every other input.

---

### ☠️ Death Now Costs You Everything
- **Every stolen gem is returned to the player you took it from** — into their offhand if they're online, queued and handed over on their next login if they aren't.
- **Your own Gold Gem breaks.** It does nothing at all until you reforge it with a **Restoration Book**.
- Both switchable: `gold.death.return-souls`, `gold.death.break-gem`.

---

### 🏆 One Gold Gem Per Server
- With `gold.summon.once-per-server` on, the summon craft works **exactly once**. After that the result blanks out in the crafting grid.
- The summon is **announced server-wide** and recorded permanently.
- Lose it, and your only option is hunting down whoever has it.

---

### 🥇 Gilded Armour
- Carrying the Gold Gem puts a **gold trim on your armour**.
- Armour you already trimmed yourself is left untouched, and the plugin only ever removes its own gilding.
- Pattern configurable via `gold.armour-trim.pattern`.

---

### 📖 Restoration Ritual Can't Be Interrupted
- The caster is **untouchable and rooted in place** for the 15 seconds the ritual runs.
- You can still **look around freely** and watch it play out.

---

### 🛠️ All Crafting Recipes Are Now Configurable
- New **`recipes.yml`** — shape, ingredients, output amount and an on/off switch for **all 9 recipes**, no code changes needed.
- Ingredients can be plain materials (`DIAMOND_BLOCK`) or exact custom items (`blissgems:wire_fragment`), so the Gold Gem summon still refuses seven ordinary lightning rods.
- Includes the Restoration Book, Repair Kit, Revive Beacon, Prismatic Edge, Gem Trader, Gem Fragment, Gem Upgrader and the Gold Gem summon.

---

### 📊 Quality of Life
- The **cooldown bar above the hotbar now shows every harvested soul**. The channelled one opens out into all the abilities it can cast right now; the rest collapse to their icon and primary cooldown, so you can see what's worth switching to mid-fight.
- **`gems.droppable-on-death` is now documented in `config.yml`** with an example — the setting worked before, it just wasn't written down anywhere.

---

*Requires Resource Pack V5.2 or later for the Gold Gem's eight soul textures.*

---
name: bliss-test
description: Test a BlissGems build before it is pushed or released. Runs every automatic test (unit, every gem ability, config changes, passives, items and tooltips, regressions) in one Docker container, has the bliss-tester agent review the screenshots, starts a local server for the user to check by hand, and only then allows a push. Use before any push or release of BlissGems, when the user says "test", "run the tests", "is it ready to release", or after changing gem code.
---

# BlissGems test procedure

Follow these steps in order. Do not skip one, do not push or release before step 7.

## 0. Before you start
- Work in the BlissGems repository root. `git status` - note uncommitted changes; the tests run on
  the working tree as it is.
- Docker must be running (`docker info`). The first run downloads ~1 GB and builds the image
  (about 10 minutes); later runs reuse it.
- Do not run the tests while `testing/play.sh` is running (`docker stop blissgems-play` first).
- Never edit `testing/container/entrypoint.sh` while a run is going: bash reads it as it runs and
  the run breaks. Files in `testing/runner/` are read when the runner starts - safe to edit until then.

## 1. Run the automatic tests
```bash
testing/run-all.sh
```
Run it in the background; a full run takes about 20-30 minutes. It:
- fetches pinned dependencies (`testing/deps.lock`: Paper 1.21.11, ViaVersion, ViaBackwards,
  Fabric API, the auk-control bot mod, the resource pack) and checks their sha256;
- builds the plugin with `mvn package` - the unit tests (config wiring: every config key is used,
  every value the code reads is in config.yml, defaults match) run here, and a failure stops the run;
- starts Paper and two bot clients (`Tester`, and `.Victim` with a Bedrock-style name) in the
  container and runs the in-game suites: `items`, `abilities`, `abilities-config`,
  `abilities-tier`, `passives`, `passives-config`, `regressions`;
- writes `testing/results/` (report.html, results.json, screenshots, server and bot logs).

One suite only, while fixing something: `testing/run-all.sh --only abilities` (or
`--only abilities/life-1-life-drainer-works`). Partial runs never write `tested.json`.

## 2. Read the result
The script prints the counts and every failure. If anything failed or errored:
- read the test's message in `testing/results/report.html` / `results.json`, and the logs
  (`server.log`, `bot-Tester.log`, `container.log`);
- a failing test is a bug in the plugin until shown otherwise. Tell the user what broke, in plain
  words, with the test name. Fix it only if the user asked you to; then go back to step 1.
- an `error` (not `fail`) means the test itself or the bot broke - fix the test harness, not the plugin.
Never change a test's expectation just to make it pass. If the expected value is really wrong,
say so to the user and change it only with their OK.

A test whose `note` starts with "Known plugin bug" is failing on purpose until the plugin is fixed
(its config variant is skipped). Mention those in the summary, don't "fix" the test.
Lessons from the first runs (all were test problems, not plugin bugs): the arena is rebuilt before
every test (Meteor Shower leaves a crater); `data get` cuts long output with "...", so effects are
asked one by one; timed abilities keep running inside the plugin, so tests wait for them to end;
targets stand outside melee reach (3 blocks) or a shift+hit becomes a real hit.

## 3. Visual review by the agent
When every test passed, launch the `bliss-tester` agent (it runs on Haiku):
> Review the BlissGems test run in testing/results (goldens in testing/goldens/screens).
It returns `VERDICT: OK` or `VERDICT: PROBLEMS` with a list. Read its list yourself and look at
any picture it flags before telling the user. If it says OK:
```bash
testing/approve.sh visual
```

## 4. Tell the user the results
Short: passed / failed counts, anything the agent flagged, and the link to
`testing/results/report.html`.

## 5. Start the server for the user
```bash
testing/play.sh                  # PLAY_OP=<their name> testing/play.sh to make them op
```
It builds the same code and starts it on `127.0.0.1:25565` (Minecraft 1.21.11) with the resource
pack. If port 25565 is busy (their own test server), ask before stopping anything. Tell the user:
"Join 127.0.0.1:25565 and check it; tell me when it's OK."

## 6. Wait for the user
Do nothing more until the user says it is OK. If they report a problem, it goes back to step 1
after the fix. When they say OK:
```bash
docker stop blissgems-play
testing/approve.sh player
```

## 7. Commit and push
`testing/tested.json` now has the version, the source fingerprint and both approvals. Commit it
with the change and push (or push the `bliss/V<version>` release branch). The release workflow
recomputes the fingerprint and refuses to publish when `tested.json` is missing, is for another
version, has a pending approval, or the code changed after the tests.

## Tooltips changed on purpose?
The tooltip text test compares against `testing/goldens/tooltips.json`. For the user to look at all
tooltips at once, make a contact sheet and send it:
```bash
docker run --rm --entrypoint python3 -v "$PWD/testing:/t" blissgems-test /t/tools/contact_sheet.py /t/results /t/results/items/tooltips-sheet.png
```
Only after the user approved the new look: `testing/run-all.sh --only items --update-goldens`,
then copy `testing/results/items/` pictures into `testing/goldens/screens/` and commit both.
The gem text lives in `src/main/resources/cosmetics.yml` (the Oraxen design); the plugin adds the
live level line ("(Pristine)") on top. Gems are nautilus shells (Bedrock off hand); their models are
in `resourcepack/assets/minecraft/items/nautilus_shell.json`.

## Adding tests
- Abilities: one `ability(...)` line in `testing/runner/suite_abilities.py` (gem, slot, key, what
  to expect, which config value controls its timer).
- Anything else: a function decorated with `@test("<suite>", "<name>")` in a `suite_*.py` file,
  imported in `runner/main.py`.
- New Minecraft version: `MC_VERSION=<version> testing/deps.sh --update`.

"""Writes results.json and report.html into the output dir. Also: report.py --unit-failed <out>."""
import html
import json
import os
import sys
import time


def summarize(results):
    counts = {}
    for r in results:
        counts[r["status"]] = counts.get(r["status"], 0) + 1
    return counts


def write(out, results, meta):
    data = {"meta": meta, "summary": summarize(results), "results": results}
    json.dump(data, open(os.path.join(out, "results.json"), "w"), indent=1, ensure_ascii=False)
    rows = []
    for r in results:
        shots = "".join(f'<a href="{html.escape(s["path"])}"><img loading="lazy" src="{html.escape(s["path"])}" '
                        f'title="{html.escape(s["label"])}"></a>' for s in r["shots"][:40])
        details = html.escape("\n".join(r["details"]))
        rows.append(f'<tr class="{r["status"]}"><td>{r["status"].upper()}</td><td>{html.escape(r["suite"])}</td>'
                    f'<td>{html.escape(r["name"])}</td><td><pre>{html.escape(r["message"])}</pre>'
                    f'<details><summary>details</summary><pre>{details}</pre></details></td>'
                    f'<td class="shots">{shots}</td><td>{r["seconds"]}s</td></tr>')
    s = data["summary"]
    page = f"""<!doctype html><meta charset="utf-8"><title>BlissGems tests</title>
<style>
:root{{--bg:#fff;--fg:#111;--mut:#666;--pass:#e6f6ea;--fail:#fde7e7;--skip:#f2f2f2;--err:#fff0d6}}
@media (prefers-color-scheme:dark){{:root{{--bg:#16161a;--fg:#eee;--mut:#999;--pass:#16301d;--fail:#3a1717;--skip:#26262b;--err:#3a2c12}}}}
body{{font:14px system-ui;background:var(--bg);color:var(--fg);margin:16px}}
table{{border-collapse:collapse;width:100%}} td{{border-top:1px solid #8884;padding:6px;vertical-align:top}}
tr.pass{{background:var(--pass)}} tr.fail{{background:var(--fail)}} tr.skip{{background:var(--skip)}} tr.error{{background:var(--err)}}
pre{{white-space:pre-wrap;margin:0}} .shots img{{height:90px;margin:2px;border:1px solid #8886}}
</style>
<h1>BlissGems tests</h1>
<p>{html.escape(meta.get("version", "?"))} · Paper {html.escape(meta.get("server", "?"))} · {html.escape(meta.get("time", ""))}</p>
<p><b>{s.get("pass", 0)} passed</b>, {s.get("fail", 0)} failed, {s.get("error", 0)} errors, {s.get("skip", 0)} skipped</p>
<table>{"".join(rows)}</table>"""
    open(os.path.join(out, "report.html"), "w").write(page)
    return data


if __name__ == "__main__" and sys.argv[1] == "--unit-failed":
    out = sys.argv[2]
    tail = open(os.path.join(out, "maven.log")).read()[-6000:] if os.path.exists(os.path.join(out, "maven.log")) else ""
    write(out, [{"suite": "unit", "name": "mvn package (unit tests)", "status": "fail", "message": tail,
                 "details": [], "shots": [], "seconds": 0}], {"time": time.strftime("%F %T")})

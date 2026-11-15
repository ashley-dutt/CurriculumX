"""Add a verified benchmark source to data/benchmarks.json.
Only add material you have actually read from the official course page, and keep its URL and licence.
  python tools/ingest_source.py --uni MIT --code 6.036 --course "Introduction to Machine Learning (Fall 2020)" \\
      --url https://ocw.mit.edu/... --license "CC BY-NC-SA 4.0" --file topics.txt
topics.txt: one topic per line, optionally "Unit title | topic". Topics are stored verbatim."""
import argparse, json, os, re, datetime
p = argparse.ArgumentParser()
for k in ("uni", "code", "course", "url", "license", "file"): p.add_argument("--" + k, required=True)
a = p.parse_args()
if not a.url.startswith("https://"): raise SystemExit("URL must start with https://")
path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "data", "benchmarks.json")
data = json.load(open(path, encoding="utf-8")); T = data["topics"]
n = 0; seen = {t["id"] for t in T}
for line in open(a.file, encoding="utf-8"):
    line = line.strip()
    if not line: continue
    unit, text = (line.split("|", 1) + [""])[:2] if "|" in line else ("", line)
    unit, text = unit.strip() or "General", (text or line).strip(); n += 1
    i = f"{a.uni}-{a.code}-{n:03d}".replace(" ", "")
    if i in seen: raise SystemExit("Duplicate id " + i + " - use a different --code")
    T.append({"id": i, "uni": a.uni, "code": a.code, "course": a.course, "unit": unit, "text": text,
              "label": f"{a.uni} {a.code} \u00b7 Item {n}", "url": a.url, "license": a.license, "retrieved": str(datetime.date.today())})
json.dump(data, open(path, "w", encoding="utf-8"), indent=1, ensure_ascii=False); print("Added", n, "topics")

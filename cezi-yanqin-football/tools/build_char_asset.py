#!/usr/bin/env python3
import json
import pathlib
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "char_meta.tsv"

OFFICIAL_NAME = "通用规范汉字表(2013)全部(8105字).txt"
official_url = "https://raw.githubusercontent.com/lqfeng/ChineseCharacters/master/" + urllib.parse.quote(OFFICIAL_NAME)
meta_url = "https://raw.githubusercontent.com/mapull/chinese-dictionary/main/character/char_base.json"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "cezi-football-build/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


official_text = fetch(official_url).decode("utf-8")
meta_data = json.loads(fetch(meta_url).decode("utf-8"))

meta_by_char = {}
for obj in meta_data:
    ch = obj.get("char")
    if ch and ch not in meta_by_char:
        meta_by_char[ch] = obj

selected = []
for line in official_text.splitlines():
    parts = line.split("\t")
    if len(parts) < 2 or not parts[0].isdigit():
        continue
    idx = int(parts[0])
    if idx > 7000:
        break
    selected.append((idx, parts[1]))

if len(selected) != 7000:
    raise RuntimeError(f"official pool expected 7000 chars, got {len(selected)}")

rows = []
missing = []
for idx, ch in selected:
    obj = meta_by_char.get(ch)
    if not obj:
        missing.append(ch)
        continue
    strokes = int(obj.get("strokes") or 0)
    radical = str(obj.get("radicals") or "—").replace("\t", " ").replace("\n", " ")
    structure = str(obj.get("structure") or "").replace("\t", " ").replace("\n", " ")
    frequency = int(obj.get("frequency") if obj.get("frequency") is not None else 4)
    if strokes <= 0:
        missing.append(ch)
        continue
    rows.append((idx, ch, strokes, radical, structure, frequency))

if missing:
    raise RuntimeError(f"metadata missing for {len(missing)} chars, sample={''.join(missing[:20])}")
if len(rows) != 7000:
    raise RuntimeError(f"metadata pool expected 7000 rows, got {len(rows)}")

OUT.parent.mkdir(parents=True, exist_ok=True)
with OUT.open("w", encoding="utf-8", newline="\n") as f:
    f.write("#index\tchar\tstrokes\tradical\tstructure\tfrequency\n")
    for row in rows:
        f.write("\t".join(map(str, row)) + "\n")

print(f"generated {OUT}: {len(rows)} characters")

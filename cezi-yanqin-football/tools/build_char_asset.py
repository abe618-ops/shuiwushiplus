#!/usr/bin/env python3
import json
import pathlib
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "app" / "src" / "main" / "assets" / "char_meta.tsv"
TARGET = 7000
MAX_STROKES = 20

OFFICIAL_NAME = "通用规范汉字表(2013)全部(8105字).txt"
official_url = "https://raw.githubusercontent.com/lqfeng/ChineseCharacters/master/" + urllib.parse.quote(OFFICIAL_NAME)
meta_url = "https://raw.githubusercontent.com/mapull/chinese-dictionary/main/character/char_base.json"


def fetch(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "cezi-football-build/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return resp.read()


def parse_json_stream(text: str):
    """Accept a JSON array or a stream/JSONL of adjacent JSON objects."""
    text = text.lstrip("\ufeff")
    try:
        data = json.loads(text)
        return data if isinstance(data, list) else [data]
    except json.JSONDecodeError:
        dec = json.JSONDecoder()
        out = []
        pos = 0
        n = len(text)
        while pos < n:
            while pos < n and (text[pos].isspace() or text[pos] in ",[]"):
                pos += 1
            if pos >= n:
                break
            obj, pos = dec.raw_decode(text, pos)
            out.append(obj)
        return out


official_text = fetch(official_url).decode("utf-8")
meta_text = fetch(meta_url).decode("utf-8")
meta_data = parse_json_stream(meta_text)

meta_by_char = {}
for obj in meta_data:
    if not isinstance(obj, dict):
        continue
    ch = obj.get("char")
    if ch and ch not in meta_by_char:
        meta_by_char[ch] = obj

rows = []
skipped_missing = []
skipped_complex = []
skipped_supplementary = []
for line in official_text.splitlines():
    parts = line.split("\t")
    if len(parts) < 2 or not parts[0].isdigit():
        continue
    idx = int(parts[0])
    ch = parts[1]
    if len(rows) >= TARGET:
        break

    # Keep the random pool visually usable on ordinary Android fonts.
    if len(ch) != 1 or ord(ch) > 0xFFFF:
        skipped_supplementary.append(ch)
        continue

    obj = meta_by_char.get(ch)
    if not obj:
        skipped_missing.append(ch)
        continue
    strokes = int(obj.get("strokes") or 0)
    if strokes <= 0:
        skipped_missing.append(ch)
        continue
    if strokes > MAX_STROKES:
        skipped_complex.append(ch)
        continue

    radical = str(obj.get("radicals") or "—").replace("\t", " ").replace("\n", " ")
    structure = str(obj.get("structure") or "").replace("\t", " ").replace("\n", " ")
    frequency = int(obj.get("frequency") if obj.get("frequency") is not None else 4)
    rows.append((idx, ch, strokes, radical, structure, frequency))

if len(rows) != TARGET:
    raise RuntimeError(
        f"eligible pool expected {TARGET}, got {len(rows)}; "
        f"missing={len(skipped_missing)}, complex={len(skipped_complex)}, supplementary={len(skipped_supplementary)}"
    )

OUT.parent.mkdir(parents=True, exist_ok=True)
with OUT.open("w", encoding="utf-8", newline="\n") as f:
    f.write("#index\tchar\tstrokes\tradical\tstructure\tfrequency\n")
    for row in rows:
        f.write("\t".join(map(str, row)) + "\n")

print(
    f"generated {OUT}: {len(rows)} characters; "
    f"skipped missing={len(skipped_missing)}, complex>{MAX_STROKES}={len(skipped_complex)}, "
    f"non-BMP={len(skipped_supplementary)}; last official index={rows[-1][0]}"
)

#!/usr/bin/env python3
import json, os, time, urllib.parse, urllib.request
from datetime import datetime, timezone

TOKEN = os.environ.get("GITHUB_TOKEN", "")
API = "https://api.github.com"
OUT = "data/openrepo-android-index.json"

# 中文概念 -> GitHub 英文发现词。中文标签会写入索引，供手机本地语义搜索。
QUERY_GROUPS = [
    ("视频下载", ["video downloader android", "media downloader android", "yt-dlp android"]),
    ("下载器", ["download manager android"]),
    ("剪贴板同步", ["clipboard sync android"]),
    ("RSS阅读器", ["rss reader android", "feed reader android"]),
    ("浏览器", ["open source browser android"]),
    ("广告拦截", ["ad blocker android"]),
    ("密码管理", ["password manager android"]),
    ("笔记", ["notes markdown android"]),
    ("音乐播放器", ["music player android"]),
    ("文件管理", ["file manager android"]),
    ("终端SSH", ["terminal ssh android"]),
    ("翻译", ["translator android"]),
    ("OCR扫描", ["ocr scanner android"]),
    ("PDF阅读", ["pdf reader android"]),
    ("相机", ["camera app android"]),
]


def request_json(path):
    req = urllib.request.Request(API + path)
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    req.add_header("User-Agent", "OpenRepo-Indexer/1.0")
    if TOKEN:
        req.add_header("Authorization", f"Bearer {TOKEN}")
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def search_repos(term, per_page=15):
    q = urllib.parse.quote(f"{term} archived:false")
    return request_json(f"/search/repositories?q={q}&sort=stars&order=desc&per_page={per_page}").get("items", [])


def find_apk_release(full_name):
    try:
        releases = request_json(f"/repos/{full_name}/releases?per_page=6")
    except Exception:
        return None
    prerelease = None
    for rel in releases:
        if rel.get("draft"):
            continue
        apks = []
        for a in rel.get("assets", []):
            n = (a.get("name") or "")
            low = n.lower()
            if not low.endswith(".apk"):
                continue
            if any(x in low for x in ["unsigned", "mapping", "source"]):
                continue
            apks.append(a)
        if not apks:
            continue
        apks.sort(key=lambda a: ("debug" in (a.get("name") or "").lower(), "test" in (a.get("name") or "").lower(), -int(a.get("size") or 0)))
        a = apks[0]
        item = {
            "version": rel.get("tag_name") or rel.get("name") or "",
            "apkName": a.get("name") or "app.apk",
            "apkSize": int(a.get("size") or 0),
            "downloadUrl": a.get("browser_download_url") or "",
            "releaseUrl": rel.get("html_url") or "",
            "publishedAt": rel.get("published_at") or "",
            "prerelease": bool(rel.get("prerelease")),
        }
        if not item["prerelease"]:
            return item
        prerelease = prerelease or item
    return prerelease


def main():
    discovered = {}
    for zh, terms in QUERY_GROUPS:
        for term in terms:
            try:
                rows = search_repos(term)
            except Exception as e:
                print("search failed", term, e)
                continue
            for r in rows:
                full = r.get("full_name")
                if not full:
                    continue
                d = discovered.setdefault(full, {"repo": r, "zh": set(), "terms": set()})
                d["zh"].add(zh)
                d["terms"].add(term)
            time.sleep(0.35)

    # Android 信号和 stars 共同排序，控制 API 消耗。
    candidates = list(discovered.values())
    def rank(d):
        r = d["repo"]
        text = " ".join([r.get("name") or "", r.get("description") or "", " ".join(r.get("topics") or [])]).lower()
        sig = sum(k in text for k in ["android", "apk", "kotlin", "compose", "mobile"])
        return sig * 100000 + int(r.get("stargazers_count") or 0)
    candidates.sort(key=rank, reverse=True)
    candidates = candidates[:180]

    apps = []
    for i, d in enumerate(candidates, 1):
        r = d["repo"]
        full = r.get("full_name")
        rel = find_apk_release(full)
        if not rel or not rel.get("downloadUrl"):
            continue
        topics = r.get("topics") or []
        zh_tags = sorted(d["zh"])
        discovery_terms = sorted(d["terms"])
        search_text = " ".join([
            r.get("name") or "", full or "", r.get("description") or "",
            " ".join(topics), " ".join(zh_tags), " ".join(discovery_terms)
        ]).lower()
        apps.append({
            "name": r.get("name") or full,
            "repo": full,
            "description": r.get("description") or "",
            "stars": int(r.get("stargazers_count") or 0),
            "topics": topics,
            "updatedAt": r.get("updated_at") or "",
            "semanticTags": zh_tags,
            "discoveryTerms": discovery_terms,
            "searchText": search_text,
            **rel,
        })
        print(f"[{i}/{len(candidates)}] + {full} -> {rel['apkName']}")
        time.sleep(0.12)

    # 去重并按 stars + 最近更新时间排序。
    uniq = {a["repo"]: a for a in apps}
    apps = list(uniq.values())
    apps.sort(key=lambda a: (a["stars"], a["updatedAt"]), reverse=True)
    payload = {
        "schema": 1,
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "count": len(apps),
        "source": "GitHub public repositories/releases",
        "apps": apps,
    }
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
    print("wrote", OUT, "apps=", len(apps))

if __name__ == "__main__":
    main()

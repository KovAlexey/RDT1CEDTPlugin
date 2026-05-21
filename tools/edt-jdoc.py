#!/usr/bin/env python3
import argparse
import hashlib
import json
import re
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from urllib.parse import urlparse


DEFAULT_MAP = ".ai/edt-javadoc-map.jsonl"
DEFAULT_CACHE = ".ai/edt-javadoc-pages"


def load_records(path: Path) -> list[dict]:
    if not path.exists():
        raise FileNotFoundError(f"Map not found: {path}")

    records = []

    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line:
                continue

            records.append(json.loads(line))

    return records


def page_url_without_fragment(url: str) -> str:
    """
    JavaDoc anchors point to methods/fields inside the same class page.

    Example:
      SomeClass.html#getName()

    Physically we only need:
      SomeClass.html

    So cache by page URL, not by fragment URL.
    """
    parsed = urlparse(url)
    return parsed._replace(fragment="").geturl()


def url_fragment(url: str) -> str:
    return urlparse(url).fragment or ""


def safe_filename_for_url(url: str) -> str:
    page_url = page_url_without_fragment(url)
    parsed = urlparse(page_url)

    path = parsed.path.strip("/")

    if not path:
        path = "index.html"

    path = path.replace("/", "__")

    digest = hashlib.sha1(page_url.encode("utf-8")).hexdigest()[:10]

    if not path.endswith(".html"):
        path += ".html"

    return f"{path}__{digest}"


def fetch_url(url: str, cache_dir: Path, force: bool = False) -> Path:
    cache_dir.mkdir(parents=True, exist_ok=True)

    page_url = page_url_without_fragment(url)
    out = cache_dir / safe_filename_for_url(page_url)

    if out.exists() and not force:
        return out

    req = urllib.request.Request(
        page_url,
        headers={
            "User-Agent": "edt-jdoc-fetch/1.0",
        },
    )

    with urllib.request.urlopen(req, timeout=60) as r:
        data = r.read()

    out.write_bytes(data)

    # Небольшая пауза, чтобы не долбить сайт пачкой запросов.
    time.sleep(0.15)

    return out


def score(record: dict, query: str) -> int:
    q = query.lower().strip()

    name = str(record.get("name", "")).lower()
    fqn = str(record.get("fqn", "")).lower()
    owner = str(record.get("owner", "")).lower()
    package = str(record.get("package", "")).lower()
    kind = str(record.get("kind", "")).lower()
    url = str(record.get("url", "")).lower()

    haystack = " ".join([name, fqn, owner, package, kind, url])

    if q == fqn:
        return 1000

    if q == name:
        return 900

    if fqn.endswith("." + q):
        return 850

    if owner.endswith("." + q):
        return 750

    if q in fqn:
        return 550

    if q in name:
        return 500

    if q in owner:
        return 400

    if q in package:
        return 250

    if q in haystack:
        return 100

    # Поддержка запросов из нескольких слов:
    #   IModelObject getName
    #   md object common module
    parts = [p for p in re.split(r"\s+", q) if p]

    if parts and all(p in haystack for p in parts):
        return 80

    return 0


def kind_sort_weight(kind: str) -> int:
    """
    Для равного score показываем типы выше members.
    """
    order = {
        "type": 0,
        "member": 1,
        "package": 2,
        "module": 3,
        "tag": 4,
    }

    return order.get(kind, 99)


def find_matches(records: list[dict], query: str, limit: int) -> list[dict]:
    matches = []

    for r in records:
        s = score(r, query)

        if s > 0:
            matches.append((s, r))

    matches.sort(
        key=lambda x: (
            -x[0],
            kind_sort_weight(x[1].get("kind", "")),
            x[1].get("fqn", ""),
        )
    )

    return [r for _, r in matches[:limit]]


def dedupe_by_page_url(records: list[dict]) -> list[dict]:
    """
    Если 10 member-записей ведут на один SomeClass.html#anchor,
    скачиваем страницу один раз.
    """
    result = []
    seen = set()

    for r in records:
        url = r.get("url") or ""

        if not url:
            continue

        page_url = page_url_without_fragment(url)

        if page_url in seen:
            continue

        seen.add(page_url)
        result.append(r)

    return result


def print_record(i: int, r: dict, cached_path: Path | None = None) -> None:
    url = r.get("url") or ""
    fragment = url_fragment(url)

    print(f"{i}. [{r.get('kind')}] {r.get('fqn')}")

    if r.get("owner"):
        print(f"   owner: {r.get('owner')}")

    if r.get("package"):
        print(f"   package: {r.get('package')}")

    print(f"   url: {url}")

    if fragment:
        print(f"   anchor: #{fragment}")

    if cached_path:
        print(f"   cached: {cached_path}")

    print(f"   source: {r.get('source')}")
    print()


def cache_records(records: list[dict], cache_dir: Path, force: bool = False) -> dict[str, Path]:
    """
    Cache pages for visible records.

    Returns:
      page_url_without_fragment -> cached file path
    """
    cached = {}

    for r in dedupe_by_page_url(records):
        url = r.get("url") or ""

        if not url:
            continue

        page_url = page_url_without_fragment(url)

        try:
            cached[page_url] = fetch_url(url, cache_dir, force=force)
        except urllib.error.HTTPError as e:
            print(f"WARN: HTTP {e.code} while fetching {page_url}", file=sys.stderr)
        except urllib.error.URLError as e:
            print(f"WARN: URL error while fetching {page_url}: {e}", file=sys.stderr)
        except TimeoutError:
            print(f"WARN: timeout while fetching {page_url}", file=sys.stderr)
        except Exception as e:
            print(f"WARN: failed to fetch {page_url}: {e}", file=sys.stderr)

    return cached


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Search EDT JavaDoc map and cache matching JavaDoc pages."
    )

    parser.add_argument(
        "query",
        help="Class, method, package or API name to search",
    )

    parser.add_argument(
        "--map",
        default=DEFAULT_MAP,
        help=f"Path to JavaDoc map JSONL. Default: {DEFAULT_MAP}",
    )

    parser.add_argument(
        "--limit",
        type=int,
        default=10,
        help="How many matching records to print and cache. Default: 10",
    )

    parser.add_argument(
        "--cache-dir",
        default=DEFAULT_CACHE,
        help=f"Directory for cached JavaDoc pages. Default: {DEFAULT_CACHE}",
    )

    parser.add_argument(
        "--force",
        action="store_true",
        help="Re-download cached pages even if they already exist",
    )

    parser.add_argument(
        "--no-cache",
        action="store_true",
        help="Search only; do not cache pages. Intended only for diagnostics.",
    )

    args = parser.parse_args()

    try:
        records = load_records(Path(args.map))
    except Exception as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    matches = find_matches(records, args.query, args.limit)

    if not matches:
        print(f"No JavaDoc matches for: {args.query}")
        return 1

    cached_by_page_url: dict[str, Path] = {}

    if not args.no_cache:
        cached_by_page_url = cache_records(
            matches,
            Path(args.cache_dir),
            force=args.force,
        )

    for i, r in enumerate(matches, start=1):
        url = r.get("url") or ""
        page_url = page_url_without_fragment(url)
        cached_path = cached_by_page_url.get(page_url)
        print_record(i, r, cached_path)

    if not args.no_cache:
        unique_pages = len(dedupe_by_page_url(matches))
        cached_count = len(cached_by_page_url)

        print(f"Cached pages: {cached_count}/{unique_pages}")
        print(f"Cache dir: {args.cache_dir}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

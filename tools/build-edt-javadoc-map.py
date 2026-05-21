#!/usr/bin/env python3
import argparse
import json
import re
import sys
from pathlib import Path
from urllib.parse import urljoin


SEARCH_FILES = [
    ("package", "package-search-index.js"),
    ("type", "type-search-index.js"),
    ("member", "member-search-index.js"),
    ("module", "module-search-index.js"),
    ("tag", "tag-search-index.js"),
]


def extract_first_js_array(text: str) -> str:
    """
    Extract first top-level JS array after '='.

    Handles formats like:
      typeSearchIndex = [...];updateSearchResults();
      var typeSearchIndex = [...];
      window.typeSearchIndex = [...];

    Does not require the array to be at EOF.
    """
    eq_pos = text.find("=")

    if eq_pos == -1:
        raise ValueError("Cannot find '=' before JS array")

    start = text.find("[", eq_pos)

    if start == -1:
        raise ValueError("Cannot find '[' after '='")

    depth = 0
    in_string = False
    string_quote = ""
    escape = False

    for i in range(start, len(text)):
        ch = text[i]

        if in_string:
            if escape:
                escape = False
                continue

            if ch == "\\":
                escape = True
                continue

            if ch == string_quote:
                in_string = False
                string_quote = ""

            continue

        if ch in ("'", '"'):
            in_string = True
            string_quote = ch
            continue

        if ch == "[":
            depth += 1
            continue

        if ch == "]":
            depth -= 1

            if depth == 0:
                return text[start:i + 1]

    raise ValueError("Cannot find matching ']' for JS array")


def read_js_array(path: Path) -> list[dict]:
    text = path.read_text(encoding="utf-8", errors="replace")
    array_text = extract_first_js_array(text)

    try:
        return json.loads(array_text)
    except json.JSONDecodeError as e:
        debug_path = path.with_suffix(path.suffix + ".array-debug.txt")
        debug_path.write_text(array_text[:20000], encoding="utf-8", errors="replace")
        raise ValueError(
            f"JSON parse failed in {path}: {e}. "
            f"First 20000 chars of extracted array saved to {debug_path}"
        )


def as_url(base_url: str, rel: str) -> str:
    if not rel:
        return ""

    return urljoin(base_url.rstrip("/") + "/", rel)


def package_to_path(package: str) -> str:
    return package.replace(".", "/")


def clean_fragment(fragment: str) -> str:
    if not fragment:
        return ""

    return fragment.strip()


def get_url_part(item: dict) -> str:
    return item.get("url") or item.get("u") or ""


def member_rel_url(package: str, clazz: str, url_part: str) -> str:
    """
    JavaDoc member search records often contain only anchor id in `u`,
    for example:
      {"p":"x.y","c":"SomeClass","l":"getName()","u":"getName()"}
    In that case real URL is:
      x/y/SomeClass.html#getName()

    But sometimes `u` can already be a page path.
    """
    if not url_part:
        if package and clazz:
            return f"{package_to_path(package)}/{clazz}.html"
        return ""

    if ".html" in url_part:
        return url_part

    if package and clazz:
        return f"{package_to_path(package)}/{clazz}.html#{clean_fragment(url_part)}"

    return url_part


def normalize_record(kind: str, item: dict, base_url: str, source_file: str) -> dict:
    package = item.get("p") or ""
    label = item.get("l") or ""
    clazz = item.get("c") or ""
    module = item.get("m") or ""
    url_part = get_url_part(item)

    record = {
        "kind": kind,
        "name": label,
        "fqn": label,
        "package": package,
        "owner": "",
        "class": clazz,
        "module": module,
        "url": "",
        "source": source_file,
    }

    if kind == "package":
        fqn = label
        rel = url_part or f"{package_to_path(label)}/package-summary.html"

        record.update({
            "fqn": fqn,
            "url": as_url(base_url, rel),
        })
        return record

    if kind == "type":
        fqn = f"{package}.{label}" if package else label

        if url_part:
            rel = url_part
        elif package:
            rel = f"{package_to_path(package)}/{label}.html"
        else:
            rel = f"{label}.html"

        record.update({
            "fqn": fqn,
            "url": as_url(base_url, rel),
        })
        return record

    if kind == "member":
        if package and clazz:
            owner = f"{package}.{clazz}"
        elif clazz:
            owner = clazz
        else:
            owner = ""

        fqn = f"{owner}.{label}" if owner else label
        rel = member_rel_url(package, clazz, url_part)

        record.update({
            "owner": owner,
            "fqn": fqn,
            "url": as_url(base_url, rel),
        })
        return record

    if kind == "module":
        fqn = label
        rel = url_part or f"{label}/module-summary.html"

        record.update({
            "fqn": fqn,
            "url": as_url(base_url, rel),
        })
        return record

    if kind == "tag":
        record.update({
            "fqn": label,
            "url": as_url(base_url, url_part),
        })
        return record

    return record


def dedupe(records: list[dict]) -> list[dict]:
    seen = set()
    result = []

    for r in records:
        key = (
            r.get("kind", ""),
            r.get("fqn", ""),
            r.get("url", ""),
        )

        if key in seen:
            continue

        seen.add(key)
        result.append(r)

    return result


def write_summary(records: list[dict], out_summary: Path) -> None:
    counts = {}

    for r in records:
        counts[r["kind"]] = counts.get(r["kind"], 0) + 1

    lines = [
        "# EDT JavaDoc map summary",
        "",
        "Generated from local JavaDoc search index files.",
        "",
        "## Counts",
        "",
    ]

    for kind in sorted(counts):
        lines.append(f"- `{kind}`: {counts[kind]}")

    lines.extend([
        "",
        "## Files",
        "",
        "- Map: `.ai/edt-javadoc-map.jsonl`",
        "- Search helper: `tools/edt-jdoc.py`",
        "",
        "## Usage",
        "",
        "```bash",
        "python3 tools/edt-jdoc.py IModelObject",
        "python3 tools/edt-jdoc.py MdObject",
        "python3 tools/edt-jdoc.py getName",
        "python3 tools/edt-jdoc.py CommonModule",
        "```",
    ])

    out_summary.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--index-dir",
        default="javadoc/javadoc-index",
        help="Directory containing downloaded JavaDoc search index files",
    )
    parser.add_argument(
        "--base-url",
        default="https://edt.1c.ru/dev/edt/2025.1/apidocs/",
        help="Base URL of EDT JavaDoc",
    )
    parser.add_argument(
        "--out",
        default=".ai/edt-javadoc-map.jsonl",
        help="Output JSONL map",
    )
    parser.add_argument(
        "--summary",
        default=".ai/edt-javadoc-map-summary.md",
        help="Output summary markdown",
    )

    args = parser.parse_args()

    index_dir = Path(args.index_dir)
    out_path = Path(args.out)
    summary_path = Path(args.summary)

    if not index_dir.exists():
        print(f"ERROR: index dir not found: {index_dir}", file=sys.stderr)
        return 1

    records = []

    for kind, filename in SEARCH_FILES:
        path = index_dir / filename

        if not path.exists():
            print(f"WARN: missing {path}", file=sys.stderr)
            continue

        print(f"Reading {path}...", file=sys.stderr)

        try:
            items = read_js_array(path)
        except Exception as e:
            print(f"ERROR: failed to parse {path}: {e}", file=sys.stderr)
            return 1

        print(f"  {len(items)} items", file=sys.stderr)

        for item in items:
            records.append(normalize_record(kind, item, args.base_url, filename))

    records = dedupe(records)

    out_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.parent.mkdir(parents=True, exist_ok=True)

    with out_path.open("w", encoding="utf-8") as f:
        for r in records:
            f.write(json.dumps(r, ensure_ascii=False, sort_keys=True) + "\n")

    write_summary(records, summary_path)

    print(f"Wrote {len(records)} records to {out_path}")
    print(f"Wrote summary to {summary_path}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

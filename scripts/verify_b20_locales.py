#!/usr/bin/env python3
"""B20: ensure new sharing labels exist in each supported Android locale."""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
LOCALES = (
    "values", "values-es", "values-de", "values-fr", "values-he",
    "values-hi", "values-id", "values-it", "values-ja", "values-ko",
    "values-pl", "values-pt", "values-ru", "values-th", "values-tr",
    "values-vi", "values-zh", "values-zh-rHK", "values-zh-rTW",
    "values-b+zh+Hant",
)
REQUIRED = ("lyrics_share_video", "lyrics_share_image")
PATTERN = re.compile(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', re.S)

def main() -> int:
    problems = []
    for locale in LOCALES:
        path = ROOT / locale / "strings.xml"
        if not path.exists():
            problems.append(f"{locale}: missing strings.xml")
            continue
        matches = list(PATTERN.finditer(path.read_text(encoding="utf-8")))
        for name in REQUIRED:
            found = [m.group(2).strip() for m in matches if m.group(1) == name]
            if len(found) != 1 or not found[0]:
                problems.append(f"{locale}: expected one nonempty {name}")
    if problems:
        print("\n".join(problems), file=sys.stderr)
        return 1
    print(f"B20: localized video/image sharing in {len(LOCALES)} Android variants")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())

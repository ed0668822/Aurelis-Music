#!/usr/bin/env python3
"""Syntax-check the inline JavaScript that ships inside the Android TV browser asset."""
from html.parser import HTMLParser
from pathlib import Path
import subprocess
import sys
import tempfile

class Scripts(HTMLParser):
    def __init__(self):
        super().__init__()
        self.inside = False
        self.parts = []
    def handle_starttag(self, tag, attrs):
        if tag.lower() == "script":
            self.inside = True
            self.parts.append("")
    def handle_endtag(self, tag):
        if tag.lower() == "script":
            self.inside = False
    def handle_data(self, data):
        if self.inside:
            self.parts[-1] += data

source = Path("app/src/main/assets/webtv/index.html")
parser = Scripts()
parser.feed(source.read_text(encoding="utf-8"))
if not parser.parts or not any(part.strip() for part in parser.parts):
    sys.exit("No inline browser receiver JavaScript found")
with tempfile.TemporaryDirectory() as folder:
    for index, script in enumerate(parser.parts):
        if not script.strip():
            continue
        path = Path(folder) / f"webtv_{index}.js"
        path.write_text(script, encoding="utf-8")
        subprocess.run(["node", "--check", str(path)], check=True)
print("Web TV receiver JavaScript syntax OK")

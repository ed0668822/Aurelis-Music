# Aurelis © 2026. GPL-3.0-or-later
"""Original deterministic test audio/art/LRC; never copies user media."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys


def generate(out):
    from PIL import Image, ImageDraw
    out.mkdir(parents=True, exist_ok=True)
    for index in range(1, 4):
        name = f"Parity Track {index:02}"
        cover = out / f"cover-{index}.png"
        image = Image.new("RGB", (600, 600), [(36, 74, 142), (135, 40, 70), (36, 115, 75)][index - 1])
        draw = ImageDraw.Draw(image)
        draw.rectangle((60, 60, 540, 540), outline="white", width=12)
        draw.ellipse((180, 180, 420, 420), fill=(240, 205, 80))
        draw.text((80, 80), name, fill="white", font_size=38)
        image.save(cover)
        lyrics = f"[ti:{name}]\n[ar:Parity Artist]\n[00:00.00]Parity line one\n[00:10.00]Parity line two\n[00:30.00]Parity line three\n"
        audio = out / f"{name}.flac"
        subprocess.run([
            "ffmpeg", "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "lavfi", "-i", f"sine=frequency={220 * index}:duration=60",
            "-i", str(cover), "-map", "0:a", "-map", "1:v", "-c:a", "flac", "-c:v", "png", "-disposition:v", "attached_pic",
            "-metadata", f"title={name}", "-metadata", f"LYRICS={lyrics}",
            "-metadata", "artist=Parity Artist", "-metadata", f"album=Parity Album {index}",
            "-metadata:s:v", "title=Album cover", "-metadata:s:v", "comment=Cover (front)", str(audio),
        ], check=True, timeout=90)
        (out / f"{name}.lrc").write_text(lyrics, encoding="utf-8")
    files = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.iterdir()) if p.is_file() and p.name != "manifest.json"}
    (out / "manifest.json").write_text(json.dumps({"original_fixture": True, "files": files}, indent=2) + "\n")


if __name__ == "__main__":
    generate(Path(sys.argv[1]))

# Aurelis © 2026. GPL-3.0-or-later
"""Bounded host/ADB telemetry during capture, without process arguments or cores."""
from collections import deque
import json
from pathlib import Path
import subprocess
import threading
import time


def command(args):
    try:
        run = subprocess.run(args, capture_output=True, text=True, timeout=3)
        return {"exit_code": run.returncode, "stdout": run.stdout[-32768:], "stderr": run.stderr[-1000:]}
    except (OSError, subprocess.TimeoutExpired) as error:
        return {"error": type(error).__name__}


class HostMonitor:
    """Telemetry failures never replace the capture verdict or fabricate a cause."""
    def __init__(self, folder):
        self.folder = Path(folder)
        self.samples = deque(maxlen=36)
        self.stopped = threading.Event()
        self.thread = None
        self.error = None

    def sample(self):
        sample = {"unix_time": time.time(),
                  "memory": command(["free", "-m"]),
                  "processes": command(["ps", "-eo", "pid,ppid,stat,rss,pcpu,comm"]),
                  "adb": command(["adb", "get-state"])}
        sample["cgroup"] = {}
        for name in ("memory.events", "memory.current", "memory.max"):
            try:
                sample["cgroup"][name] = (Path("/sys/fs/cgroup") / name).read_text()[:2048]
            except OSError:
                sample["cgroup"][name] = "unavailable"
        self.samples.append(sample)
        self.write()

    def write(self):
        self.folder.mkdir(parents=True, exist_ok=True)
        target = self.folder / "during-capture.json"
        temporary = target.with_suffix(".tmp")
        temporary.write_text(json.dumps({"samples": list(self.samples), "error": self.error,
                                         "cause_verdict": "UNCONFIRMED"}, indent=2) + "\n")
        temporary.replace(target)

    def kernel(self, stage):
        (self.folder / (stage + "-kernel.json")).write_text(json.dumps(
            command(["sudo", "-n", "dmesg", "--ctime"]), indent=2) + "\n")

    def loop(self):
        try:
            self.folder.mkdir(parents=True, exist_ok=True)
            self.kernel("before")
            while not self.stopped.is_set():
                self.sample()
                if self.stopped.wait(5):
                    break
            self.kernel("after")
            self.write()
        except Exception as error:
            self.error = type(error).__name__
            try:
                self.write()
            except OSError:
                pass

    def start(self):
        self.thread = threading.Thread(target=self.loop, daemon=True)
        self.thread.start()

    def stop(self):
        self.stopped.set()
        if self.thread is not None:
            self.thread.join(timeout=15)
        return {"stopped": self.thread is None or not self.thread.is_alive(), "error": self.error}

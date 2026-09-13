#!/usr/bin/env python3
"""Load this project's local environment as data and run the Java 21 backend."""
import os
import subprocess
import sys
from pathlib import Path

root = Path(__file__).resolve().parent.parent
env = os.environ.copy()
keys = {
    "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD", "REDIS_PASSWORD",
    "DB_PORT", "REDIS_PORT", "BACKEND_PORT",
    "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "ALLOWED_DOMAINS", "SESSION_COOKIE_SECURE",
}
for line in (root / ".env").read_text().splitlines():
    key, separator, value = line.partition("=")
    if separator and key.strip() in keys:
        env.setdefault(key.strip(), value.strip().strip("\"'"))
env.setdefault("SERVER_PORT", env.get("BACKEND_PORT", "18080"))
if sys.platform == "darwin":
    env["JAVA_HOME"] = subprocess.check_output(["/usr/libexec/java_home", "-v", "21"], text=True).strip()
raise SystemExit(subprocess.call([str(root / "backend/mvnw"), "spring-boot:run"], cwd=root / "backend", env=env))

#!/usr/bin/env python3
"""Initialize local credentials without replacing existing environment values."""
from pathlib import Path
import secrets

root = Path(__file__).resolve().parent.parent
path = root / ".env"
text = path.read_text() if path.exists() else ""
present = {line.split("=", 1)[0].strip() for line in text.splitlines() if "=" in line and not line.lstrip().startswith("#")}
defaults = {
    "POSTGRES_DB": "elib", "POSTGRES_USER": "elib",
    "POSTGRES_PASSWORD": secrets.token_urlsafe(32),
    "REDIS_PASSWORD": secrets.token_urlsafe(32),
    "DB_PORT": "55432", "REDIS_PORT": "56379", "BACKEND_PORT": "18080", "FRONTEND_PORT": "5173",
    "GOOGLE_CLIENT_ID": "", "GOOGLE_CLIENT_SECRET": "",
    "ALLOWED_DOMAINS": "", "SESSION_COOKIE_SECURE": "false",
}
missing = {key: value for key, value in defaults.items() if key not in present}
if missing:
    with path.open("a") as output:
        if text and not text.endswith("\n"):
            output.write("\n")
        output.write("\n# E-LIB local environment\n")
        for key, value in missing.items():
            output.write(f"{key}={value}\n")
path.chmod(0o600)
print(f"Local environment ready; added {len(missing)} missing settings, preserved existing values.")

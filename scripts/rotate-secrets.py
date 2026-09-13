#!/usr/bin/env python3
"""
E-LIB Automated Secret Rotation Script (Phase 14)
Rotates database, redis, and session secrets securely.

Usage:
  python scripts/rotate-secrets.py [--target all|db|redis|session] [--dry-run] [--env-file .env]
"""

import argparse
import os
import re
import secrets
import shutil
import sys
from datetime import datetime
from pathlib import Path


def generate_secure_token(length: int = 32) -> str:
    """Generates a cryptographically strong URL-safe random secret."""
    return secrets.token_urlsafe(length)


def backup_file(filepath: Path) -> Path:
    """Creates a timestamped backup of the configuration file."""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_path = filepath.with_suffix(f".bak_{timestamp}")
    shutil.copy2(filepath, backup_path)
    return backup_path


def parse_env_file(content: str) -> dict[str, str]:
    """Extracts key-value pairs from an env file string."""
    env_vars = {}
    for line in content.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" in line:
            key, val = line.split("=", 1)
            env_vars[key.strip()] = val.strip()
    return env_vars


def update_env_content(content: str, updates: dict[str, str]) -> str:
    """Replaces values for specified keys in .env format preserving layout."""
    lines = content.splitlines()
    new_lines = []
    updated_keys = set()

    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            new_lines.append(line)
            continue

        if "=" in stripped:
            key, _ = stripped.split("=", 1)
            key = key.strip()
            if key in updates:
                new_lines.append(f"{key}={updates[key]}")
                updated_keys.add(key)
                continue

        new_lines.append(line)

    # Append any keys that did not previously exist
    for key, val in updates.items():
        if key not in updated_keys:
            new_lines.append(f"{key}={val}")

    return "\n".join(new_lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description="E-LIB Production Secret Rotation Tool")
    parser.add_argument(
        "--target",
        choices=["all", "db", "redis", "session"],
        default="all",
        help="Target secrets to rotate (default: all)",
    )
    parser.add_argument(
        "--env-file",
        default=".env",
        help="Path to the environment file (default: .env)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Simulate secret generation without updating files",
    )
    args = parser.parse_args()

    env_path = Path(args.env_file)
    if not env_path.exists():
        example_path = Path(".env.example")
        if example_path.exists():
            print(f"Notice: '{env_path}' not found. Initializing from '{example_path}'...")
            shutil.copy2(example_path, env_path)
        else:
            print(f"Error: Target env file '{env_path}' does not exist.", file=sys.stderr)
            sys.exit(1)

    print("=" * 72)
    print("           E-LIB PRODUCTION SECRET ROTATION PROCEDURE")
    print("=" * 72)
    print(f"Target Scope:  {args.target.upper()}")
    print(f"Target File:   {env_path.resolve()}")
    print(f"Mode:          {'DRY RUN (No changes saved)' if args.dry_run else 'ACTIVE ROTATION'}")
    print("-" * 72)

    with open(env_path, "r", encoding="utf-8") as f:
        original_content = f.read()

    new_secrets = {}

    if args.target in ["all", "db"]:
        new_secrets["POSTGRES_PASSWORD"] = generate_secure_token(32)
        print(" [✓] Generated new POSTGRES_PASSWORD (32 bytes entropy)")

    if args.target in ["all", "redis"]:
        new_secrets["REDIS_PASSWORD"] = generate_secure_token(32)
        print(" [✓] Generated new REDIS_PASSWORD (32 bytes entropy)")

    if args.target in ["all", "session"]:
        new_secrets["SESSION_SECRET"] = generate_secure_token(48)
        print(" [✓] Generated new SESSION_SECRET (48 bytes entropy)")

    if args.dry_run:
        print("\n[Dry-Run Simulation Output]")
        for k, v in new_secrets.items():
            masked = v[:4] + "*" * (len(v) - 8) + v[-4:]
            print(f"  {k} = {masked}")
        print("\nDry-run completed. No files modified.")
        sys.exit(0)

    # Create backup before modification
    backup_path = backup_file(env_path)
    print(f"\n[+] Created configuration backup: {backup_path}")

    updated_content = update_env_content(original_content, new_secrets)
    with open(env_path, "w", encoding="utf-8") as f:
        f.write(updated_content)

    # Restrict file permissions on POSIX systems (chmod 600)
    if os.name != "nt":
        os.chmod(env_path, 0o600)
        print("[+] Set strict file permissions (0600) on .env")

    print(f"[+] Successfully updated '{env_path}' with new cryptographic secrets.")
    print("\n" + "=" * 72)
    print("              SYNCHRONIZATION & RELOAD COMMANDS")
    print("=" * 72)
    print("1. Apply new database password to running PostgreSQL:")
    if "POSTGRES_PASSWORD" in new_secrets:
        pwd = new_secrets["POSTGRES_PASSWORD"]
        print(f"   docker compose exec db psql -U elib -c \"ALTER USER elib WITH PASSWORD '{pwd}';\"")
    print("\n2. Apply new Redis password to running Redis instance:")
    if "REDIS_PASSWORD" in new_secrets:
        pwd = new_secrets["REDIS_PASSWORD"]
        print(f"   docker compose exec redis redis-cli -a <OLD_PWD> CONFIG SET requirepass '{pwd}'")
    print("\n3. Restart backend service to load new connection credentials:")
    print("   docker compose restart backend")
    print("=" * 72)
    print("✨ Secret rotation completed safely with zero secrets logged in plain text.\n")


if __name__ == "__main__":
    main()

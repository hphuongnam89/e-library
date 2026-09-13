#!/usr/bin/env python3
"""Exercise the running Compose stack, optionally pausing only this project's dependencies."""
import argparse
import json
import subprocess
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument("--base-url", default="http://127.0.0.1:5173")
parser.add_argument("--check-outages", action="store_true")
args = parser.parse_args()


def request(path, expected, method="GET"):
    req = Request(args.base_url.rstrip("/") + path, method=method)
    try:
        response = urlopen(req, timeout=15)
    except HTTPError as error:
        response = error
    with response:
        body = response.read()
        assert response.status == expected, f"{path}: expected {expected}, got {response.status}"
        return body, response.headers


def ready():
    body, _ = request("/api/system/health/readiness", 200)
    assert json.loads(body) == {"status": "UP"}


ready()
request("/system", 200)
body, headers = request("/api/admin/users", 401)
assert "application/problem+json" in headers["Content-Type"]
assert json.loads(body)["requestId"] == headers["X-Request-ID"]
request("/api/admin/users", 403, "POST")
# Phase 2 — Auth endpoints
csrf_body, _ = request("/api/v1/auth/csrf", 200)
assert "token" in json.loads(csrf_body)
me_body, me_headers = request("/api/v1/auth/me", 401)
assert "application/problem+json" in me_headers["Content-Type"]
assert json.loads(me_body)["requestId"] == me_headers["X-Request-ID"]
# Phase 3 — Organization endpoints
inst_body, inst_headers = request("/api/v1/institutions", 401)
assert "application/problem+json" in inst_headers["Content-Type"]
assert json.loads(inst_body)["requestId"] == inst_headers["X-Request-ID"]
request("/api/v1/institutions", 403, "POST")
# Phase 4 — Catalog endpoints
cat_body, cat_headers = request("/api/v1/categories", 401)
assert "application/problem+json" in cat_headers["Content-Type"]
assert json.loads(cat_body)["requestId"] == cat_headers["X-Request-ID"]
request("/api/v1/categories", 403, "POST")
titles_body, titles_headers = request("/api/v1/book-titles", 401)
assert "application/problem+json" in titles_headers["Content-Type"]
assert json.loads(titles_body)["requestId"] == titles_headers["X-Request-ID"]
request("/api/v1/book-titles", 403, "POST")
copies_body, copies_headers = request("/api/v1/book-copies", 401)
assert "application/problem+json" in copies_headers["Content-Type"]
assert json.loads(copies_body)["requestId"] == copies_headers["X-Request-ID"]
request("/api/v1/book-copies", 403, "POST")
# Phase 5 — Circulation endpoints
borrows_body, borrows_headers = request("/api/v1/borrows", 401)
assert "application/problem+json" in borrows_headers["Content-Type"]
assert json.loads(borrows_body)["requestId"] == borrows_headers["X-Request-ID"]
request("/api/v1/borrows", 403, "POST")
me_borrows_body, me_borrows_headers = request("/api/v1/me/borrows", 401)
assert "application/problem+json" in me_borrows_headers["Content-Type"]
assert json.loads(me_borrows_body)["requestId"] == me_borrows_headers["X-Request-ID"]
print("PASS: live readiness, deep link, unauthorized JSON, CSRF, Phase 2 auth, Phase 3 org, Phase 4 catalog, and Phase 5 circulation endpoints")

if args.check_outages:
    for service in ("redis", "db"):
        subprocess.run(["docker", "compose", "pause", service], cwd=root, check=True, capture_output=True)
        try:
            body, _ = request("/api/system/health/readiness", 503)
            assert json.loads(body)["status"] == "DOWN"
            body, _ = request("/api/system/health/liveness", 200)
            assert json.loads(body) == {"status": "UP"}
            print(f"PASS: {service} unavailable => readiness 503, liveness 200")
        finally:
            subprocess.run(["docker", "compose", "unpause", service], cwd=root, check=True, capture_output=True)
        for attempt in range(20):
            try:
                ready()
                break
            except (AssertionError, OSError):
                if attempt == 19:
                    raise
                time.sleep(1)
        print(f"PASS: {service} recovery => readiness 200")

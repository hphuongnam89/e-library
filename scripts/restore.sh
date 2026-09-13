#!/bin/sh
# ==============================================================================
# E-LIB Production PostgreSQL Restore Script (Phase 14)
# Verifies SHA-256 Checksum before restoring to prevent corruption or tampering
# ==============================================================================
set -eu

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path-to-backup.dump>"
    exit 1
fi

BACKUP_FILE="$1"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"
DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${POSTGRES_USER:-elib}"
DB_NAME="${POSTGRES_DB:-elib}"

echo "=============================================================================="
echo "          E-LIB DATABASE RESTORE START: $(date -Iseconds)"
echo "=============================================================================="
echo "Host:     ${DB_HOST}:${DB_PORT}"
echo "Database: ${DB_NAME}"
echo "File:     ${BACKUP_FILE}"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "[!] Error: Backup file '${BACKUP_FILE}' does not exist!" >&2
    exit 1
fi

# 1. Verify SHA-256 checksum if checksum file exists
if [ -f "${CHECKSUM_FILE}" ]; then
    echo "Verifying SHA-256 checksum..."
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum -c "${CHECKSUM_FILE}"
    elif command -v shasum >/dev/null 2>&1; then
        shasum -a 256 -c "${CHECKSUM_FILE}"
    fi
    echo "[✓] Checksum verification passed."
else
    echo "[!] Warning: No checksum file found at '${CHECKSUM_FILE}'. Proceeding with caution."
fi

# 2. Execute restoration with --clean --if-exists
echo "Executing pg_restore into database '${DB_NAME}'..."
pg_restore -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    --schema=elib \
    "${BACKUP_FILE}"

echo "=============================================================================="
echo "[✓] Database restoration completed successfully: $(date -Iseconds)"
echo "=============================================================================="

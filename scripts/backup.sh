#!/bin/sh
# ==============================================================================
# E-LIB Production PostgreSQL Backup Script (Phase 14)
# Formats: PostgreSQL Custom Compressed Archive (-Fc) + SHA-256 Checksum
# ==============================================================================
set -eu

BACKUP_DIR="${BACKUP_DIR:-/var/backups/elib}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${POSTGRES_USER:-elib}"
DB_NAME="${POSTGRES_DB:-elib}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/elib_${TIMESTAMP}.dump"
CHECKSUM_FILE="${BACKUP_FILE}.sha256"

mkdir -p "${BACKUP_DIR}"

echo "=============================================================================="
echo "          E-LIB DATABASE BACKUP START: $(date -Iseconds)"
echo "=============================================================================="
echo "Host:     ${DB_HOST}:${DB_PORT}"
echo "Database: ${DB_NAME}"
echo "Output:   ${BACKUP_FILE}"

# 1. Execute compressed custom archive dump
pg_dump -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
    --format=custom \
    --schema=elib \
    --no-owner \
    --no-privileges \
    --file="${BACKUP_FILE}"

# 2. Compute SHA-256 checksum for data integrity verification
if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${BACKUP_FILE}" > "${CHECKSUM_FILE}"
elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "${BACKUP_FILE}" > "${CHECKSUM_FILE}"
fi

BACKUP_SIZE="$(ls -lh "${BACKUP_FILE}" | awk '{print $5}')"
echo "[✓] Backup created successfully: ${BACKUP_FILE} (${BACKUP_SIZE})"
echo "[✓] Checksum generated: $(cat "${CHECKSUM_FILE}")"

# 3. Retention policy: Remove backups older than RETENTION_DAYS
echo "Applying retention policy (${RETENTION_DAYS} days)..."
find "${BACKUP_DIR}" -type f \( -name "elib_*.dump" -o -name "elib_*.dump.sha256" \) -mtime +"${RETENTION_DAYS}" -delete

echo "=============================================================================="
echo "          E-LIB DATABASE BACKUP COMPLETED: $(date -Iseconds)"
echo "=============================================================================="

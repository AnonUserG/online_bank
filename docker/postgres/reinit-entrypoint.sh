#!/bin/sh
set -euo pipefail

DATA_DIR="${PGDATA:-/var/lib/postgresql/data}"
mkdir -p "$DATA_DIR"
echo "[reinit] Resetting Postgres data directory at $DATA_DIR"
find "$DATA_DIR" -mindepth 1 -delete

exec /usr/local/bin/docker-entrypoint.sh postgres

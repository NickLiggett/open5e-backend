#!/bin/sh
# Runs once, when the Postgres data volume is first created (docker-entrypoint-initdb.d).
# Loads the open5e dump; Flyway then baselines the restored schema when the app starts.
set -e

DUMP=/dump/open5e_backup.dump
if [ ! -f "$DUMP" ]; then
    echo "ERROR: $DUMP not found. Copy open5e_backup.dump into docker/postgres/, then run" >&2
    echo "       'docker compose down -v' and start again (see README)." >&2
    exit 1
fi

echo "Restoring $DUMP into $POSTGRES_DB"
pg_restore --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --no-owner --no-privileges --exit-on-error "$DUMP"
echo "Restore complete"

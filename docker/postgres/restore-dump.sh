#!/bin/sh
# Runs once, when the Postgres data volume is first created (docker-entrypoint-initdb.d).
# Loads docker/postgres/open5e_backup.dump if it's there. The dump must come from a database this app created: it
# includes Flyway's history, so the app sees an up-to-date schema. Without a dump the database starts empty: the app
# creates the schema, and the importer loads the default content from the Open5e API (see README).
set -e

DUMP=/dump/open5e_backup.dump
if [ ! -f "$DUMP" ]; then
    echo "No dump at docker/postgres/open5e_backup.dump: starting with an empty database."
    echo "Load the default content with the importer once the app has started (see README)."
    exit 0
fi

echo "Restoring $DUMP into $POSTGRES_DB"
pg_restore --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --no-owner --no-privileges --exit-on-error "$DUMP"
echo "Restore complete"

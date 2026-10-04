#!/bin/bash
# Dumps both databases (the app's and Keycloak's) to /var/backups/dnddms, keeping the last 14 days. The server setup
# runs it every night. These are on the same server as the data: copy them somewhere else too (see README).
#   restore:  docker compose -f compose.prod.yaml exec -T db pg_restore -U open5e -d open5e --clean --if-exists < file.dump
set -euo pipefail
cd "$(dirname "$0")"
dir="${BACKUP_DIR:-/var/backups/dnddms}"
mkdir -p "$dir"
stamp="$(date +%F-%H%M)"
for database in open5e keycloak; do
  docker compose -f compose.prod.yaml exec -T db pg_dump -U open5e -d "$database" -Fc > "$dir/$database-$stamp.dump"
done
find "$dir" -name '*.dump' -mtime +14 -delete
echo "Backed up to $dir ($stamp)"

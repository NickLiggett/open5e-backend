#!/bin/bash
# Runs once, when Postgres first creates its data directory: Keycloak gets its own database and user, so it can't read
# the app's data and the app can't read Keycloak's.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE ROLE keycloak LOGIN PASSWORD '${KEYCLOAK_DB_PASSWORD}';
CREATE DATABASE keycloak OWNER keycloak;
SQL

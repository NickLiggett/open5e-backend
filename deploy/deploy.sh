#!/bin/bash
# Deploys to this server: makes the Keycloak realm file from the template, pulls the images and starts what changed.
# Run from the folder it is in (the workflow does), with the .env file there.
#   ./deploy.sh [backend-tag] [frontend-tag]      tags default to what .env says, then to "latest"
# To go back, run it again with the tags of the version you want (kept in .last-deploy after each deploy).
set -euo pipefail
cd "$(dirname "$0")"

[ -f .env ] || { echo "No .env here. Copy .env.example to .env and fill it in." >&2; exit 1; }
set -a
# shellcheck disable=SC1091
. ./.env
set +a

: "${DOMAIN:?DOMAIN is not set in .env}"
for name in ACME_EMAIL POSTGRES_PASSWORD KEYCLOAK_DB_PASSWORD KEYCLOAK_ADMIN_PASSWORD SMTP_HOST SMTP_USERNAME SMTP_PASSWORD MAIL_FROM; do
  [ -n "${!name:-}" ] || { echo "$name is not set in .env" >&2; exit 1; }
done

export BACKEND_TAG="${1:-${BACKEND_TAG:-latest}}"
export FRONTEND_TAG="${2:-${FRONTEND_TAG:-latest}}"
export SMTP_PORT="${SMTP_PORT:-587}"
COMPOSE="docker compose -f compose.prod.yaml"

echo "Deploying backend:$BACKEND_TAG frontend:$FRONTEND_TAG to $DOMAIN"

# Keycloak reads its realm from this file the first time it starts. It holds the mail password, so keep it private.
mkdir -p keycloak/generated
umask 077
envsubst '${DOMAIN} ${SMTP_HOST} ${SMTP_PORT} ${SMTP_USERNAME} ${SMTP_PASSWORD} ${MAIL_FROM}' \
  < keycloak/open5e-realm.template.json > keycloak/generated/open5e-realm.json
chmod 644 keycloak/generated/open5e-realm.json # the Keycloak container's user has to read it; the folder keeps others out
chmod 700 keycloak/generated

if [ -n "${GHCR_USER:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
fi

# PULL_IMAGES=false is for trying this out with images built on this machine; the server always pulls.
[ "${PULL_IMAGES:-true}" = "false" ] || $COMPOSE pull
$COMPOSE up -d --remove-orphans

# The first start takes a while: Keycloak sets up its database and Caddy asks Let's Encrypt for certificates.
echo "Waiting for https://$DOMAIN/api/documents ..."
for attempt in $(seq 1 60); do
  # HEALTH_CURL_ARGS is for trying this out on a machine without a real certificate (-k); leave it unset on the server.
  # shellcheck disable=SC2086
  if curl -fsS ${HEALTH_CURL_ARGS:-} -o /dev/null --max-time 10 "https://$DOMAIN/api/documents?pageSize=1"; then
    echo "Up."
    printf 'BACKEND_TAG=%s\nFRONTEND_TAG=%s\nDEPLOYED_AT=%s\n' "$BACKEND_TAG" "$FRONTEND_TAG" "$(date -u +%FT%TZ)" > .last-deploy
    docker image prune -f > /dev/null
    $COMPOSE ps
    exit 0
  fi
  sleep 5
done

echo "The site did not come up within 5 minutes. Recent logs:" >&2
$COMPOSE ps >&2
$COMPOSE logs --tail=40 caddy backend keycloak >&2
exit 1

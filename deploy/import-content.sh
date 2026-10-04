#!/bin/bash
# Loads, or refreshes, the default Open5e content (creatures, spells, items, ...) from the Open5e API into the database.
# Needed once after the first deploy, and any time you want Open5e's latest. It never touches users' homebrew; see
# "Refreshing default content" in the main README.   ./import-content.sh [dry-run|apply]   (apply by default)
set -euo pipefail
cd "$(dirname "$0")"
mode="${1:-apply}"
docker compose -f compose.prod.yaml run --rm backend --spring.profiles.active=import --open5e.import.mode="$mode"

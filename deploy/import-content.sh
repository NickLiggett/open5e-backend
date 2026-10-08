#!/bin/bash
# Loads, or refreshes, the default content: Open5e's (creatures, spells, items, ...), fetched from the Open5e API, together
# with this project's own custom content (src/main/resources/custom-content in the backend image). Needed once after the
# first deploy, and any time you want Open5e's latest or have deployed new custom content. It never touches users'
# homebrew; see "Refreshing default content" in the main README.
#   ./import-content.sh [dry-run|apply] [all|custom]      (apply and all by default)
# "custom" applies only the custom content, without fetching Open5e at all, and leaves Open5e's rows exactly as they are.
# Either way, the private content in ./private-content is applied last, as its owners' own documents.
set -euo pipefail
cd "$(dirname "$0")"
mode="${1:-apply}"
content="${2:-all}"
docker compose -f compose.prod.yaml run --rm backend --spring.profiles.active=import --open5e.import.mode="$mode" --open5e.import.content="$content"

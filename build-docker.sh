#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$SCRIPT_DIR"

COMPOSE_CMD=(docker compose -f docker/docker-compose.yml)

"${COMPOSE_CMD[@]}" build build
"${COMPOSE_CMD[@]}" run --rm --no-deps build
printf 'Linux native executable created: %s/dist/talispam\n' "$SCRIPT_DIR"

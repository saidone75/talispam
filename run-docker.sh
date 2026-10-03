#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$SCRIPT_DIR"

COMPOSE_CMD=(docker compose -f docker/docker-compose.yml)

ACTION="${1:-}"
case "$ACTION" in
    build)
        "${COMPOSE_CMD[@]}" build build
        "${COMPOSE_CMD[@]}" run --rm --no-deps build
        printf 'Linux native executable created: %s/dist/talispam\n' "$SCRIPT_DIR"
        ;;
    purge)
        "${COMPOSE_CMD[@]}" down --rmi local --remove-orphans
        ;;
    *)
        printf 'Usage: %s {build|purge}\n' "${0##*/}" >&2
        exit 1
        ;;
esac

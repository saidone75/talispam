#!/usr/bin/env bash
#
# talispam — A charm to avert spam and bring good messages.
#
# Copyright (c) 2020-2026 Saidone
# SPDX-License-Identifier: MIT
#
# See LICENSE in the project root for the full license text.
#

set -euo pipefail

BUILD_OPTIONS=()
for arg in "$@"; do
  case "$arg" in
    --no-cache) BUILD_OPTIONS=(--no-cache) ;;
    -h|--help)
      printf 'Usage: %s [--no-cache]\n' "$0"
      exit 0
      ;;
    *)
      printf 'Unknown option: %s\nUsage: %s [--no-cache]\n' "$arg" "$0" >&2
      exit 1
      ;;
  esac
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$SCRIPT_DIR"

COMPOSE_CMD=(docker compose -f docker/docker-compose.yml)

"${COMPOSE_CMD[@]}" build "${BUILD_OPTIONS[@]}" build
"${COMPOSE_CMD[@]}" run --rm --no-deps build
printf 'Linux native executable created in: %s/dist/ (talispam-<version>)\n' "$SCRIPT_DIR"

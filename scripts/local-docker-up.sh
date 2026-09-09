#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-.env.local}"
COMPOSE_INFRA="${COMPOSE_INFRA:-docker-compose.infra.yml}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.local.yml}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy from .env.local.example"
  exit 1
fi

# shellcheck disable=SC1090
set -a
source "$ENV_FILE"
set +a

is_tunnel_mode() {
  [[ "${POSTGRES_HOST:-}" == "host.docker.internal" ]] \
    || [[ "${RABBIT_HOST:-}" == "host.docker.internal" ]]
}

wait_healthy() {
  local container="$1" max_attempts="${2:-30}"
  for ((i = 1; i <= max_attempts; i++)); do
    local status
    status="$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container" 2>/dev/null || echo "missing")"
    case "$status" in
      healthy) echo "OK:   $container is healthy"; return 0 ;;
      missing) echo "WARN: container $container not found"; return 1 ;;
      *) sleep 2 ;;
    esac
  done
  echo "WARN: $container did not become healthy in time"
  return 1
}

ensure_infra() {
  if is_tunnel_mode; then
    return 0
  fi

  if ! docker network inspect hesap-local >/dev/null 2>&1; then
    echo "Creating network and starting infra (postgres + rabbitmq)..."
    docker compose -f "$COMPOSE_INFRA" --env-file "$ENV_FILE" up -d
  elif ! docker ps --format '{{.Names}}' | grep -qE '^(postgres-local|businessmq-local)$'; then
    echo "Starting infra containers..."
    docker compose -f "$COMPOSE_INFRA" --env-file "$ENV_FILE" up -d
  else
    echo "Infra already running."
  fi

  wait_healthy postgres-local || true
  wait_healthy businessmq-local || true
}

check_tunnel() {
  local port="$1" name="$2"
  if nc -z 127.0.0.1 "$port" 2>/dev/null; then
    echo "OK:   $name on 127.0.0.1:$port"
  else
    echo "WARN: $name not reachable on 127.0.0.1:$port — run: ssh -N hesap-tunnel"
  fi
}

if is_tunnel_mode; then
  echo "Tunnel mode: checking host ports..."
  check_tunnel "${POSTGRES_PORT:-5435}" "Postgres"
  check_tunnel "${RABBIT_PORT:-35672}" "RabbitMQ"
else
  echo "Infra mode: postgres + rabbitmq in Docker (hesap-local network)"
  ensure_infra
fi

echo "Build..."
./gradlew clean build -x test

mkdir -p "${FILE_STORAGE_PATH:-./local-data/files}"

echo "Starting app stack..."
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build "$@"

GW_PORT="${PORT_GATEWAY:-8000}"
echo ""
echo "Gateway: http://localhost:${GW_PORT}"
echo "Swagger: http://localhost:${GW_PORT}/swagger-ui.html"
echo "Logs:    docker compose -f $COMPOSE_FILE --env-file $ENV_FILE logs -f user"

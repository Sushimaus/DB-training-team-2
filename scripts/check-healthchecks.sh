#!/usr/bin/env bash
# TICKET-ADV152 — Verify every healthcheck works in isolation.
#
# Brings the stack up one dependency layer at a time and asserts each
# container reaches `healthy` within its retry budget before moving on.
# On failure, prints the container's logs and the exact healthcheck test
# command so you can `docker exec` it by hand (per Hint 1: don't guess).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

wait_healthy() {
  local name="$1" max_attempts="$2" sleep_s="$3"
  for ((i = 1; i <= max_attempts; i++)); do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "starting")
    if [[ "$status" == "healthy" ]]; then
      echo "  $name healthy (after ~$((i * sleep_s))s)"
      return 0
    fi
    sleep "$sleep_s"
  done
  echo "  $name did NOT become healthy within $((max_attempts * sleep_s))s (status: $status)"
  echo "  --- last logs for $name ---"
  docker logs "$name" --tail 30 2>&1 || true
  echo "  --- configured healthcheck test ---"
  docker inspect --format='{{json .Config.Healthcheck.Test}}' "$name" 2>/dev/null || true
  return 1
}

echo "[1/8] postgres..."
docker compose up -d postgres
wait_healthy reconx-postgres 10 1

echo "[2/8] zookeeper..."
docker compose up -d zookeeper
wait_healthy reconx-zookeeper 15 2

echo "[3/8] kafka..."
docker compose up -d kafka
wait_healthy reconx-kafka 15 2

echo "[4/8] prometheus..."
docker compose up -d prometheus
wait_healthy reconx-prometheus 10 2

echo "[5/8] grafana..."
docker compose up -d grafana
wait_healthy reconx-grafana 15 2

echo "[6/8] backend..."
docker compose up -d backend
wait_healthy reconx-backend 20 3

echo "[7/8] frontend..."
docker compose up -d frontend
wait_healthy reconx-frontend 10 2

echo "[8/8] kafdrop (debug profile)..."
docker compose --profile debug up -d kafdrop
wait_healthy reconx-kafdrop 15 2

echo
echo "All healthchecks green."
docker compose --profile debug ps

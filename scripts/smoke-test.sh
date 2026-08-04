#!/usr/bin/env bash
# ============================================================================
# File: scripts/smoke-test.sh
# TICKET-ADV153 — End-to-end smoke test for the full 7-service stack
# Run from repo root: bash scripts/smoke-test.sh
#
# Seven checks: stack up, login, trade POST, Kafka event consumed, Postgres
# audit row, Prometheus target UP, Grafana datasource reachable. Exits 0 only
# if every check passes; prints "[step N] ... FAILED" and exits non-zero on
# the first failure.
# ============================================================================
set -euo pipefail

BACKEND="http://localhost:8080/api"
TRADE_REF="SMK-$(date +%Y%m%d)-0001"

echo "[step 1/7] Bringing the stack up..."
docker compose down -v >/dev/null 2>&1 || true
docker compose up -d
elapsed=0
until [[ "$(docker inspect --format='{{.State.Health.Status}}' reconx-backend 2>/dev/null || echo starting)" == "healthy" ]]; do
  elapsed=$((elapsed + 5))
  if [[ $elapsed -ge 120 ]]; then
    echo "[step 1] stack up FAILED: backend did not become healthy within 120s"
    exit 1
  fi
  sleep 5
done
echo "  ok - backend healthy"

echo "[step 2/7] Logging in..."
TOKEN=$(curl -fsS -X POST "${BACKEND}/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"trader@db.com","password":"trader123"}' | jq -r .token)
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "[step 2] login FAILED: no token in response"
  exit 1
fi
echo "  ok - JWT acquired"

echo "[step 3/7] Posting a trade..."
TRADE=$(curl -fsS -X POST "${BACKEND}/v1/trades" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d "{\"tradeRef\":\"${TRADE_REF}\",\"instrumentId\":1,\"counterpartyId\":1,\"assetClass\":\"EQUITY\",\"side\":\"BUY\",\"quantity\":100,\"price\":245.50,\"tradeDate\":\"2026-06-02\"}")
TRADE_ID=$(echo "$TRADE" | jq -r .id)
if [[ -z "$TRADE_ID" || "$TRADE_ID" == "null" ]]; then
  echo "[step 3] trade POST FAILED: no id in response: $TRADE"
  exit 1
fi
echo "  ok - trade created: id=$TRADE_ID ref=$TRADE_REF"

echo "[step 4/7] Confirming Kafka event..."
if docker exec reconx-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic trade-events \
  --from-beginning --max-messages 20 --timeout-ms 15000 2>/dev/null | grep -q "$TRADE_REF"; then
  echo "  ok - trade-event found on topic"
else
  echo "[step 4] Kafka event FAILED: $TRADE_REF not found on trade-events topic"
  exit 1
fi

echo "[step 5/7] Confirming Postgres audit row..."
COUNT=$(docker exec reconx-postgres psql -U reconx_user -d reconx -tAc \
  "SELECT COUNT(*) FROM audit_log WHERE trade_ref = '${TRADE_REF}';" | tr -d '[:space:]')
if [[ "$COUNT" -ge 1 ]]; then
  echo "  ok - audit row present ($COUNT row(s))"
else
  echo "[step 5] Postgres audit row FAILED: no audit_log row for $TRADE_REF"
  exit 1
fi

echo "[step 6/7] Confirming Prometheus scrape..."
if curl -fsS "http://localhost:9090/api/v1/query?query=up%7Bjob%3D%22reconx-backend%22%7D" \
  | jq -e '.data.result[0].value[1]=="1"' >/dev/null 2>&1; then
  echo "  ok - Prometheus scraping backend"
else
  echo "[step 6] Prometheus FAILED: reconx-backend target not UP"
  exit 1
fi

echo "[step 7/7] Confirming Grafana datasource..."
if curl -fsS -u admin:admin "http://localhost:3000/api/datasources/uid/reconx-prometheus" \
  | jq -e '.uid=="reconx-prometheus"' >/dev/null 2>&1; then
  echo "  ok - Grafana datasource provisioned"
else
  echo "[step 7] Grafana FAILED: datasource reconx-prometheus not reachable"
  exit 1
fi

echo
echo "All 7 checks green - stack is demo-ready."

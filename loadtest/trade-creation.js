// ============================================================================
// File: loadtest/trade-creation.js
// TICKET-ADV158 — k6 load test: 200 concurrent users posting trades for 2 min
// Run:  k6 run loadtest/trade-creation.js
//       BASE_URL=http://localhost:8080 k6 run loadtest/trade-creation.js
// ============================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const tradeLatency = new Trend('trade_post_latency_ms');
const errorRate    = new Rate('trade_post_errors');

export const options = {
  scenarios: {
    constant_load: {
      executor:     'constant-vus',
      vus:          200,
      duration:     '2m',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'trade_post_latency_ms': ['p(95)<800', 'p(99)<2000'],
    'trade_post_errors':     ['rate<0.02'],
    'http_req_failed':       ['rate<0.02'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// One-time login before VUs spin up — every VU reuses this token instead of
// logging in on every iteration.
export function setup() {
  const res = http.post(`${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'trader@db.com', password: 'trader123' }),
    { headers: { 'Content-Type': 'application/json' } });

  const token = res.json('token');
  if (!token) {
    throw new Error(`login failed: ${res.status} ${res.body}`);
  }
  return { token };
}

export default function (data) {
  // Real TradeRequest contract (backend/src/main/java/.../dto/TradeRequest.java):
  // tradeRef must match ^[A-Z]{3}-\d{8}-\d{4}$ (exactly 3 letters, 8 digits,
  // 4 digits — no digits in the letter segment). instrumentId/counterpartyId
  // reference seeded rows (id=1 in both instruments and counterparties).
  // __VU (unique per VU) + __ITER (strictly increasing per VU) together
  // guarantee no tradeRef collision within a run.
  const datePart = String(__VU).padStart(4, '0')
                 + String(Math.floor(__ITER / 10000) % 10000).padStart(4, '0');
  const seqPart = String(__ITER % 10000).padStart(4, '0');
  const tradeRef = `LDT-${datePart}-${seqPart}`;

  const payload = JSON.stringify({
    tradeRef:       tradeRef,
    instrumentId:   1,
    counterpartyId: 1,
    assetClass:     'EQUITY',
    side:           __VU % 2 === 0 ? 'BUY' : 'SELL',
    quantity:       100 + (__VU % 50),
    price:          245.50 + (__ITER % 10) * 0.01,
    tradeDate:      '2026-06-02',
  });

  const t0 = Date.now();
  const res = http.post(`${BASE_URL}/api/v1/trades`, payload, {
    headers: {
      'Content-Type': 'application/json',
      Authorization:  `Bearer ${data.token}`,
    },
  });
  tradeLatency.add(Date.now() - t0);

  const ok = check(res, {
    '201 created':  (r) => r.status === 201,
    'has trade id': (r) => !!r.json('id'),
  });
  errorRate.add(!ok);

  sleep(0.5);
}

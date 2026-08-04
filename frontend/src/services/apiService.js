// TICKET-ADV112-related — fetch wrapper that attaches Bearer JWT from sessionStorage.
const BASE = '/api';

function authHeaders() {
  const token = typeof sessionStorage !== 'undefined'
    ? sessionStorage.getItem('reconx-token')
    : null;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(method, path, body) {
  const headers = {
    'Content-Type': 'application/json',
    ...authHeaders(),
  };
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body != null ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    let detail = res.statusText;
    try {
      const payload = await res.text();
      if (payload) detail = payload;
    } catch { /* ignore parse errors */ }
    throw new Error(`HTTP ${res.status}: ${detail}`);
  }
  if (res.status === 204) return null;
  const contentType = res.headers.get('content-type') || '';
  if (!contentType.includes('application/json')) return null;
  return res.json();
}

function toQueryString(params) {
  if (!params || typeof params !== 'object') return '';
  const usp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      usp.set(key, value);
    }
  });
  const qs = usp.toString();
  return qs ? `?${qs}` : '';
}

export const api = {
  login: (email, password) => {
    return request('POST', '/auth/login', { email, password });
  },

  listTrades: (params = {}) => {
    return request('GET', `/v1/trades${toQueryString(params)}`);
  },

  createTrade: (req) => {
    return request('POST', '/v1/trades', req);
  },

  updateStatus: (id, status) => {
    return request('PATCH', `/v1/trades/${id}/status`, { status });
  },
  deleteTrade: (id) => {
    return request('DELETE', `/v1/trades/${id}`);
  },
  runRecon: (req) => {
    return request('POST', '/v1/recon/run', req);
  },
  reconResults: (jobId) => {
    return request('GET', `/v1/recon/jobs/${jobId}/results`);
  },
  audit: (tradeRef) => {
    return request('GET', `/v1/audit/trades/${tradeRef}`);
  },
};
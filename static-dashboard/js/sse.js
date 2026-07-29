// TICKET-ADV106 / ADV107 — EventSource live feed with prepend + slide-in animation.
(function () {
  const FEED_EL = document.getElementById('trade-feed');
  if (!FEED_EL) return;

  const STREAM_URL = '/api/v1/trades/stream';
  let sse = null;
  let connectionStatus = 'connecting';

  function updateConnectionBadge(text, variant) {
    const badge = document.getElementById('sse-status');
    if (!badge) return;
    badge.textContent = text;
    badge.className = 'sse-status sse-status--' + variant;
  }

  function prepend(trade) {
    const el = document.createElement('article');
    el.className = 'trade-card trade-card--' + trade.status.toLowerCase();
    el.innerHTML = `
      <strong>${trade.tradeRef}</strong>
      <span> ${trade.symbol} </span>
      <span> qty=${trade.qty} </span>
      <span> price=${trade.price} </span>
      <span> [${trade.status}]</span>`;
    FEED_EL.prepend(el);
  }

  function connect() {
    sse = new EventSource(STREAM_URL);

    sse.onopen = () => {
      connectionStatus = 'live';
      updateConnectionBadge('Live', 'live');
    };

    sse.onmessage = (event) => {
      try {
        const trade = JSON.parse(event.data);
        prepend(trade);
      } catch (err) {
        console.error('Failed to parse SSE trade event', err);
      }
    };

    sse.onerror = () => {
      connectionStatus = 'reconnecting';
      updateConnectionBadge('Reconnecting…', 'reconnecting');
      // Do NOT call connect() here — EventSource auto-reconnects with
      // backoff on its own. Manually reconnecting would DDoS the dev server.
    };
  }

  window.addEventListener('beforeunload', () => sse?.close());

  connect();
})();
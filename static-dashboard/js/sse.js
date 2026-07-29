// TICKET-ADV105 — EventSource live feed with prepend + slide-in animation.
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

  const quantityFormatter = new Intl.NumberFormat('en-US');
  const priceFormatter = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  });

  function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (char) => {
      switch (char) {
        case '&':
          return '&amp;';
        case '<':
          return '&lt;';
        case '>':
          return '&gt;';
        case '"':
          return '&quot;';
        case '\'':
          return '&#39;';
        default:
          return char;
      }
    });
  }

  function getStatusModifier(status) {
    if (status === 'MATCHED') return 'trade-card--matched';
    if (status === 'BREAK' || status === 'UNMATCHED') return 'trade-card--break';
    return '';
  }

  function prependTradeRow(trade) {
    const row = document.createElement('article');
    const status = String(trade.status ?? 'PENDING').toUpperCase();
    const statusModifier = getStatusModifier(status);

    row.className = ['trade-card', statusModifier, 'trade-card--new']
      .filter(Boolean)
      .join(' ');

    row.innerHTML = `
      <header class="trade-card__header">
        <strong>${escapeHtml(trade.tradeRef)}</strong>
        <span>[${escapeHtml(status)}]</span>
      </header>
      <div class="trade-card__body">
        <span>${escapeHtml(trade.symbol)}</span>
        <span>qty=${quantityFormatter.format(Number(trade.qty) || 0)}</span>
        <span>price=${priceFormatter.format(Number(trade.price) || 0)}</span>
      </div>
    `;

    FEED_EL.prepend(row);
    setTimeout(() => row.classList.remove('trade-card--new'), 500);

    while (FEED_EL.children.length > 50) {
      FEED_EL.lastElementChild.remove();
    }
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
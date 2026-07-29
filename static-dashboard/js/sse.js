// TICKET-ADV105 — EventSource live feed with prepend + slide-in animation.
(function () {
  const FEED_EL = document.getElementById('trade-feed');
  if (!FEED_EL) return;

  // Hardcoded demo events for the static dashboard (no backend required).
  // Replace with: const sse = new EventSource('/api/v1/trades/stream');
  const demoEvents = [
    { tradeRef: 'EQU-20260603-0001', symbol: 'SAP.DE',  qty: 1000, price: 125.50, status: 'MATCHED' },
    { tradeRef: 'FX-20260603-0001',  symbol: 'EUR/USD', qty: 1_000_000, price: 1.0852, status: 'PENDING' },
    { tradeRef: 'EQU-20260603-0002', symbol: 'AAPL',    qty: 500,  price: 178.20, status: 'BREAK' },
  ];

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

  demoEvents.forEach((trade, index) => {
    setTimeout(() => prependTradeRow(trade), 500 * index);
  });
})();

import React, { useState } from 'react';

const STATUS_TONE = {
  MATCHED: 'success',
  PENDING: 'warning',
  UNMATCHED: 'danger',
  DISPUTED: 'danger',
};

function statusTone(status) {
  return STATUS_TONE[status] ?? 'neutral';
}

function CopyIcon() {
  return (
    <svg viewBox="0 0 16 16" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">
      <rect x="5" y="5" width="9" height="9" rx="1.5" />
      <path d="M3 10.5V3.5A1.5 1.5 0 0 1 4.5 2H10" />
    </svg>
  );
}

function CopyTradeRefButton({ tradeRef }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy(e) {
    // Stop propagation so this doesn't also fire the row's onClick (row
    // selection) — copying and selecting are separate intents.
    e.stopPropagation();
    try {
      await navigator.clipboard.writeText(tradeRef);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard API unavailable or permission denied — nothing to show.
    }
  }

  return (
    <span className="copy-trade-ref">
      <button
        type="button"
        className="copy-trade-ref__btn"
        onClick={handleCopy}
        aria-label={`Copy trade reference ${tradeRef}`}
        title="Copy trade reference"
      >
        <CopyIcon />
      </button>
      {copied && <span role="status" className="copy-trade-ref__toast">Copied!</span>}
    </span>
  );
}

function TradeRowImpl({ trade, onClick }) {
  // DataTable.Body wraps each row in a `display: grid` div (see global.css
  // .data-table__row) with one grid item per column — <tr>/<td> don't
  // participate in that grid (an orphaned table-row becomes a single grid
  // item, collapsing all its cells into one column), so these are plain
  // grid-item <div>s to match DataTable.Header's <button> grid items.
  const handleClick = () => onClick(trade.id);
  return (
    <>
      <div role="cell" onClick={handleClick} className="trade-ref-cell">
        {trade.tradeRef}
        <CopyTradeRefButton tradeRef={trade.tradeRef} />
      </div>
      <div role="cell" onClick={handleClick}>{trade.instrumentSymbol}</div>
      <div role="cell" onClick={handleClick}>{trade.quantity}</div>
      <div role="cell" onClick={handleClick}>{trade.price}</div>
      <div role="cell" onClick={handleClick}>
        <span className={`status-pill status-pill--${statusTone(trade.status)}`}>
          {trade.status}
        </span>
      </div>
    </>
  );
}

function areEqual(prev, next) {
  return prev.trade.id     === next.trade.id
      && prev.trade.status === next.trade.status
      && prev.trade.price  === next.trade.price
      && prev.onClick      === next.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
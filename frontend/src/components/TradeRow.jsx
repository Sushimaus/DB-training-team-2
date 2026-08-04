import React from 'react';

const STATUS_TONE = {
  MATCHED: 'success',
  PENDING: 'warning',
  UNMATCHED: 'danger',
  DISPUTED: 'danger',
};

function statusTone(status) {
  return STATUS_TONE[status] ?? 'neutral';
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
      <div role="cell" onClick={handleClick}>{trade.tradeRef}</div>
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
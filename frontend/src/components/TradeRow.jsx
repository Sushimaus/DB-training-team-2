import React from 'react';

function TradeRowImpl({ trade, onClick }) {
  return (
    <tr onClick={() => onClick(trade.id)}>
      <td>{trade.tradeRef}</td>
      <td>{trade.symbol}</td>
      <td>{trade.qty}</td>
      <td>{trade.price}</td>
      <td>
        <span className={`status-pill status-pill--${trade.status.toLowerCase()}`}>
          {trade.status}
        </span>
      </td>
    </tr>
  );
}

function areEqual(prev, next) {
  return prev.trade.id     === next.trade.id
      && prev.trade.status === next.trade.status
      && prev.trade.price  === next.trade.price
      && prev.onClick      === next.onClick;
}

export const TradeRow = React.memo(TradeRowImpl, areEqual);
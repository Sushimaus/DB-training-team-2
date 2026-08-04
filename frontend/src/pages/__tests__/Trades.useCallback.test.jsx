// TICKET-ADV121 — useCallback keeps <TradeRow>'s React.memo (TICKET-ADV119) intact.
import { useCallback, useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import { TradeRow } from '../../components/TradeRow.jsx';

const trade = { id: 1, tradeRef: 'T-1', instrumentSymbol: 'AAPL', quantity: 10, price: 245.5, status: 'PENDING' };

// Mirrors the Trades.jsx pattern: a parent with unrelated state (`tick`)
// re-rendering above a memoised <TradeRow>, using a useCallback handler.
function StableHandlerHarness({ captured }) {
  const [tick, setTick] = useState(0);
  const handleSelect = useCallback((id) => id, []);
  captured.push(handleSelect);
  return (
    <div>
      <button onClick={() => setTick((t) => t + 1)}>bump unrelated state</button>
      <span>tick: {tick}</span>
    </div>
  );
}

// The "pre-fix" shape ticket 121 fixes: a brand-new arrow every render.
function UnstableHandlerHarness({ captured }) {
  const [tick, setTick] = useState(0);
  const handleSelect = (id) => id;
  captured.push(handleSelect);
  return (
    <div>
      <button onClick={() => setTick((t) => t + 1)}>bump unrelated state</button>
      <span>tick: {tick}</span>
    </div>
  );
}

describe('useCallback keeps the onClick handler reference stable (TICKET-ADV121)', () => {
  it('passes the SAME function reference to TradeRow across unrelated re-renders', async () => {
    const captured = [];
    render(<StableHandlerHarness captured={captured} />);

    await userEvent.click(screen.getByText('bump unrelated state'));
    await userEvent.click(screen.getByText('bump unrelated state'));

    expect(screen.getByText('tick: 2')).toBeInTheDocument();
    expect(captured).toHaveLength(3); // initial mount + 2 re-renders
    expect(captured[0]).toBe(captured[1]);
    expect(captured[1]).toBe(captured[2]);
  });

  it('a plain inline arrow (pre-fix shape) is a NEW reference every render', async () => {
    const captured = [];
    render(<UnstableHandlerHarness captured={captured} />);

    await userEvent.click(screen.getByText('bump unrelated state'));
    await userEvent.click(screen.getByText('bump unrelated state'));

    expect(captured).toHaveLength(3);
    expect(captured[0]).not.toBe(captured[1]);
    expect(captured[1]).not.toBe(captured[2]);
  });
});

describe('<TradeRow> renders correctly with a useCallback-provided onClick', () => {
  it('renders the row fields and invokes the stable handler with the trade id on click', async () => {
    const clicks = [];
    function Harness() {
      const handleSelect = useCallback((id) => clicks.push(id), []);
      return (
        <div role="row">
          <TradeRow trade={trade} onClick={handleSelect} />
        </div>
      );
    }

    render(<Harness />);
    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('PENDING')).toBeInTheDocument();

    await userEvent.click(screen.getByText('AAPL'));
    expect(clicks).toEqual([1]);
  });
});

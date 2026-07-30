// TICKET-ADV114 — Compound <DataTable> sub-component behavior.
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import DataTable from '../DataTable.jsx';

const columns = [
  { key: 'symbol', label: 'Symbol' },
  { key: 'qty', label: 'Qty' },
];

describe('<DataTable.Header>', () => {
  it('marks the active sort column and calls onSortChange with its key', async () => {
    const onSortChange = vi.fn();
    render(
      <DataTable sort="symbol" onSortChange={onSortChange}>
        <DataTable.Header columns={columns} />
      </DataTable>
    );

    const symbolBtn = screen.getByText('Symbol');
    const qtyBtn = screen.getByText('Qty');
    expect(symbolBtn.className).toContain('data-table__th--active');
    expect(qtyBtn.className).toContain('data-table__th--idle');

    await userEvent.click(qtyBtn);
    expect(onSortChange).toHaveBeenCalledWith('qty');
  });

  it('does not throw when no onSortChange is supplied', async () => {
    render(
      <DataTable>
        <DataTable.Header columns={columns} />
      </DataTable>
    );
    await userEvent.click(screen.getByText('Symbol'));
  });
});

describe('<DataTable.Body>', () => {
  it('renders one row per item via the render prop', () => {
    render(
      <DataTable>
        <DataTable.Body
          rows={[{ id: 1, symbol: 'AAPL' }, { id: 2, symbol: 'MSFT' }]}
          render={(row) => <span>{row.symbol}</span>}
        />
      </DataTable>
    );

    expect(screen.getByText('AAPL')).toBeInTheDocument();
    expect(screen.getByText('MSFT')).toBeInTheDocument();
  });

  it('renders nothing when rows is empty', () => {
    const { container } = render(
      <DataTable>
        <DataTable.Body rows={[]} render={(row) => <span>{row.symbol}</span>} />
      </DataTable>
    );
    expect(container.querySelector('.data-table__row')).toBeNull();
  });
});

describe('<DataTable.Pagination>', () => {
  it('disables Prev on the first page and Next on the last page', () => {
    render(
      <DataTable>
        <DataTable.Pagination page={0} totalPages={3} onChange={() => {}} />
      </DataTable>
    );
    expect(screen.getByText('‹')).toBeDisabled();
    expect(screen.getByText('›')).not.toBeDisabled();
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });

  it('calls onChange with page - 1 / page + 1', async () => {
    const onChange = vi.fn();
    render(
      <DataTable>
        <DataTable.Pagination page={1} totalPages={3} onChange={onChange} />
      </DataTable>
    );
    await userEvent.click(screen.getByText('‹'));
    expect(onChange).toHaveBeenCalledWith(0);
    await userEvent.click(screen.getByText('›'));
    expect(onChange).toHaveBeenCalledWith(2);
  });
});

describe('<DataTable> composition', () => {
  it('renders correctly with Pagination omitted', () => {
    render(
      <DataTable>
        <DataTable.Header columns={columns} />
        <DataTable.Body rows={[{ id: 1, symbol: 'AAPL' }]} render={(r) => <span>{r.symbol}</span>} />
      </DataTable>
    );
    expect(screen.getByText('Symbol')).toBeInTheDocument();
    expect(screen.getByText('AAPL')).toBeInTheDocument();
  });
});

// TICKET-ADV114 — Compound DataTable.
// TICKET-ADV117 — useDebouncedSearch.
import React, { useState, useEffect, useCallback } from 'react';
import { withAuth } from '@components/withAuth.jsx';
import DataTable from '@components/DataTable.jsx';
import { useDebouncedSearch } from '@hooks/useDebouncedSearch.js';
import { api } from '@services/apiService.js';
import { TradeRow } from '@components/TradeRow.jsx';

function Trades() {
  const [search, setSearch] = useState('');
  const debounced = useDebouncedSearch(search, 300);
  const [page, setPage] = useState(0);
  const [data, setData] = useState({ items: [], totalPages: 0 });

  // Reference-stable across renders — onClick prop on <TradeRow> won't
  // change identity on unrelated re-renders, so its React.memo holds.
  // No row-selection UI consumes the id yet; this is a placeholder handler.
  const handleSelect = useCallback((_id) => {}, []);

  // TODO(TICKET-ADV114 + ADV117): useEffect that:
  //   - builds a query string from `page` and `debounced` (status filter)
  //   - calls api.listTrades(params) and stores the response in `data`
  //   - re-runs whenever `page` or `debounced` changes
  //   - degrades gracefully on error (set empty page).

  useEffect(() => {
    let cancelled = false;

    async function loadTrades() {
      try {
        const params = { page };
        if (debounced) params.status = debounced;
        const response = await api.listTrades(params);
        if (!cancelled) {
          setData({
            items: response.content ?? response.items ?? [],
            totalPages: response.totalPages ?? 1,
          });
        }
      } catch (err) {
        // eslint-disable-next-line no-console
        console.error('Failed to load trades', err);
        if (!cancelled) setData({ items: [], totalPages: 0 });
      }
    }
    
    loadTrades();
    return () => { cancelled = true; };
  }, [page, debounced]);

  return (
    <section>
      <h2>Trades</h2>
      <input
        aria-label="Filter by status"
        placeholder="status filter (PENDING/MATCHED/…)"
        value={search}
        onChange={(e) => setSearch(e.target.value.toUpperCase())}
      />
      <DataTable>
        <DataTable.Header columns={[
          { key: 'tradeRef', label: 'Ref' },
          { key: 'symbol',   label: 'Symbol' },
          { key: 'qty',      label: 'Qty' },
          { key: 'price',    label: 'Price' },
          { key: 'status',   label: 'Status' },
        ]} />
        <DataTable.Body
          rows={data.items}
          render={(row) => (
            <TradeRow trade={row} onClick={handleSelect} />
          )}
        />
        <DataTable.Pagination
          page={page}
          totalPages={Math.max(1, data.totalPages)}
          onChange={setPage}
        />
      </DataTable>
    </section>
  );
}

export default withAuth(Trades);

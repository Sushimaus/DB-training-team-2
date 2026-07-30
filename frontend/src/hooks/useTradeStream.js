// TICKET-ADV116 — useTradeStream() — SSE subscription returning live trades.
import { useEffect, useState } from 'react';

export function useTradeStream(url = '/api/v1/trades/stream') {
  // TODO(TICKET-ADV116): subscribe to the SSE endpoint with `new EventSource(url)`.
  //                     - onopen   -> setConnected(true)
  //                     - onmessage(e) -> JSON.parse(e.data), prepend to `trades`,
  //                       cap the list at ~200 items so the UI doesn't blow up.
  //                     - onerror  -> setConnected(false)
  //                     Close the EventSource in the effect cleanup.
  const [trades, setTrades] = useState([]);
  const [isConnected, setConnected] = useState(false);

  useEffect(() => {
    const sse = new EventSource(url);

    sse.onopen = () => setConnected(true);

    sse.onmessage = (e) => {
      try {
        const t = JSON.parse(e.data);
        setTrades((prev) => [t, ...prev].slice(0, 200));
      } catch {
      }
    };

    sse.onerror = () => setConnected(false);

    return () => sse.close();
  }, [url]);

  return { trades, isConnected };
}
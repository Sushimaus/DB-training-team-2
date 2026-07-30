// TICKET-ADV115 — useWebSocket(url) with auto-reconnect (exp backoff up to 5 tries).
import { useEffect, useRef, useState } from 'react';

export function useWebSocket(url, { reconnect = true, maxRetries = 5 } = {}) {
  const [data, setData] = useState(null);
  const [status, setStatus] = useState('connecting');
  const wsRef = useRef(null);
  const retriesRef = useRef(0);
  const timerRef = useRef(null);

  useEffect(() => {
    let cancelled = false;

    function connect() {
      const ws = new WebSocket(url);
      wsRef.current = ws;
      setStatus('connecting');

      ws.onopen = () => {
        if (cancelled) return;
        setStatus('open');
        retriesRef.current = 0;
      };

      ws.onmessage = (event) => {
        if (cancelled) return;
        try {
          setData(JSON.parse(event.data));
        } catch {
          setData(event.data);
        }
      };

      ws.onerror = () => {
        if (cancelled) return;
        setStatus('error');
      };

      ws.onclose = () => {
        if (cancelled) return;
        setStatus('closed');
        if (reconnect && retriesRef.current < maxRetries) {
          const delay = Math.min(30000, 500 * 2 ** retriesRef.current);
          retriesRef.current += 1;
          timerRef.current = setTimeout(connect, delay);
        }
      };
    }

    connect();

    return () => {
      cancelled = true;
      clearTimeout(timerRef.current);
      wsRef.current && wsRef.current.close();
    };
  }, [url, reconnect, maxRetries]);

  const send = (payload) => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
  };

  return { data, status, send };
}

import { useEffect, useRef } from 'react';

export function useInfiniteScroll(loadMore, { rootMargin = '200px' } = {}) {
  const sentinelRef = useRef(null);
  const loadMoreRef = useRef(loadMore);

  useEffect(() => {
    loadMoreRef.current = loadMore;
  }, [loadMore]);

  useEffect(() => {
    const node = sentinelRef.current;
    if (!node) {
      return undefined;
    }

    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) {
        loadMoreRef.current();
      }
    }, { rootMargin });

    observer.observe(node);

    return () => observer.disconnect();
  }, [rootMargin]);

  return sentinelRef;
}

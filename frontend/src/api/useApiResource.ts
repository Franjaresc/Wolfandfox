import { useEffect, useEffectEvent, useState } from 'react';
import type { ApiFailure, ApiResult } from './problems';

export type Resource<T> =
  { status: 'loading' } | { status: 'ready'; data: T } | { status: 'failed'; failure: ApiFailure };

/**
 * Loads data for a key, reloading when the key changes or on retry. A response is matched to the
 * request that caused it, so a slow earlier response can never replace a newer one on screen.
 */
export function useApiResource<T>(
  key: string,
  load: () => Promise<ApiResult<T>>,
): { resource: Resource<T>; retry: () => void } {
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<{ request: string; result: ApiResult<T> } | null>(null);
  const request = `${key}#${String(attempt)}`;
  const runLoad = useEffectEvent(load);

  useEffect(() => {
    let current = true;
    void runLoad().then((result) => {
      if (current) {
        setSettled({ request, result });
      }
    });
    return () => {
      current = false;
    };
  }, [request]);

  const retry = () => {
    setAttempt((value) => value + 1);
  };

  if (settled?.request !== request) {
    return { resource: { status: 'loading' }, retry };
  }
  return {
    resource: settled.result.ok
      ? { status: 'ready', data: settled.result.data }
      : { status: 'failed', failure: settled.result.failure },
    retry,
  };
}

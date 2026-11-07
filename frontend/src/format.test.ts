import { expect, test } from 'vitest';
import { formatDate, formatDateTime } from './format';

// The test run uses TZ=UTC (vite.config.ts), so instants format the same on every machine.

test('a calendar date uses the same style as dates with a time', () => {
  expect(formatDate('2026-01-15')).toBe('15 Jan 2026');
  expect(formatDateTime('2026-01-15T18:30:00Z')).toBe('15 Jan 2026, 18:30');
});

test('a calendar date stays on its day at the edges of the year', () => {
  expect(formatDate('2026-01-01')).toBe('1 Jan 2026');
  expect(formatDate('2026-12-31')).toBe('31 Dec 2026');
});

import { expect, test } from 'vitest';
import { isValidDate, liveTotal, type FormValues } from './validation';

const EMPTY: FormValues = {
  routeCode: '',
  closureDate: '',
  vehicleCode: '',
  driverCode: '',
  packagesDelivered: '',
  packagesFailed: '',
  packagesReturned: '',
  kmDriven: '',
  notes: '',
};

test.each(['2026-01-15', '2024-02-29'])('%s is a valid date', (value) => {
  expect(isValidDate(value)).toBe(true);
});

test.each(['2026-02-30', '2025-02-29', '2026-13-01', '15/01/2026', '2026-1-5', ''])(
  '%s is not a valid date',
  (value) => {
    expect(isValidDate(value)).toBe(false);
  },
);

test('the total stays exact at the largest counts the contract allows', () => {
  const largest = '2147483647';

  expect(
    liveTotal({
      ...EMPTY,
      packagesDelivered: largest,
      packagesFailed: largest,
      packagesReturned: largest,
    }),
  ).toBe(6442450941);
});

test('the total is unknown while a count is above the contract limit', () => {
  expect(liveTotal({ ...EMPTY, packagesDelivered: '2147483648' })).toBeNull();
});

import { expect, test } from 'vitest';
import { isValidDate, liveTotal, localToday, validateField, type FormValues } from './validation';

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

test('today is the local calendar date, late in the evening too', () => {
  expect(localToday(new Date(2026, 0, 15, 0, 0))).toBe('2026-01-15');
  expect(localToday(new Date(2026, 0, 15, 23, 59))).toBe('2026-01-15');
  expect(localToday(new Date(2026, 11, 31, 12, 0))).toBe('2026-12-31');
});

test.each([
  ['2026-01-14', undefined],
  ['2026-01-15', undefined],
  ['2026-01-16', 'future'],
  ['2027-01-01', 'future'],
])('closure date %s with today 2026-01-15 gives %s', (value, expected) => {
  expect(validateField('closureDate', value, '2026-01-15')).toBe(expected);
});

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

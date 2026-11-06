import { HttpResponse } from 'msw';
import type { components } from '../api/schema.gen';

type ProblemCode = components['schemas']['ProblemCode'];
type RouteClosure = components['schemas']['RouteClosure'];

export const ROUTE_CLOSURES_URL = '/api/v1/route-closures';

/** Text a real backend must never get onto the screen. Tests assert that it stays hidden. */
export const BACKEND_TEXT = 'SQL [insert into route_closure] at org.hibernate.Secret';

export function closure(overrides: Partial<RouteClosure> = {}): RouteClosure {
  return {
    id: '00000000-0000-4000-8000-000000000001',
    routeCode: 'RT-0001',
    closureDate: '2026-01-15',
    vehicleCode: 'VAN-0042',
    driverCode: 'DRV-0007',
    packagesDelivered: 42,
    packagesFailed: 3,
    packagesReturned: 1,
    totalPackages: 46,
    kmDriven: 87,
    closedBy: 'demo.supervisor',
    closedAt: '2026-01-15T18:30:00Z',
    ...overrides,
  };
}

export function problem(code: ProblemCode, status: number, errors?: { field: string }[]) {
  return HttpResponse.json(
    {
      type: 'urn:wolfandfox:problem:test',
      code,
      title: BACKEND_TEXT,
      status,
      detail: BACKEND_TEXT,
      ...(errors === undefined
        ? {}
        : { errors: errors.map((error) => ({ ...error, message: BACKEND_TEXT })) }),
    },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

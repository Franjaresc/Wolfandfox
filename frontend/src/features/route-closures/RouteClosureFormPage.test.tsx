import { render, screen, waitFor } from '@testing-library/react';
import userEvent, { type UserEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { messages } from '../../messages';
import { BACKEND_TEXT, ROUTE_CLOSURES_URL, closure, problem } from '../../test/fixtures';
import { server } from '../../test/server';
import { RouteClosureFormPage } from './RouteClosureFormPage';

const labels = messages.closure.fields;
const fieldErrors = messages.closure.errors;

type TextField = keyof typeof VALID;

const VALID = {
  routeCode: 'RT-0001',
  closureDate: '2026-01-15',
  vehicleCode: 'VAN-0042',
  driverCode: 'DRV-0007',
  packagesDelivered: '42',
  packagesFailed: '3',
  packagesReturned: '1',
  kmDriven: '87',
};

function setup() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <RouteClosureFormPage />
    </MemoryRouter>,
  );
  return user;
}

/** Found by label, as a person would, because a date input has no ARIA role to query. */
function control(field: TextField | 'notes') {
  return screen.getByLabelText(new RegExp(`^${labels[field]}`));
}

async function fill(user: UserEvent, overrides: Partial<Record<TextField | 'notes', string>> = {}) {
  const values: Partial<Record<TextField | 'notes', string>> = { ...VALID, ...overrides };
  for (const [field, value] of Object.entries(values)) {
    if (value !== '') {
      await user.type(control(field as TextField), value);
    }
  }
}

async function submit(user: UserEvent) {
  await user.click(screen.getByRole('button', { name: messages.closure.form.submit }));
}

function expectFieldError(field: TextField | 'notes', message: string) {
  const element = control(field);
  expect(element).toHaveAttribute('aria-invalid', 'true');
  expect(element).toHaveAccessibleDescription(expect.stringContaining(message));
}

function respondWith(response: () => Response) {
  server.use(http.post(ROUTE_CLOSURES_URL, response));
}

function expectNoBackendText() {
  expect(document.body).not.toHaveTextContent('SQL');
  expect(document.body).not.toHaveTextContent('hibernate');
  expect(document.body).not.toHaveTextContent(BACKEND_TEXT);
}

describe('validation', () => {
  test.each([
    ['routeCode', fieldErrors.routeCode.required],
    ['closureDate', fieldErrors.closureDate.required],
    ['vehicleCode', fieldErrors.vehicleCode.required],
    ['driverCode', fieldErrors.driverCode.required],
    ['packagesDelivered', fieldErrors.packagesDelivered.required],
    ['packagesFailed', fieldErrors.packagesFailed.required],
    ['packagesReturned', fieldErrors.packagesReturned.required],
    ['kmDriven', fieldErrors.kmDriven.required],
  ] as const)('%s is required', async (field, message) => {
    const user = setup();
    await fill(user, { [field]: '' });

    await submit(user);

    expectFieldError(field, message);
  });

  describe.each(['routeCode', 'vehicleCode', 'driverCode'] as const)('%s format', (field) => {
    test.each([
      ['lowercase letters', 'rt-0001'],
      ['a single character', 'R'],
      ['a leading hyphen', '-RT1'],
      ['a space', 'RT 0001'],
      ['more than 20 characters', 'A'.repeat(21)],
    ])('rejects %s', async (_name, value) => {
      const user = setup();
      await fill(user, { [field]: value });

      await submit(user);

      expectFieldError(field, fieldErrors[field].format);
    });

    test.each([
      ['2 characters', 'A1'],
      ['20 characters', 'A'.repeat(20)],
      ['hyphens after the first character', 'RT-00-1'],
    ])('accepts %s', async (_name, value) => {
      respondWith(() => HttpResponse.json(closure(), { status: 201 }));
      const user = setup();
      await fill(user, { [field]: value });

      await submit(user);

      expect(
        await screen.findByRole('heading', { name: messages.closure.form.confirmation }),
      ).toBeInTheDocument();
    });
  });

  describe.each(['packagesDelivered', 'packagesFailed', 'packagesReturned', 'kmDriven'] as const)(
    '%s number',
    (field) => {
      test.each([
        ['a negative number', '-1'],
        ['a decimal', '1.5'],
        ['letters', 'abc'],
        ['a sign', '+1'],
      ])('rejects %s', async (_name, value) => {
        const user = setup();
        await fill(user, { [field]: value });

        await submit(user);

        expectFieldError(field, fieldErrors[field].integer);
      });

      test('rejects a number the contract cannot hold', async () => {
        const user = setup();
        await fill(user, { [field]: '2147483648' });

        await submit(user);

        expectFieldError(field, fieldErrors[field].tooLarge);
      });

      test.each(['0', '2147483647'])('accepts %s', async (value) => {
        respondWith(() => HttpResponse.json(closure(), { status: 201 }));
        const user = setup();
        await fill(user, { [field]: value });

        await submit(user);

        expect(
          await screen.findByRole('heading', { name: messages.closure.form.confirmation }),
        ).toBeInTheDocument();
      });
    },
  );

  test('rejects notes over 1000 characters', async () => {
    const user = setup();
    await fill(user);
    await user.click(control('notes'));
    await user.paste('n'.repeat(1001));

    await submit(user);

    expectFieldError('notes', fieldErrors.notes.tooLong);
  });

  test('accepts notes of exactly 1000 characters and leaves them optional', async () => {
    let body: unknown;
    server.use(
      http.post(ROUTE_CLOSURES_URL, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();
    await fill(user);
    await user.click(control('notes'));
    await user.paste('n'.repeat(1000));

    await submit(user);

    await screen.findByRole('heading', { name: messages.closure.form.confirmation });
    expect((body as { notes: string }).notes).toHaveLength(1000);
  });

  test('does not call the backend while the form is invalid', async () => {
    let requests = 0;
    server.use(
      http.post(ROUTE_CLOSURES_URL, () => {
        requests += 1;
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();

    await submit(user);

    expect(control('routeCode')).toHaveFocus();
    expect(requests).toBe(0);
  });

  test('moves focus to the first invalid field after a failed submit', async () => {
    const user = setup();
    await fill(user, { routeCode: '', driverCode: '' });

    await submit(user);

    expect(control('routeCode')).toHaveFocus();
    expectFieldError('driverCode', fieldErrors.driverCode.required);
  });

  test('clears a field error once the value becomes valid', async () => {
    const user = setup();
    await submit(user);
    expectFieldError('routeCode', fieldErrors.routeCode.required);

    await user.type(control('routeCode'), 'RT-0001');

    expect(control('routeCode')).not.toHaveAttribute('aria-invalid');
    expect(screen.queryByText(fieldErrors.routeCode.required)).not.toBeInTheDocument();
  });
});

describe('closure date', () => {
  beforeEach(() => {
    // Only Date is faked, so user-event's timers keep working.
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 0, 15, 23, 30));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  test('offers today, in local time, as the latest date', () => {
    setup();

    expect(control('closureDate')).toHaveAttribute('max', '2026-01-15');
  });

  test('rejects tomorrow with a clear message, without calling the backend', async () => {
    let requests = 0;
    server.use(
      http.post(ROUTE_CLOSURES_URL, () => {
        requests += 1;
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();
    await fill(user, { closureDate: '2026-01-16' });

    await submit(user);

    expectFieldError('closureDate', fieldErrors.closureDate.future);
    expect(control('closureDate')).toHaveFocus();
    expect(requests).toBe(0);
  });

  test('accepts today', async () => {
    respondWith(() => HttpResponse.json(closure(), { status: 201 }));
    const user = setup();
    await fill(user, { closureDate: '2026-01-15' });

    await submit(user);

    expect(
      await screen.findByRole('heading', { name: messages.closure.form.confirmation }),
    ).toBeInTheDocument();
  });

  test('shows the date message when the server, whose clock decides, rejects the date', async () => {
    respondWith(() => problem('VALIDATION_FAILED', 400, [{ field: 'closureDate' }]));
    const user = setup();
    await fill(user, { closureDate: '2026-01-15' });

    await submit(user);

    await waitFor(() => {
      expect(control('closureDate')).toHaveFocus();
    });
    expectFieldError('closureDate', fieldErrors.closureDate.rejected);
    expectNoBackendText();
  });
});

describe('live total', () => {
  function total() {
    return screen.getByRole('status', { name: labels.totalPackages });
  }

  test('adds up the counts as they are typed', async () => {
    const user = setup();
    expect(total()).toHaveTextContent('0');

    await user.type(control('packagesDelivered'), '42');
    expect(total()).toHaveTextContent('42');

    await user.type(control('packagesFailed'), '3');
    expect(total()).toHaveTextContent('45');

    await user.type(control('packagesReturned'), '1');
    expect(total()).toHaveTextContent('46');
  });

  test('shows no number while a count is invalid', async () => {
    const user = setup();
    await user.type(control('packagesDelivered'), '4x');

    expect(total()).toHaveTextContent(messages.closure.form.totalUnknown);
  });

  test('is read-only', () => {
    setup();

    expect(screen.queryByRole('textbox', { name: labels.totalPackages })).not.toBeInTheDocument();
  });
});

describe('submitting', () => {
  test('sends the request and shows a confirmation with the created closure', async () => {
    let body: unknown;
    server.use(
      http.post(ROUTE_CLOSURES_URL, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();
    await fill(user, { notes: 'Fictional note' });

    await submit(user);

    const confirmation = await screen.findByRole('heading', {
      name: messages.closure.form.confirmation,
    });
    expect(confirmation).toHaveFocus();
    expect(body).toEqual({
      routeCode: 'RT-0001',
      closureDate: '2026-01-15',
      vehicleCode: 'VAN-0042',
      driverCode: 'DRV-0007',
      packagesDelivered: 42,
      packagesFailed: 3,
      packagesReturned: 1,
      kmDriven: 87,
      notes: 'Fictional note',
    });
    expect(screen.getByText('RT-0001')).toBeInTheDocument();
    expect(screen.getByText('46')).toBeInTheDocument();
    expect(screen.getByText('demo.supervisor')).toBeInTheDocument();
  });

  test('omits empty notes from the request', async () => {
    let body: unknown;
    server.use(
      http.post(ROUTE_CLOSURES_URL, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();
    await fill(user);

    await submit(user);

    await screen.findByRole('heading', { name: messages.closure.form.confirmation });
    expect(body).not.toHaveProperty('notes');
  });

  test('can start another closure after a success', async () => {
    respondWith(() => HttpResponse.json(closure(), { status: 201 }));
    const user = setup();
    await fill(user);
    await submit(user);

    await user.click(
      await screen.findByRole('button', { name: messages.closure.form.closeAnother }),
    );

    expect(control('routeCode')).toHaveValue('');
  });

  test('prevents a second submit while one is in flight', async () => {
    let requests = 0;
    let release: () => void = () => undefined;
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    server.use(
      http.post(ROUTE_CLOSURES_URL, async () => {
        requests += 1;
        await held;
        return HttpResponse.json(closure(), { status: 201 });
      }),
    );
    const user = setup();
    await fill(user);
    await submit(user);

    const busy = await screen.findByRole('button', { name: messages.closure.form.submitting });
    expect(busy).toBeDisabled();
    await user.keyboard('{Enter}');
    release();

    await screen.findByRole('heading', { name: messages.closure.form.confirmation });
    expect(requests).toBe(1);
  });
});

describe('server errors', () => {
  test('400 shows our message next to each named field and focuses the first', async () => {
    respondWith(() =>
      problem('VALIDATION_FAILED', 400, [{ field: 'kmDriven' }, { field: 'vehicleCode' }]),
    );
    const user = setup();
    await fill(user);

    await submit(user);

    await waitFor(() => {
      expect(control('vehicleCode')).toHaveFocus();
    });
    expectFieldError('vehicleCode', fieldErrors.vehicleCode.rejected);
    expectFieldError('kmDriven', fieldErrors.kmDriven.rejected);
    expectNoBackendText();
  });

  test('400 without a usable field shows a general message and focuses it', async () => {
    respondWith(() => problem('VALIDATION_FAILED', 400, [{ field: 'somethingElse' }]));
    const user = setup();
    await fill(user);

    await submit(user);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(messages.errors.problems.VALIDATION_FAILED);
    expect(alert).toHaveFocus();
    expectNoBackendText();
  });

  test('409 shows the duplicate message on the form and keeps the values', async () => {
    respondWith(() => problem('ROUTE_CLOSURE_DUPLICATE', 409));
    const user = setup();
    await fill(user);

    await submit(user);

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(messages.errors.problems.ROUTE_CLOSURE_DUPLICATE);
    expect(alert).toHaveFocus();
    expect(control('routeCode')).toHaveValue('RT-0001');
    expectNoBackendText();
  });

  test('500 shows a non-technical message and allows resubmitting', async () => {
    respondWith(() => problem('INTERNAL_ERROR', 500));
    const user = setup();
    await fill(user);

    await submit(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      messages.errors.problems.INTERNAL_ERROR,
    );
    expectNoBackendText();

    respondWith(() => HttpResponse.json(closure(), { status: 201 }));
    await submit(user);

    expect(
      await screen.findByRole('heading', { name: messages.closure.form.confirmation }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('network failure shows a connection message and keeps the values', async () => {
    respondWith(() => HttpResponse.error());
    const user = setup();
    await fill(user);

    await submit(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(messages.errors.network);
    expect(control('kmDriven')).toHaveValue('87');
  });

  test('a response that is not a problem, such as a proxy error, shows unavailable', async () => {
    respondWith(() => new HttpResponse(null, { status: 500 }));
    const user = setup();
    await fill(user);

    await submit(user);

    expect(await screen.findByRole('alert')).toHaveTextContent(messages.errors.unavailable);
  });
});

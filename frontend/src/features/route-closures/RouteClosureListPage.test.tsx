import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, test } from 'vitest';
import { messages } from '../../messages';
import { BACKEND_TEXT, ROUTE_CLOSURES_URL, closure, problem } from '../../test/fixtures';
import { renderApp } from '../../test/renderApp';
import { server } from '../../test/server';

const text = messages.closure.list;
const LIST = '/closures';

function page(
  items: ReturnType<typeof closure>[],
  overrides: Partial<{
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  }> = {},
) {
  return HttpResponse.json({
    items,
    page: 0,
    size: 20,
    totalElements: items.length,
    totalPages: items.length === 0 ? 0 : 1,
    ...overrides,
  });
}

/** Answers every list request with the given response and records what was asked for. */
function respondWith(response: () => Response) {
  const requests: URLSearchParams[] = [];
  server.use(
    http.get(ROUTE_CLOSURES_URL, ({ request }) => {
      requests.push(new URL(request.url).searchParams);
      return response();
    }),
  );
  return requests;
}

function lastRequest(requests: URLSearchParams[]) {
  const request = requests.at(-1);
  if (request === undefined) {
    throw new Error('No request was sent');
  }
  return Object.fromEntries(request.entries());
}

describe('states', () => {
  test('shows a loading state until the response arrives', async () => {
    let release: () => void = () => undefined;
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    server.use(
      http.get(ROUTE_CLOSURES_URL, async () => {
        await held;
        return page([closure()]);
      }),
    );

    renderApp(LIST);

    expect(screen.getByRole('status')).toHaveTextContent(text.loading);
    release();
    expect(await screen.findByRole('link', { name: 'RT-0001' })).toBeInTheDocument();
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  test('shows an empty state with a way to close a route', async () => {
    respondWith(() => page([]));

    renderApp(LIST);

    const empty = await screen.findByText(text.empty, { exact: false });
    expect(within(empty).getByRole('link', { name: text.emptyAction })).toHaveAttribute(
      'href',
      '/closures/new',
    );
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  test('says when filters match nothing', async () => {
    respondWith(() => page([]));

    renderApp(`${LIST}?routeCode=RT-0009`);

    expect(await screen.findByText(text.noMatches)).toBeInTheDocument();
  });

  test.each([
    ['500', () => problem('INTERNAL_ERROR', 500), messages.errors.problems.INTERNAL_ERROR],
    ['a network failure', () => HttpResponse.error(), messages.errors.network],
    [
      'a response that is not a problem',
      () => new HttpResponse(null, { status: 502 }),
      messages.errors.unavailable,
    ],
  ])('shows a clear message on %s and recovers on retry', async (_name, failure, message) => {
    respondWith(failure);
    const user = renderApp(LIST);

    expect(await screen.findByRole('alert')).toHaveTextContent(message);
    expect(document.body).not.toHaveTextContent(BACKEND_TEXT);

    respondWith(() => page([closure()]));
    await user.click(screen.getByRole('button', { name: messages.common.retry }));

    expect(await screen.findByRole('link', { name: 'RT-0001' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('results', () => {
  test('shows the closures in the order the backend returns them', async () => {
    respondWith(() =>
      page([
        closure({ id: '00000000-0000-4000-8000-000000000003', routeCode: 'RT-0003' }),
        closure({ id: '00000000-0000-4000-8000-000000000001', routeCode: 'RT-0001' }),
        closure({ id: '00000000-0000-4000-8000-000000000002', routeCode: 'RT-0002' }),
      ]),
    );

    renderApp(LIST);

    await screen.findByRole('table');
    const rows = within(screen.getByRole('table')).getAllByRole('link');
    expect(rows.map((row) => row.textContent)).toEqual(['RT-0003', 'RT-0001', 'RT-0002']);
  });

  test('shows the values of a closure and links to its detail', async () => {
    respondWith(() => page([closure()]));

    renderApp(LIST);

    const link = await screen.findByRole('link', { name: 'RT-0001' });
    expect(link).toHaveAttribute('href', `/closures/${closure().id}`);
    const row = within(screen.getByRole('table')).getAllByRole('row')[1];
    expect(row).toHaveTextContent('2026-01-15');
    expect(row).toHaveTextContent('VAN-0042');
    expect(row).toHaveTextContent('DRV-0007');
    expect(row).toHaveTextContent('46');
    expect(row).toHaveTextContent('87');
  });
});

describe('pagination', () => {
  test('asks for the first page at the default size', async () => {
    const requests = respondWith(() => page([closure()]));

    renderApp(LIST);

    await screen.findByRole('table');
    expect(lastRequest(requests)).toEqual({ page: '0', size: '20' });
  });

  test('moves between pages and disables what is not available', async () => {
    const requests = respondWith(() => page([closure()], { totalElements: 41, totalPages: 3 }));
    const user = renderApp(LIST);

    await screen.findByText(text.pageOf(1, 3));
    expect(screen.getByRole('button', { name: text.previous })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: text.next }));
    await screen.findByText(text.pageOf(2, 3));
    expect(lastRequest(requests)).toEqual({ page: '1', size: '20' });

    await user.click(screen.getByRole('button', { name: text.next }));
    await screen.findByText(text.pageOf(3, 3));
    expect(screen.getByRole('button', { name: text.next })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: text.previous }));
    await screen.findByText(text.pageOf(2, 3));
    expect(lastRequest(requests)).toEqual({ page: '1', size: '20' });
  });

  test('keeps focus on the results when the page changes', async () => {
    respondWith(() => page([closure()], { totalElements: 41, totalPages: 3 }));
    const user = renderApp(LIST);
    await screen.findByText(text.pageOf(1, 3));

    await user.click(screen.getByRole('button', { name: text.next }));

    await screen.findByText(text.pageOf(2, 3));
    expect(document.activeElement).not.toBe(document.body);
    expect(document.activeElement).toContainElement(screen.getByRole('table'));
  });

  test('offers a way back from a page beyond the last', async () => {
    const requests = respondWith(() => page([], { totalElements: 5, totalPages: 1 }));
    const user = renderApp(`${LIST}?page=7`);

    await screen.findByText(text.noMatches);
    await user.click(screen.getByRole('button', { name: text.previous }));

    await screen.findAllByText(text.noMatches);
    expect(lastRequest(requests)).toEqual({ page: '6', size: '20' });
  });

  test('changing the page size asks for it and returns to the first page', async () => {
    const requests = respondWith(() => page([closure()], { totalElements: 41, totalPages: 3 }));
    const user = renderApp(`${LIST}?page=2`);
    await screen.findByRole('table');

    await user.selectOptions(screen.getByLabelText(text.pageSize), '50');

    await screen.findByRole('table');
    expect(lastRequest(requests)).toEqual({ page: '0', size: '50' });
  });

  test('offers no page size above what the contract allows', () => {
    respondWith(() => page([]));
    renderApp(LIST);

    const options = within(screen.getByLabelText(text.pageSize))
      .getAllByRole('option')
      .map((option) => Number(option.textContent));

    expect(Math.max(...options)).toBe(100);
  });

  test.each([
    ['a size above the limit', '?size=1000', { page: '0', size: '20' }],
    ['a negative page', '?page=-1', { page: '0', size: '20' }],
    ['a page that is not a number', '?page=abc', { page: '0', size: '20' }],
    ['a decimal page', '?page=1.5', { page: '0', size: '20' }],
    ['an invalid route code', '?routeCode=rt-1', { page: '0', size: '20' }],
    ['an invalid date', '?date=2026-02-30', { page: '0', size: '20' }],
  ])('falls back to defaults for %s in the URL', async (_name, query, expected) => {
    const requests = respondWith(() => page([closure()]));

    renderApp(`${LIST}${query}`);

    await screen.findByRole('table');
    expect(lastRequest(requests)).toEqual(expected);
  });
});

describe('filters', () => {
  const dateInput = () => screen.getByLabelText(messages.closure.fields.closureDate);
  const codeInput = () => screen.getByLabelText(messages.closure.fields.routeCode);
  const apply = () => screen.getByRole('button', { name: text.apply });

  test('filters by date and route code and returns to the first page', async () => {
    const requests = respondWith(() => page([closure()], { totalElements: 41, totalPages: 3 }));
    const user = renderApp(`${LIST}?page=2`);
    await screen.findByRole('table');

    await user.type(dateInput(), '2026-01-15');
    await user.type(codeInput(), 'RT-0001');
    await user.click(apply());

    await screen.findByRole('table');
    expect(lastRequest(requests)).toEqual({
      date: '2026-01-15',
      routeCode: 'RT-0001',
      page: '0',
      size: '20',
    });
  });

  test('applies the filters from the keyboard', async () => {
    const requests = respondWith(() => page([closure()]));
    const user = renderApp(LIST);
    await screen.findByRole('table');

    await user.type(codeInput(), 'RT-0001{Enter}');

    await screen.findByRole('table');
    expect(lastRequest(requests)).toHaveProperty('routeCode', 'RT-0001');
  });

  test('reads the filters from the URL', async () => {
    const requests = respondWith(() => page([closure()]));

    renderApp(`${LIST}?date=2026-01-15&routeCode=RT-0001`);

    await screen.findByRole('table');
    expect(dateInput()).toHaveValue('2026-01-15');
    expect(codeInput()).toHaveValue('RT-0001');
    expect(lastRequest(requests)).toHaveProperty('date', '2026-01-15');
  });

  test('clears the filters', async () => {
    const requests = respondWith(() => page([closure()]));
    const user = renderApp(`${LIST}?routeCode=RT-0001`);
    await screen.findByRole('table');

    await user.click(screen.getByRole('button', { name: text.clear }));

    await screen.findByRole('table');
    expect(codeInput()).toHaveValue('');
    expect(lastRequest(requests)).toEqual({ page: '0', size: '20' });
  });

  test('rejects an invalid route code next to the field, without calling the backend', async () => {
    const requests = respondWith(() => page([closure()]));
    const user = renderApp(LIST);
    await screen.findByRole('table');
    const before = requests.length;

    await user.type(codeInput(), 'rt-1');
    await user.click(apply());

    expect(codeInput()).toHaveAttribute('aria-invalid', 'true');
    expect(codeInput()).toHaveAccessibleDescription(messages.closure.errors.routeCode.format);
    expect(codeInput()).toHaveFocus();
    expect(requests).toHaveLength(before);
  });
});

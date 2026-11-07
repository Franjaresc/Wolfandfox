import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { expect, test } from 'vitest';
import { messages } from '../../messages';
import { BACKEND_TEXT, ROUTE_CLOSURES_URL, closure, problem } from '../../test/fixtures';
import { renderApp } from '../../test/renderApp';
import { server } from '../../test/server';

const text = messages.closure.detail;
const ID = closure().id;
const PATH = `/closures/${ID}`;

function respondWith(response: () => Response) {
  server.use(http.get(`${ROUTE_CLOSURES_URL}/:id`, response));
}

test('shows a loading state, then the closure', async () => {
  respondWith(() => HttpResponse.json(closure({ notes: 'Fictional note' })));

  renderApp(PATH);

  expect(screen.getByRole('status')).toHaveTextContent(text.loading);
  expect(await screen.findByText('RT-0001')).toBeInTheDocument();
  expect(screen.getByText('VAN-0042')).toBeInTheDocument();
  expect(screen.getByText('DRV-0007')).toBeInTheDocument();
  expect(screen.getByText('46')).toBeInTheDocument();
  expect(screen.getByText('Fictional note')).toBeInTheDocument();
  expect(screen.getByText('demo.supervisor')).toBeInTheDocument();
});

test('shows the closure date and closing time in one consistent format', async () => {
  respondWith(() => HttpResponse.json(closure()));

  renderApp(PATH);

  expect(await screen.findByText('15 Jan 2026')).toBeInTheDocument();
  expect(screen.getByText('15 Jan 2026, 18:30')).toBeInTheDocument();
  expect(screen.queryByText('2026-01-15')).not.toBeInTheDocument();
});

test('asks for the id in the path', async () => {
  let requestedId: unknown;
  server.use(
    http.get(`${ROUTE_CLOSURES_URL}/:id`, ({ params }) => {
      requestedId = params.id;
      return HttpResponse.json(closure());
    }),
  );

  renderApp(PATH);

  await screen.findByText('RT-0001');
  expect(requestedId).toBe(ID);
});

test('marks missing notes clearly', async () => {
  respondWith(() => HttpResponse.json(closure()));

  renderApp(PATH);

  expect(await screen.findByText(messages.closure.noNotes)).toBeInTheDocument();
});

test('404 shows a not found state with no retry and a way back', async () => {
  respondWith(() => problem('NOT_FOUND', 404));

  renderApp(PATH);

  expect(await screen.findByRole('alert')).toHaveTextContent(text.notFound);
  expect(screen.queryByRole('button', { name: messages.common.retry })).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: text.back })).toHaveAttribute('href', '/closures');
  expect(document.body).not.toHaveTextContent(BACKEND_TEXT);
});

test('an id that is not a UUID shows the same not found state', async () => {
  respondWith(() => problem('VALIDATION_FAILED', 400, [{ field: 'id' }]));

  renderApp('/closures/not-a-uuid');

  expect(await screen.findByRole('alert')).toHaveTextContent(text.notFound);
});

test.each([
  ['500', () => problem('INTERNAL_ERROR', 500), messages.errors.problems.INTERNAL_ERROR],
  ['a network failure', () => HttpResponse.error(), messages.errors.network],
  [
    'a response that is not a problem',
    () => new HttpResponse(null, { status: 500 }),
    messages.errors.unavailable,
  ],
])('shows a clear message on %s and recovers on retry', async (_name, failure, message) => {
  respondWith(failure);
  const user = renderApp(PATH);

  expect(await screen.findByRole('alert')).toHaveTextContent(message);
  expect(document.body).not.toHaveTextContent(BACKEND_TEXT);

  respondWith(() => HttpResponse.json(closure()));
  await user.click(screen.getByRole('button', { name: messages.common.retry }));

  expect(await screen.findByText('RT-0001')).toBeInTheDocument();
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
});

test('opens from a link in the list', async () => {
  server.use(
    http.get(ROUTE_CLOSURES_URL, () =>
      HttpResponse.json({ items: [closure()], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
    ),
  );
  respondWith(() => HttpResponse.json(closure()));
  const user = renderApp('/closures');

  await user.click(await screen.findByRole('link', { name: 'RT-0001' }));

  expect(await screen.findByRole('heading', { name: text.title })).toBeInTheDocument();
  expect(await screen.findByText('VAN-0042')).toBeInTheDocument();
});

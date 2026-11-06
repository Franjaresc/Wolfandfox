import { screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { expect, test } from 'vitest';
import { messages } from './messages';
import { ROUTE_CLOSURES_URL } from './test/fixtures';
import { renderApp } from './test/renderApp';
import { server } from './test/server';

test('opens the list from the root path', async () => {
  server.use(
    http.get(ROUTE_CLOSURES_URL, () =>
      HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
    ),
  );

  renderApp('/');

  expect(screen.getByRole('heading', { name: messages.app.title })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: messages.closure.list.title })).toBeInTheDocument();
  expect(await screen.findByText(messages.closure.list.empty)).toBeInTheDocument();
});

test('opens the form from its path', () => {
  renderApp('/closures/new');

  expect(screen.getByRole('heading', { name: messages.closure.form.title })).toBeInTheDocument();
});

import { http, HttpResponse } from 'msw';
import { expect, test } from 'vitest';
import { server } from '../test/server';
import { apiClient } from './client';

test('sends requests to the origin serving the page', async () => {
  let requestedUrl = '';
  server.use(
    http.get('/api/v1/route-closures', ({ request }) => {
      requestedUrl = request.url;
      return HttpResponse.json({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    }),
  );

  await apiClient.GET('/api/v1/route-closures', { params: { query: { routeCode: 'RT-0001' } } });

  expect(requestedUrl).toBe(`${window.location.origin}/api/v1/route-closures?routeCode=RT-0001`);
});

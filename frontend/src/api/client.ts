import createClient from 'openapi-fetch';
import type { paths } from './schema.gen';

/**
 * Requests always go to the origin serving the page: the Vite proxy in development, and whatever
 * hosts the application later. fetch is looked up on each call rather than captured once.
 */
export const apiClient = createClient<paths>({
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
});

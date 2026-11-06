import { setupServer } from 'msw/node';

/** Mocks the network at the HTTP boundary. Each test declares the responses it needs. */
export const server = setupServer();

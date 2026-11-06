import { http, HttpResponse } from 'msw';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, test } from 'vitest';
import { parse } from 'yaml';
import { failureMessage, messages } from '../messages';
import { server } from '../test/server';
import { apiClient } from './client';
import { callApi, type ApiFailure, type ProblemCode } from './problems';

const URL = '/api/v1/route-closures';
const BACKEND_TEXT = 'SQL [insert into route_closure] at org.hibernate.Secret';

function problem(code: string, status: number, extra: object = {}) {
  return HttpResponse.json(
    { type: 'urn:test', code, title: BACKEND_TEXT, status, detail: BACKEND_TEXT, ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );
}

function list() {
  return callApi(() => apiClient.GET(URL));
}

async function failureOf(response: () => Response): Promise<ApiFailure> {
  server.use(http.get(URL, response));
  const result = await list();
  if (result.ok) {
    throw new Error('Expected a failure');
  }
  return result.failure;
}

describe('callApi', () => {
  test('returns the data of a successful response', async () => {
    const page = { items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };
    server.use(http.get(URL, () => HttpResponse.json(page)));

    expect(await list()).toEqual({ ok: true, data: page });
  });

  test.each<[ProblemCode, number]>([
    ['VALIDATION_FAILED', 400],
    ['ROUTE_CLOSURE_DUPLICATE', 409],
    ['NOT_FOUND', 404],
    ['REQUEST_NOT_SUPPORTED', 405],
    ['INTERNAL_ERROR', 500],
  ])('maps the problem code %s', async (code, status) => {
    expect(await failureOf(() => problem(code, status))).toEqual({
      kind: 'problem',
      code,
      fields: [],
    });
  });

  test('keeps only the names of field errors, never their messages', async () => {
    const failure = await failureOf(() =>
      problem('VALIDATION_FAILED', 400, {
        errors: [
          { field: 'kmDriven', message: BACKEND_TEXT },
          { field: 'notes', message: BACKEND_TEXT },
        ],
      }),
    );

    expect(failure).toEqual({
      kind: 'problem',
      code: 'VALIDATION_FAILED',
      fields: ['kmDriven', 'notes'],
    });
    expect(JSON.stringify(failure)).not.toContain('SQL');
  });

  test('ignores malformed field errors', async () => {
    const failure = await failureOf(() =>
      problem('VALIDATION_FAILED', 400, { errors: [{ message: 'no field' }, 'text', null] }),
    );

    expect(failure).toEqual({ kind: 'problem', code: 'VALIDATION_FAILED', fields: [] });
  });

  test('treats a code the client does not know as unavailable', async () => {
    expect(await failureOf(() => problem('SOMETHING_NEW', 500))).toEqual({ kind: 'unavailable' });
  });

  test.each([
    ['an empty 500', () => new HttpResponse(null, { status: 500 })],
    ['an empty 502', () => new HttpResponse(null, { status: 502 })],
    ['an HTML proxy page', () => HttpResponse.html('<h1>Bad Gateway</h1>', { status: 502 })],
    ['plain JSON that is not a problem', () => HttpResponse.json({ error: 'x' }, { status: 500 })],
    ['a code in plain JSON', () => HttpResponse.json({ code: 'INTERNAL_ERROR' }, { status: 500 })],
    [
      'an invalid problem body',
      () =>
        new HttpResponse('not json', {
          status: 500,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
    ],
  ])('treats %s as unavailable', async (_name, response) => {
    expect(await failureOf(response)).toEqual({ kind: 'unavailable' });
  });

  test('reports a failed connection as a network failure', async () => {
    expect(await failureOf(() => HttpResponse.error())).toEqual({ kind: 'network' });
  });
});

describe('failureMessage', () => {
  test('uses our own text, whatever the server sent', async () => {
    const failure = await failureOf(() => problem('ROUTE_CLOSURE_DUPLICATE', 409));

    expect(failureMessage(failure)).toBe(messages.errors.problems.ROUTE_CLOSURE_DUPLICATE);
  });

  test('has a message for each kind of failure', () => {
    expect(failureMessage({ kind: 'network' })).toBe(messages.errors.network);
    expect(failureMessage({ kind: 'unavailable' })).toBe(messages.errors.unavailable);
  });

  test('covers every code in the contract', () => {
    const contract = parse(
      readFileSync(resolve(process.cwd(), '../contracts/openapi.yaml'), 'utf8'),
    ) as {
      components: { schemas: { ProblemCode: { enum: string[] } } };
    };

    expect(Object.keys(messages.errors.problems).sort()).toEqual(
      [...contract.components.schemas.ProblemCode.enum].sort(),
    );
  });
});

import type { components } from './schema.gen';

export type ProblemCode = components['schemas']['ProblemCode'];

/**
 * Why a call failed, without any text from the server. The backend's title, detail and field
 * messages are dropped here on purpose, so they cannot reach the screen.
 */
export type ApiFailure =
  | { kind: 'problem'; code: ProblemCode; fields: readonly string[] }
  /** The request never completed. */
  | { kind: 'network' }
  /** A response that is not our API's, such as a proxy error page while the backend is down. */
  | { kind: 'unavailable' };

export type ApiResult<T> = { ok: true; data: T } | { ok: false; failure: ApiFailure };

/** A Record, so a code added to the contract does not compile until it is listed here. */
const PROBLEM_CODES = {
  VALIDATION_FAILED: true,
  ROUTE_CLOSURE_DUPLICATE: true,
  NOT_FOUND: true,
  REQUEST_NOT_SUPPORTED: true,
  INTERNAL_ERROR: true,
} satisfies Record<ProblemCode, true>;

function isProblemCode(value: unknown): value is ProblemCode {
  return typeof value === 'string' && Object.hasOwn(PROBLEM_CODES, value);
}

function fieldNames(errors: unknown): string[] {
  if (!Array.isArray(errors)) {
    return [];
  }
  return errors.flatMap((error: unknown) =>
    typeof error === 'object' &&
    error !== null &&
    'field' in error &&
    typeof error.field === 'string'
      ? [error.field]
      : [],
  );
}

export function toFailure(body: unknown, response: Response): ApiFailure {
  const isProblem = response.headers.get('Content-Type')?.includes('application/problem+json');
  if (isProblem && typeof body === 'object' && body !== null && 'code' in body) {
    if (isProblemCode(body.code)) {
      return {
        kind: 'problem',
        code: body.code,
        fields: 'errors' in body ? fieldNames(body.errors) : [],
      };
    }
  }
  return { kind: 'unavailable' };
}

interface ClientResponse<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

/** Turns a client call into a result that never throws, so screens handle one failure shape. */
export async function callApi<T>(call: () => Promise<ClientResponse<T>>): Promise<ApiResult<T>> {
  let outcome: ClientResponse<T>;
  try {
    outcome = await call();
  } catch {
    return { ok: false, failure: { kind: 'network' } };
  }
  if (outcome.response.ok && outcome.data !== undefined) {
    return { ok: true, data: outcome.data };
  }
  return { ok: false, failure: toFailure(outcome.error, outcome.response) };
}

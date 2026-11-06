import type { ApiFailure, ProblemCode } from './api/problems';

/** Every user-facing string. Components never contain text of their own, so i18n can be added later. */
export const messages = {
  app: {
    title: 'Wolfandfox',
  },
  errors: {
    /** Keyed by the contract's codes, so adding a code without a message does not compile. */
    problems: {
      VALIDATION_FAILED: 'Some of the information is not valid. Check it and try again.',
      ROUTE_CLOSURE_DUPLICATE: 'This route is already closed for that date.',
      NOT_FOUND: 'We could not find what you were looking for.',
      REQUEST_NOT_SUPPORTED: 'That action is not available. Reload the page and try again.',
      INTERNAL_ERROR: 'Something went wrong on our side. Please try again.',
    } satisfies Record<ProblemCode, string>,
    network: 'We could not reach the service. Check your connection and try again.',
    unavailable: 'The service is not available right now. Please try again in a moment.',
  },
} as const;

export function failureMessage(failure: ApiFailure): string {
  switch (failure.kind) {
    case 'problem':
      return messages.errors.problems[failure.code];
    case 'network':
      return messages.errors.network;
    case 'unavailable':
      return messages.errors.unavailable;
  }
}

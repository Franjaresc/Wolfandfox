import type { ApiFailure, ProblemCode } from './api/problems';

const CODE_FORMAT =
  'Use 2 to 20 uppercase letters, digits or hyphens, starting with a letter or digit.';
const WHOLE_NUMBER = 'Enter a whole number, 0 or more.';
const NUMBER_TOO_LARGE = 'This number is too large.';
const REJECTED = 'This value was not accepted. Check it and try again.';

function codeErrors(required: string) {
  return { required, format: CODE_FORMAT, rejected: REJECTED };
}

function integerErrors(required: string) {
  return { required, integer: WHOLE_NUMBER, tooLarge: NUMBER_TOO_LARGE, rejected: REJECTED };
}

/** Every user-facing string. Components never contain text of their own, so i18n can be added later. */
export const messages = {
  app: {
    title: 'Wolfandfox',
    navigation: 'Main',
    closeRoute: 'Close a route',
    closures: 'Route closures',
    notFound: 'This page does not exist.',
  },
  common: {
    retry: 'Try again',
  },
  closure: {
    fields: {
      routeCode: 'Route code',
      closureDate: 'Closure date',
      vehicleCode: 'Vehicle code',
      driverCode: 'Driver code',
      packagesDelivered: 'Packages delivered',
      packagesFailed: 'Packages failed',
      packagesReturned: 'Packages returned',
      totalPackages: 'Total packages',
      kmDriven: 'Kilometres driven',
      notes: 'Notes',
      closedBy: 'Closed by',
      closedAt: 'Closed at',
    },
    hints: {
      code: 'For example RT-0001.',
    },
    noNotes: 'None',
    detail: {
      title: 'Route closure',
      loading: 'Loading route closure…',
      notFound: 'We could not find this route closure. It may not exist.',
      back: 'Back to route closures',
    },
    list: {
      title: 'Route closures',
      filters: 'Filter route closures',
      apply: 'Apply filters',
      clear: 'Clear filters',
      pageSize: 'Results per page',
      loading: 'Loading route closures…',
      empty: 'No routes have been closed yet.',
      emptyAction: 'Close a route',
      noMatches: 'No route closures match these filters on this page.',
      pagination: 'Pages of route closures',
      previous: 'Previous',
      next: 'Next',
      pageOf: (page: number, total: number) => `Page ${String(page)} of ${String(total)}`,
      total: (count: number) => `${String(count)} in total`,
    },
    form: {
      title: 'Close a route',
      requiredNote: 'All fields are required unless marked as optional.',
      optional: '(optional)',
      submit: 'Close route',
      submitting: 'Closing route…',
      confirmation: 'Route closed',
      closeAnother: 'Close another route',
      totalUnknown: '—',
    },
    errors: {
      routeCode: codeErrors('Enter the route code.'),
      closureDate: {
        required: 'Enter the closure date.',
        format: 'Enter a valid date.',
        future: 'The closure date cannot be in the future.',
        // In practice the server rejects a date that passed here only when its "today" is earlier.
        rejected: 'This date was not accepted. It cannot be later than today.',
      },
      vehicleCode: codeErrors('Enter the vehicle code.'),
      driverCode: codeErrors('Enter the driver code.'),
      packagesDelivered: integerErrors('Enter the packages delivered.'),
      packagesFailed: integerErrors('Enter the packages failed.'),
      packagesReturned: integerErrors('Enter the packages returned.'),
      kmDriven: integerErrors('Enter the kilometres driven.'),
      notes: {
        tooLong: 'Notes must be 1000 characters or fewer.',
        rejected: REJECTED,
      },
    },
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

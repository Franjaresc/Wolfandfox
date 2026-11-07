import { messages } from './messages';

// Dates follow the language of the interface, not the browser's, so a page never mixes languages.
const dateFormat = new Intl.DateTimeFormat(messages.locale, {
  dateStyle: 'medium',
  // A closure date is a calendar day, not an instant. Formatting it in UTC, where it was parsed,
  // keeps it from moving to the previous day in time zones behind UTC.
  timeZone: 'UTC',
});

// An instant, such as when a route was closed, is shown in the viewer's own time zone.
const dateTimeFormat = new Intl.DateTimeFormat(messages.locale, {
  dateStyle: 'medium',
  timeStyle: 'short',
});

/** Formats a calendar date from the API, such as 2026-01-15. */
export function formatDate(isoDate: string): string {
  return dateFormat.format(new Date(`${isoDate}T00:00:00Z`));
}

/** Formats an instant from the API, such as 2026-01-15T18:30:00Z. */
export function formatDateTime(isoDateTime: string): string {
  return dateTimeFormat.format(new Date(isoDateTime));
}

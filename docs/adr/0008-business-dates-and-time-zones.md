# ADR-0008: Business dates, time zones and date display

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

A route can't be closed for a day that hasn't happened yet. That rule depends on what "today" is, which depends on a time zone: at 00:30 in one zone it is still yesterday in another. The browser, the server and the people using the system can each be in a different zone.

The rule also has to be testable without waiting for midnight, and the screens showed dates in two different styles.

## Decision

**The domain receives today's date as a parameter.** `RouteClosure.close(…, today)` rejects a closure date after `today` with `ClosureDateInFutureException`. The domain never reads a clock, so its tests pass any date they like. Stored closures aren't rechecked, because a valid closure stays valid as the calendar moves on.

**The use case derives `today` from the injected `Clock`.** It reads the clock once and uses that instant for both `closedAt` and the business date, so the two can't disagree. An ArchUnit rule stops `domain` and `application` from calling `LocalDate.now()`, `Instant.now()`, `Clock.systemUTC()` or similar. Calls that take a `Clock`, such as `LocalDate.now(clock)`, stay allowed.

**The clock's zone defines the business day.** It is configured with `WOLFANDFOX_TIME_ZONE` and defaults to UTC. A deployment should set it to where its routes run.

**The server decides, and the browser helps.** The form limits the date picker and checks the date against the browser's local "today", so most mistakes are caught before sending. When the two zones disagree near midnight, the server's answer wins. Its 400 names `closureDate`, and the form shows a date-specific message.

**The API reports it as a field error.** The response is 400 with code `VALIDATION_FAILED` and a field error on `closureDate`, like any other invalid field. The rule is described in the contract, because OpenAPI has no keyword that can express it.

**Dates are displayed in the interface's language.** One formatter shows both kinds of date in the same style, using the locale in `messages.ts`, currently `en-GB`. The browser's locale isn't used, because that could mix languages on one page. A closure date is a calendar day, so it's formatted in UTC and can't move to the previous day for viewers behind UTC. An instant such as `closedAt` is shown in the viewer's own time zone.

## Consequences

**Easier**

- Tests use fixed dates and clocks, including a test showing that the clock's zone changes which day is "today".
- Any future rule about dates gets the same deterministic source of time.
- Translating the interface later changes the date format along with the text.

**Harder**

- Near midnight, the browser can accept a date that the server rejects, or the reverse. The server's message covers the first case. In the second, the person can't pick a date the server would allow until their own midnight.
- The database can't enforce this rule, because "today" isn't a fixed value. A row inserted outside the application isn't checked.
- With the default UTC zone, people far from UTC see "today" change at an unexpected hour, until the zone is configured.

## Alternatives considered

**Bean Validation's `@PastOrPresent` on the request.** One annotation, but it uses the JVM's default clock and zone, can't be tested with a fixed clock, and would put a business rule in the web layer.

**The domain reading `LocalDate.now()` itself.** Simpler call sites, but untestable without controlling the system clock, and the zone would depend on the machine.

**A check constraint in the database.** It can't refer to a stable "today".

**The browser's local date as the authority.** It's convenient for the person using it, but the client can be wrong or tampered with, so the server has to decide anyway.

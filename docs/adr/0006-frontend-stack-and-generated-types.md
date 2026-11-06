# ADR-0006: Frontend stack, committed types and testing approach

- **Status:** Accepted
- **Date:** 2026-11-06
- **Supersedes:** the part of ADR-0003 that says generated code is never committed, for the frontend's types only. Backend generated code stays uncommitted.

## Context

The frontend must stay in step with `contracts/openapi.yaml`, and the build must fail if its types fall behind. It has two screens plus a detail view, one developer, and a need to look clean and be accessible. It will need translating later, but not yet.

Generated TypeScript types carry no patterns or limits, so type generation alone can't keep client-side validation aligned with the contract.

## Decision

**Commit the generated types.** `openapi-typescript` writes `src/api/schema.gen.d.ts`, which is committed. `types:check` regenerates to a temporary file and fails on any difference, and `build` runs it first. A contract change therefore shows its effect on the frontend in the same diff.

**Keep validation aligned with a test.** `validation.contract.test.ts` reads the contract and fails if the frontend's patterns, limits, page sizes or field list differ from it.

**Use `openapi-fetch` as the client.** It's a thin wrapper typed from the generated paths. Requests always go to the origin serving the page, and the Vite dev server proxies `/api` to `BACKEND_PORT`.

**Show only our own text.** `api/problems.ts` reduces any failure to a stable code and field names, dropping the backend's `title`, `detail` and field messages, so they can't reach the screen. Anything that isn't a valid problem, such as a proxy's 502 while the backend is down, becomes a generic "unavailable" failure. All text lives in `src/messages.ts`, and an ESLint rule rejects literal text in JSX. There is no i18n library yet.

**Use plain CSS and no UI framework.** The screens are one form, one table and one summary, which don't justify a framework's weight or its visual identity.

**Test at the HTTP boundary.** Vitest and Testing Library drive the real components and the real client, and MSW answers at the network level. No application module is mocked, so request building, response parsing and error mapping are all exercised. Unmocked requests fail the test.

**Hold two tools back, on purpose.** TypeScript stays on 5.9.3 because `openapi-typescript` accepts only 5.x. ESLint stays on 9.39.5 because `eslint-plugin-jsx-a11y` accepts only up to 9.

## Consequences

**Easier**

- Reviewers see API changes in the frontend's diff, and types are available without a generation step.
- Stale types, drifting validation rules and hard-coded text fail automatically.
- Tests survive refactoring, because they don't depend on how components call the API.
- Adding translations later means replacing one module and mapping stable codes.

**Harder**

- The committed file adds noise to diffs when the contract changes, and it can be edited by mistake. `types:check` catches both forgetting to regenerate and hand edits.
- Backend and frontend now treat generated code differently, which needs explaining. `CLAUDE.md` records it.
- npm marks ESLint 9.39.5 as deprecated, so this choice has a limited life. Upgrade when `eslint-plugin-jsx-a11y` supports ESLint 10, or replace the plugin.
- `npm audit` reports a moderate minimatch advisory reached through `typescript-eslint`. It affects lint tooling, not the browser bundle, and npm's suggested fix is a downgrade, so it's left in place until an upstream release resolves it.
- Field errors from the server use a generic per-field message, because the frontend ignores the backend's wording. Client-side validation catches nearly everything first, so this should be rare.
- jsdom doesn't do layout, so tests can't catch rendering or responsive problems. Those need a manual check, or browser tests later.

## Alternatives considered

**Generate types at build time without committing them.** This matches the backend and keeps diffs smaller. Rejected because a check that regenerates and compares needs a committed file to compare against. Without one, the only failure signal is a type error somewhere in the code, and only if the changed part happens to be used.

**A UI framework such as MUI or Tailwind.** Faster for large applications. Rejected because it adds weight and decisions for three simple views.

**A data-fetching library such as TanStack Query.** It offers caching and request deduplication, which these screens don't need. A small `useApiResource` hook covers loading, retry and stale responses.

**Mocking the client module in tests.** Simpler to set up. Rejected because it would skip the request and error handling that most needs testing.

**Browser tests with Playwright.** They would cover rendering and real keyboard use. Left out of this phase to keep the tooling small, so those checks are manual for now.

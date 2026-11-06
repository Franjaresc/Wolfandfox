# ADR-0001: Monorepo and modular monolith

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Wolfandfox starts with one bounded context, `route`, one developer, one frontend and one database. Later phases may add events, a reporting service and file storage, but none of them exists yet.

The API contract, backend and frontend will change together for the foreseeable future. The boundaries between future contexts are also still guesses, and those guesses should stay cheap to correct.

## Decision

Keep everything in **one repository**, and build the backend as **one deployable modular monolith**.

- The repository holds `backend/`, `frontend/`, `contracts/`, `deploy/` and `docs/`. A contract change and the code that implements it land in the same commit.
- The backend is a single Maven module with one top-level package per bounded context, starting with `route`.
- Boundaries inside each context are enforced by ArchUnit tests that run in the normal build, rather than by build modules.
- A context is extracted into its own service only when there is a concrete reason, such as a different scaling profile or an independent release cadence.

## Consequences

**Easier**

- Atomic changes across contract, backend and frontend.
- One command to build and test, one CI workflow, and one thing to deploy.
- Refactoring across contexts while their boundaries are still being discovered.
- Local transactions, with no distributed-consistency problems yet.

**Harder**

- Contexts can't be released or scaled independently.
- A failure or slow build affects the whole backend.
- Without discipline a monolith decays, so the architecture tests must stay strict.
- ArchUnit reports a boundary violation at test time, not at compile time. It is a weaker guarantee than separate Maven modules.
- CI builds everything on each change until path filters are worth adding.

## Alternatives considered

**Microservices from the start.** Rejected because one context has nothing to split, and the operational cost (service discovery, distributed tracing, eventual consistency) would buy no benefit at this size. A clean extraction later demonstrates more judgment than premature distribution.

**Separate repositories for backend, frontend and contract.** Rejected because every API change would need coordinated pull requests and versioning across repositories, which only pays off with independent teams.

**One Maven module per hexagonal layer.** This gives compile-time boundary enforcement, and is the natural next step if ArchUnit proves too weak. Rejected for now because it adds several build descriptors per context for a guarantee that ArchUnit provides adequately with a single context.

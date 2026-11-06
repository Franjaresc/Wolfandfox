# ADR-0002: Hexagonal architecture enforced by tests

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

Business rules such as "one closure per route and date" should outlive framework upgrades and stay testable without a database or an HTTP server. Later phases will also replace or add infrastructure: real authentication, events and file storage. Those changes shouldn't reach the rules.

Layering by convention tends to erode one convenient import at a time, so the rules need something that fails the build.

## Decision

Each bounded context is split into `domain`, `application`, `adapter` and `config`, with dependencies pointing inward. ArchUnit tests enforce this in the normal build.

- `domain` is pure Java and owns the invariants of a single closure. Value objects make an invalid state impossible to construct.
- `application` holds use cases and ports. Use cases are plain classes wired in `config`, with no stereotype annotations.
- `application` has its own `PageQuery` and `PageResult`, so Spring Data types never cross a port.
- **Exception:** `application` may use Spring's `@Transactional`. A use case is the correct transaction boundary, and declaring it there is clearer than wrapping each use case from outside.
- Input ports take value objects rather than raw strings, so validity is established before a use case runs.
- Uniqueness per route and date spans many closures, so it can't live in the aggregate. The database constraint is the guarantee. The use case's pre-check only avoids a failed insert in the common case, and the persistence adapter translates a constraint violation into the same domain error, so concurrent requests also get a conflict.

## Consequences

**Easier**

- Domain and use case tests run in seconds with no Spring context or Docker.
- `CurrentUserPort` can move from a stub to JWT without changing a use case.

**Harder**

- More types and mapping code than a layered CRUD application: a closure exists as a domain record, a JPA entity and two DTOs. That is heavy for a single use case, and it pays off only as rules and adapters grow.
- The same rules appear in the contract, the domain and the database. This is deliberate defence in depth, but a rule change has to touch all three.
- The `@Transactional` exception ties `application` to one Spring annotation.

## Alternatives considered

**Classic layered architecture (controller, service, repository).** Less code, and adequate for simple CRUD. Rejected because nothing stops persistence and web concerns from leaking into the rules, and tests tend to need the framework.

**No framework at all in `application`, with transactions applied by a decorator in `config`.** The purest option. Rejected because it adds a wrapper per use case to avoid one annotation that has no effect on behaviour under test.

**Rich aggregate that checks its own uniqueness through a repository.** Rejected because it would make the domain depend on a port and on I/O.

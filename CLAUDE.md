# Wolfandfox: conventions for AI assistants

Wolfandfox is a public portfolio project: a courier back-office for daily routes, vehicles, drivers and end-of-day route closures. Readers judge its engineering quality, so prefer clear and small over clever and complete.

## Do not

- Do not run `git push`. The maintainer pushes.
- Do not commit secrets or `.env` files. Only `.env.example` is tracked.
- Do not use real data. No real names, plates, addresses, clients or business rules in code, seeds, docs or tests. Use obviously fake values such as `RT-0001`, `VAN-0042`, `DRV-0007` and `demo.supervisor`.
- Do not add `Co-Authored-By` trailers to commits.
- Do not implement anything listed in `docs/backlog.md` unless asked.
- Do not add unused scaffolding, empty placeholders or speculative abstractions.
- Do not edit generated code or an applied Flyway migration. Change the contract, or add a new migration.

## Architecture

The system is a modular monolith with hexagonal modules. See `docs/adr/`.

Dependencies point inward:

```
adapter.in.web ──▶ application ──▶ domain
adapter.out.*  ──▶ application ──▶ domain
```

- `domain`: pure Java. No Spring, JPA, Jackson or adapter imports. It owns the invariants.
- `application`: use cases and ports. It depends only on `domain`. The one allowed framework dependency is Spring's `@Transactional`. Use cases are plain classes wired in `config`, with no stereotype annotations.
- `adapter`: translates between the outside world and ports. Adapters never depend on each other.
- `config`: Spring wiring. It is the only package that may see every layer.
- Framework types such as Spring Data's `Page` or JPA entities never cross a port.
- ArchUnit tests enforce these rules. If a rule blocks you, fix the design rather than the rule.

### Package layout

Base package `dev.franjaresc.wolfandfox`, one top-level package per bounded context:

```
route/
├── domain/
├── application/
│   ├── port/in/
│   ├── port/out/
│   └── service/
├── adapter/in/web/
├── adapter/out/persistence/
├── adapter/out/identity/
└── config/
```

### Contract and schema

- `contracts/openapi.yaml` is the source of truth for the API. Change it first, then implement. Backend interfaces and DTOs and frontend types are generated from it, and generated code is not committed.
- Flyway owns the schema, and Hibernate only validates it. Derived values, such as total packages, are never stored.
- Errors are RFC 7807 `application/problem+json`.
- The local database's host port comes from `POSTGRES_PORT` in `deploy/.env` (default `15432`). Never hardcode a host port: the backend datasource URL must read this variable with the same default. Testcontainers picks its own port and needs no configuration.

## Naming

- Input ports: `<Verb><Noun>UseCase`, or `<Find/Get><Noun>Query` for reads.
- Output ports: `<Noun>Port`.
- Use case implementations: `<Noun>Service`.
- Adapters: `<Noun><Technology>Adapter`, for example `RouteClosureJpaAdapter`.
- JPA entities: `<Noun>JpaEntity`. Domain classes carry no technology suffix.
- REST paths are plural and versioned: `/api/v1/route-closures`.
- Migrations: `V<n>__<snake_case_description>.sql`.
- Code, commits, docs and ADRs are written in English.

## Tests

- Backend `*Test` classes run with Surefire and need no Docker: domain, use cases with Mockito, MockMvc slices and ArchUnit.
- Backend `*IT` classes run with Failsafe against PostgreSQL through Testcontainers. Never replace it with H2.
- Use JUnit 5 with AssertJ. Name test methods after behaviour, such as `rejectsNegativeKilometres`.
- Frontend tests use Vitest and Testing Library, and assert what the user sees rather than implementation details.
- Run `./mvnw verify` in `backend/` before calling backend work done. In `frontend/`, run lint, typecheck, test and build.

## Commits

- Use Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`, `refactor:`, with an optional scope such as `feat(route):`.
- Make one logical change per commit, and include its tests in the same commit.
- If a choice is ambiguous and expensive to redo, ask. Otherwise choose a sensible default and record it in an ADR.

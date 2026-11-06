# ADR-0003: Contract-first API with generated code

- **Status:** Accepted; partially superseded by [ADR-0006](0006-frontend-stack-and-generated-types.md), which commits the frontend's generated types
- **Date:** 2026-11-06

## Context

The backend and frontend must agree on paths, payloads and validation rules. Hand-written DTOs on both sides drift silently, and the drift usually surfaces at runtime.

## Decision

`contracts/openapi.yaml` is the source of truth, and both sides generate code from it during the build. Generated code is never committed.

- **Backend:** `openapi-generator-maven-plugin` generates Spring interfaces and DTOs into `target/generated-sources`. Controllers implement those interfaces, so a contract change that the backend doesn't follow stops compiling.
- **Frontend:** TypeScript types are generated from the same file.
- **Validation:** constraints in the contract become Bean Validation annotations on the backend, and the frontend form mirrors them.
- **Version:** the contract uses OpenAPI 3.0.3, the version with the most mature support across the generators.
- **Errors:** problem responses are part of the contract, so clients can rely on their shape.

## Consequences

**Easier**

- Contract drift becomes a compile error on both sides.
- The API can be reviewed as one readable file, before any implementation exists.

**Harder**

- The generator is a critical build dependency. Upgrades can change generated signatures, so its version is pinned.
- A clean checkout can't compile until generation has run, which IDEs may need configuring for.
- Generated types are slightly awkward: a `Dto` suffix, and mapping code between DTOs and domain types.
- The compiler can't catch everything. Generated interfaces don't enforce status codes or problem responses, so web-layer tests must cover them.

## Alternatives considered

**Code-first, with the specification generated from annotations.** Faster to start. Rejected because the specification then follows the implementation, accidental API changes go unnoticed, and frontend work can't start from the contract.

**A contract written by hand and checked only by contract tests.** No generator dependency. Rejected because compile errors are faster and cheaper than test failures, and it would still require writing the DTOs by hand.

**Committing generated code.** Makes the build simpler and shows generated changes in reviews. Rejected because it adds noisy diffs and files that can be edited by mistake.

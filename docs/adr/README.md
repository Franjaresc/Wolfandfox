# Architecture Decision Records

Each record captures one significant decision: its context, the choice made, what it costs, and what was rejected. Records are immutable once accepted. To change a decision, add a new record that supersedes the old one.

To add a record, copy [template.md](template.md) to `NNNN-short-title.md` and add a row below.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-monorepo-and-modular-monolith.md) | Monorepo and modular monolith | Accepted |
| [0002](0002-hexagonal-architecture-enforced-by-tests.md) | Hexagonal architecture enforced by tests | Accepted |
| [0003](0003-contract-first-api-with-generated-code.md) | Contract-first API with generated code | Accepted; partially superseded by 0006 |
| [0004](0004-spring-boot-4.md) | Spring Boot 4 instead of 3.x | Accepted |
| [0005](0005-strict-api-behaviour-and-contract-tests.md) | Strict API behaviour, verified against the contract | Accepted |
| [0006](0006-frontend-stack-and-generated-types.md) | Frontend stack, committed types and testing approach | Accepted |

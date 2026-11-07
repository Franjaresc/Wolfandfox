# ADR-0005: Strict API behaviour, verified against the contract

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

ADR-0003 makes contract drift a compile error for signatures, but the compiler can't see what a running server actually sends: status codes, headers, content types, or which JSON fields appear. Jackson's defaults also accept input the contract forbids. It ignores unknown fields, truncates `87.5` to `87`, and turns the number `1234` into the string `"1234"`, which is a valid route code.

Duplicate closures have a related problem. A concurrent request can pass the use case's pre-check, so the database constraint must produce the same result as the pre-check.

## Decision

**Contract fidelity: validate real responses in tests.** `ContractValidator` uses `openapi-request-validator-core` to check responses against `contracts/openapi.yaml`. MockMvc tests and the HTTP integration tests pass every response through it, whatever else they assert. `ContractValidatorTest` proves that the validator can fail.

**Strict JSON.** Unknown fields, wrong types and coerced values are rejected with a 400. Most of this is configured in `application.yml`. Rejecting numbers and booleans as strings has no property, so `StrictJsonConfig` sets it. Null fields are omitted from responses, because the contract declares none as nullable.

**Errors.** Every failure is `application/problem+json`. Details come only from domain messages or fixed text, never from framework, Jackson or database exceptions. Unexpected errors are logged in full and returned as a generic 500. Messages are always English, because Bean Validation would otherwise follow the machine's locale and the `Accept-Language` header, and mix languages with domain messages.

**Duplicates.** The persistence adapter identifies the violation by constraint name (`uq_route_closure_route_code_closure_date`), taken from Hibernate's exception rather than from a message. Other integrity failures stay unexpected errors. The concurrency test uses a barrier so that both requests always pass the pre-check, which means every run exercises the constraint path.

## Consequences

**Easier**

- Drift in fields, status codes, headers and content types fails the build.
- Clients get one predictable error shape, and a misspelled field is reported instead of being silently dropped.

**Harder**

- Validation covers only responses that tests exercise. An untested status code on an endpoint can still drift.
- Adding a response field becomes a contract change first. Clients sending extra fields get a 400, so this API can't accept fields from a newer client.
- Framework responses such as 405 and 415 use the problem format but aren't declared in the contract.
- The validator adds a test-only dependency with its own Jackson 2 and Swagger parser. Its messages vary by version and locale, so tests don't assert on their wording. Moving from `swagger-request-validator-core` 2.46.1 to these relocated coordinates at 3.0.0 exposed a real contract bug the older version missed: `instance` was declared as an absolute URI but sent as a relative path.
- Identifying the duplicate relies on Hibernate's `ConstraintViolationException` exposing the name, and the name is repeated between the migration and the adapter. Tests against PostgreSQL fail if the two diverge.

## Alternatives considered

**A separate contract-testing tool such as Schemathesis or Pact.** Schemathesis could generate cases that nobody wrote, and Pact suits independently released consumers. Rejected for now because both add tooling and CI steps, while a monorepo with one client gets most of the value from validating the tests it already has.

**Tolerant reader for unknown fields.** Common for APIs with many independent clients. Rejected because this API has one client in the same repository, and silently ignoring a misspelled optional field such as `notes` would lose data.

**Matching constraint names in the exception message.** Simpler, but message text is not a stable interface.

**A concurrency test with no barrier.** Simpler, but it would usually pass through the pre-check and never reach the constraint.

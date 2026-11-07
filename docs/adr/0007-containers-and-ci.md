# ADR-0007: Containers and continuous integration

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

Someone evaluating this project should be able to run it with one command, without installing Java or Node. Every change should also be built and tested automatically, including the images, which unit and integration tests never touch.

## Decision

**Multi-stage images, both running as non-root.** The backend builds on a JDK and runs on a JRE. The frontend builds on Node and runs on `nginxinc/nginx-unprivileged`. Both build from the repository root, because both builds read `contracts/openapi.yaml`. Base images are pinned to exact versions.

**One compose file for both ways of working.** `docker compose up --build` starts PostgreSQL, the backend and the frontend, in that order, each waiting for the previous one to be healthy. `docker compose up -d postgres` starts only the database for local development. Every host port is configurable and bound to loopback.

**nginx serves the frontend and proxies only `/api`.** The browser stays on one origin, as it does with the Vite proxy in development, so no CORS configuration is needed.

**Actuator exposes only `/actuator/health`, without details.** Container health checks need a cheap signal that includes the database. nginx doesn't forward it, and a test proves that no other Actuator endpoint is exposed.

**Compose defaults to the placeholder password from `.env.example`.** This makes the one-command run work straight from a clone. The value is already public, so it protects nothing, and it's acceptable only because the database is bound to loopback and holds fictional data. It can be overridden in `deploy/.env`.

**CI has three jobs:** backend (`mvnw clean verify`), frontend (lint, format, typecheck, `types:check`, test, build) and full stack (builds the images, starts the stack and runs `deploy/smoke-test.sh` over HTTP). Actions are pinned to commit hashes, the token has only `contents: read`, and checkouts don't keep credentials.

**Dependabot checks weekly** for Maven, npm, GitHub Actions, Docker and compose. It doesn't propose major versions for choices made on purpose: TypeScript, ESLint, Java 21, the Node line and PostgreSQL 16. For nginx it doesn't propose minor versions either, because odd minor versions are mainline and this project stays on the stable line.

## Consequences

**Easier**

- The project is designed to run from a clone with only Docker installed.
- The CI job tests the images and their wiring, not just the code.
- Pinned versions make builds repeatable, and Dependabot keeps them from going stale.

**Harder**

- Image builds skip tests, so an image is only as trustworthy as the CI run for that commit.
- The Node version lives in both `.nvmrc` and the frontend's Dockerfile. A CI step fails if they differ, so a Dependabot bump of either needs the other updated by hand.
- The full-stack job makes CI slower.
- Environments that intercept TLS can't download dependencies during image builds, because the images don't trust the intercepting certificate. This project doesn't add a workaround, and TLS verification stays on. Turn off the interception, or build elsewhere.
- Actuator is a new dependency, and a health endpoint arrives before the rest of the observability work.
- The default password would be unsafe anywhere except a local machine. A real deployment needs its own secrets, and this is not a production setup.

## Alternatives considered

**Health checks against a business endpoint.** No new dependency, but each check would run a page query and mix monitoring with API traffic.

**Requiring `deploy/.env` before the first run.** Stricter, but it adds a step for a password that is already public.

**Official `nginx` image.** It's more widely used and was updated more recently when this was decided, but its default configuration runs the master process as root.

**Compose profiles to separate the database from the application.** Rejected because plain `docker compose up --build` must start everything, and naming the `postgres` service is enough for the database-only mode.

**No full-stack job in CI.** Faster, but a broken Dockerfile would go unnoticed until someone tried to run the project.

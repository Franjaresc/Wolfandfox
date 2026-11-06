# Wolfandfox

![CI status: not yet configured](https://img.shields.io/badge/CI-not%20yet%20configured-lightgrey)

A courier and last-mile logistics back-office for daily routes, vehicles, drivers and end-of-day route closures. It is a portfolio project that demonstrates software architecture in Java: a modular monolith with hexagonal modules, contract-first APIs and architecture rules enforced by tests.

**All data in this project is fictional.**

## Status

Phase 1 is in progress. The backend and frontend work end to end. CI and the architecture overview are still to come.

## Stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Maven |
| Frontend | React 19, Vite, strict TypeScript, plain CSS |
| Database | PostgreSQL 16, Flyway |
| API contract | OpenAPI, the source of truth for backend code and frontend types |

## Run it locally

Requires Docker, JDK 21 and Node 26 (see `frontend/.nvmrc`). Use one terminal for each of the last two steps.

**1. Start the database.**

```sh
cd deploy
cp .env.example .env
docker compose up -d
```

PostgreSQL is available on `localhost:15432`. To use another host port, change `POSTGRES_PORT` in `deploy/.env`.

**2. Start the backend** on port 8080. On Windows, use `mvnw.cmd`.

```sh
cd backend
./mvnw spring-boot:run
```

**3. Start the frontend.**

```sh
cd frontend
npm ci
npm run dev
```

Open <http://localhost:5173>. The dev server forwards `/api` to the backend. If the backend runs on another port, copy `frontend/.env.example` to `frontend/.env` and change `BACKEND_PORT`.

## Scripts

**Backend**, in `backend/`:

| Command | What it does |
|---|---|
| `./mvnw test` | Runs unit, web and architecture tests. No Docker needed. |
| `./mvnw verify` | Also runs integration tests on PostgreSQL 16 through Testcontainers. Docker must be running. |

**Frontend**, in `frontend/`:

| Command | What it does |
|---|---|
| `npm run dev` | Starts the dev server with the `/api` proxy. |
| `npm run build` | Checks the generated types, type-checks and builds for production. |
| `npm test` | Runs the tests once. |
| `npm run lint` | Runs ESLint, allowing no warnings. |
| `npm run typecheck` | Runs the TypeScript compiler. |
| `npm run format` | Formats the code with Prettier. `format:check` only reports. |
| `npm run types:generate` | Regenerates `src/api/schema.gen.d.ts` from the contract. Run it after changing the contract. |
| `npm run types:check` | Fails if the generated types are out of date with the contract. |

## Documentation

- [Architecture Decision Records](docs/adr/README.md)
- [Backlog of later phases](docs/backlog.md)
- [Conventions for AI assistants](CLAUDE.md)

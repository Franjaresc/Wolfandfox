# Wolfandfox

![CI status: not yet configured](https://img.shields.io/badge/CI-not%20yet%20configured-lightgrey)

A courier and last-mile logistics back-office for daily routes, vehicles, drivers and end-of-day route closures. It is a portfolio project that demonstrates software architecture in Java: a modular monolith with hexagonal modules, contract-first APIs and architecture rules enforced by tests.

**All data in this project is fictional.**

## Status

Phase 1 is in progress. The foundations are in place, and the backend and frontend are still to come. The architecture overview and full run instructions will be added once they exist.

## Stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Maven |
| Frontend | React, Vite, TypeScript |
| Database | PostgreSQL 16, Flyway |
| API contract | OpenAPI |

## Run the database

Requires Docker.

```sh
cd deploy
cp .env.example .env
docker compose up -d
```

PostgreSQL is available on `localhost:15432`. To use another host port, change `POSTGRES_PORT` in `deploy/.env`.

## Documentation

- [Architecture Decision Records](docs/adr/README.md)
- [Backlog of later phases](docs/backlog.md)
- [Conventions for AI assistants](CLAUDE.md)

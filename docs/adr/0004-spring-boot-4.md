# ADR-0004: Spring Boot 4 instead of 3.x

- **Status:** Accepted
- **Date:** 2026-11-06

## Context

The original project brief named Spring Boot 3.x. When the project started, Maven Central offered 3.5.16 as the last 3.x release and 4.1.2 as the newest overall. A new project pays a migration cost later if it starts on the older line. It also signals its age to anyone reading the build file.

The main risk was tooling: whether code generation and the test stack already supported Boot 4.

## Decision

Use **Spring Boot 4.1.x** with Java 21.

The tooling risk was checked before committing to it. OpenAPI Generator 7.27.0 has `useSpringBoot4` and `useJackson3` options, and the code it generates from this project's contract compiles against Boot 4.1.

## Consequences

**Easier**

- No migration from 3.x to plan for, and a current baseline for later phases.

**Harder**

- Boot 4 splits auto-configuration into focused modules and renames starters such as `spring-boot-starter-webmvc`, so many examples online are out of date.
- Jackson 3 becomes the default, and its package names differ from Jackson 2.
- Fewer community answers exist for problems specific to Boot 4.

## Alternatives considered

**Spring Boot 3.5.16.** Matches the original brief and has the most mature tooling. Rejected because a new project would start on the older line with an upgrade already owed, and the tooling check removed the main argument for it.

**Spring Boot 4.0.x.** Rejected because it has no advantage over 4.1 for a new project.

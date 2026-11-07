# Backlog

Later phases, in rough priority order. None of this is implemented, and it isn't in scope for Phase 1.

## Later phases

### Authentication and authorization
Issue and validate JWTs, add roles, and replace the stub behind `CurrentUserPort` with a real adapter. The use cases don't need to change, because the port is already the seam.

### Security hardening
Follows authentication, and covers:

- JWT with roles, enforced on every endpoint.
- Rate limiting at the gateway or API.
- Security headers and strict CORS.
- Dependency and secret scanning in CI.
- A short threat model, including what is delegated to infrastructure, such as DDoS protection.

### Frontend internationalization
Translate the frontend into English and Spanish with react-i18next. Replace `src/messages.ts` with translation resources, and keep mapping the backend's stable problem codes to text.

### Events and reporting service
Publish domain events such as "route closed" to Kafka, using a transactional outbox so that events and database writes stay consistent. Add a separate reporting service that consumes them. This would be the first real reason to extract a service from the monolith.

### Proof-of-delivery photos
Store photos in S3 or MinIO, with presigned URLs so that uploads and downloads bypass the backend.

### API gateway
Add a single entry point for routing, rate limiting and token validation. It becomes worthwhile once there is more than one service.

### Observability
Add structured logs, metrics and distributed tracing through OpenTelemetry, with readiness endpoints. A basic `/actuator/health` already exists for container health checks (ADR-0007).

### Dashboards
Show operational views over the reporting data, such as delivery success rate and kilometres per route.

## Open domain questions

Phase 1 deliberately doesn't decide these:

- **Vehicles and drivers:** they are opaque codes for now. Should they become bounded contexts of their own, with validated references?
- **Correcting a closure:** a closure can't be edited or reopened yet. What should the correction flow be, and how should it be audited?

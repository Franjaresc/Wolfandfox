# Backlog

Later phases, in rough priority order. None of this is implemented, and it isn't in scope for Phase 1.

## Later phases

### Authentication and authorization
Issue and validate JWTs, add roles, and replace the stub behind `CurrentUserPort` with a real adapter. The use cases don't need to change, because the port is already the seam.

### Events and reporting service
Publish domain events such as "route closed" to Kafka, using a transactional outbox so that events and database writes stay consistent. Add a separate reporting service that consumes them. This would be the first real reason to extract a service from the monolith.

### Proof-of-delivery photos
Store photos in S3 or MinIO, with presigned URLs so that uploads and downloads bypass the backend.

### API gateway
Add a single entry point for routing, rate limiting and token validation. It becomes worthwhile once there is more than one service.

### Observability
Add structured logs, metrics and distributed tracing through OpenTelemetry, with health and readiness endpoints.

### Dashboards
Show operational views over the reporting data, such as delivery success rate and kilometres per route.

## Open domain questions

Phase 1 deliberately doesn't decide these:

- **Future closure dates:** should closing a route for a future date be rejected? That rule depends on which time zone defines "today".
- **Vehicles and drivers:** they are opaque codes for now. Should they become bounded contexts of their own, with validated references?
- **Correcting a closure:** a closure can't be edited or reopened yet. What should the correction flow be, and how should it be audited?

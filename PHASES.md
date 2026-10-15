# Biz-Engine implementation phases

- Phase 14: Spring Boot foundation, Flyway, PostgreSQL configuration, request IDs, API error contract, status endpoint.
- Phase 15: Partner domain, repository, service, REST DTOs/controllers, validation, and tests.
- Phase 16: Partner Ed25519 signed-request authentication, credentials, replay protection, nonce cleanup, and authentication guide.
- Phase 17: Operator authentication/authorization, tenant isolation, and access policy.
- Phase 18: Idempotent event ingestion and canonical event model.
- Phase 19: Transactional outbox and Kafka publishing/consumption.
- Phase 20: Entity, identity, context, observation, and first deterministic intelligence vertical slice.

18. Event ingestion: authenticated, authorized, idempotent persistence of partner operational facts.

## Phase 19 — Transactional outbox and Kafka
- `biz_event` and `biz_outbox_event` are written in the same PostgreSQL transaction.
- A scheduled publisher locks pending outbox rows with `FOR UPDATE SKIP LOCKED`, publishes to Kafka, and marks them published only after broker acknowledgement.
- Publication is at-least-once. A crash after Kafka acknowledgement but before database commit can create a duplicate; consumers must deduplicate using the stable `eventId` / outbox ID.
- Transient failures use bounded exponential backoff. After the configured attempt limit, messages are forwarded to `biz.events.dlq`; if the DLQ is unavailable, the durable row is retried.
- Kafka is transport, not the source of truth. PostgreSQL remains authoritative.

- Phase 22: Deterministic time-window aggregation, comparison with a previous window, weighted signal strength, trend snapshots, and partner-scoped trend API.


## Phase 23 — Insights and evidence
- Deterministic insight rules evaluate the latest eligible partner-scope trends.
- Insight records carry rule version, scope, direction, evidence-quality score, status, and expiry.
- Evidence links preserve trend snapshot references; deduplication, opposite-direction supersession, and expiration are implemented.
- `GET /api/v1/insights` is partner-authenticated and returns only the authenticated partner's own insights.
- No AI-generated conclusions or cross-partner/country insights are exposed in this phase.


## Phase 24 — Recommendations
- Deterministic recommendation rules create review prompts from active insights with sufficient confidence and MEDIUM/HIGH evidence quality.
- Recommendations have an objective, rationale, suggested next step, priority, confidence, evidence quality, source insight, and validity window.
- Recommendation generation is idempotent per partner/source insight/rule code; accepted or dismissed status is preserved across reevaluation.
- `GET /api/v1/recommendations` requires signed partner authentication and the `RECOMMENDATION_READ` permission for the partner scope.
- Recommendations are proposals only. This phase does not execute actions or claim causal explanations; decision, policy evaluation, action execution, and outcome feedback remain later phases.


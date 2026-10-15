# Phase 18 — Event ingestion

## Contract

`POST /api/v1/events` accepts one operational fact from an authenticated partner. Requests must be signed using the Phase 16 Ed25519 protocol and must include an `Idempotency-Key` containing 16–128 safe characters. For this endpoint only, the canonical signed message inserts the exact `Idempotency-Key` value as a separate line immediately before the body-hash line. This prevents an intermediary from changing the idempotency key without invalidating the signature.

Example body:

```json
{
  "applicationId": "8f7e50a7-f6d6-44c1-b2c7-8f50f9e2c181",
  "eventType": "content.viewed",
  "schemaVersion": 1,
  "occurredAt": "2026-10-12T10:00:00Z",
  "subjectType": "content",
  "subjectReference": "content-123",
  "context": {"surface": "feed"},
  "payload": {"durationSeconds": 8}
}
```

`payload` and `context` must be JSON objects. `occurredAt` is when the partner says the fact happened; `receivedAt` is assigned by Biz-Engine. The partner ID is taken from the verified request context, never from the JSON body.

## Responses

- `201 Created`: a new event was persisted.
- `200 OK`: the same idempotency key and semantically identical request was already accepted; the original event ID is returned.
- `409 Conflict`: the idempotency key was already used with a different request body.
- `401 Unauthorized`: signature or credential verification failed.
- `403 Forbidden`: there is no active exact application-scope `EVENT_INGEST` grant, or policy denies it.
- `400 Bad Request`: malformed or invalid request.

## Authorization setup

Provision an active `PartnerAccessGrant` for the partner with:

- `scope_type = 'APPLICATION'`
- `scope_id = '<application UUID>'`
- `permission = 'EVENT_INGEST'`
- `effect = 'ALLOW'`
- `enabled = true`

A matching explicit `DENY` overrides an allow. For event ingestion, a generic `PARTNER` or `GLOBAL` grant is not enough; an exact application grant is required. Application registration and ownership FK are deferred to a later phase, so application IDs must be provisioned through a controlled process at this stage.

## Persistence and reliability boundary

PostgreSQL is authoritative. The `(partner_id, idempotency_key)` unique constraint and `INSERT ... ON CONFLICT DO NOTHING` make concurrent retries safe. The canonicalized request hash detects key reuse with different content. This phase does not publish to Kafka yet; Phase 19 will add the transactional outbox so facts are committed before asynchronous processing.

## Important limitations

- The `application_id` is not yet a foreign key because the application registry module is not implemented.
- No AI or derived insight is created during ingestion. This endpoint stores facts only.
- Event schema registry, retention/partitioning, payload size policy beyond the authenticated request body limit, and downstream projections are future work.

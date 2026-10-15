# Phase 20 — Entity, identity, context, and observation

## Responsibilities

- `biz_event` remains the immutable operational fact and source record.
- `biz_entity` represents a referable subject in a partner/application namespace.
- `biz_identifier` stores keyed hashes of identifiers, not raw values.
- `biz_context` stores a versioned normalized representation of event context.
- `biz_observation` records the event as processed and links the context and an entity when one can be resolved.

## Resolution rules

1. An event without a usable `subjectType` or `subjectReference` may still be processed. Its observation has no `entity_id`.
2. Entity lookup is constrained by partner, application, entity type, and external reference.
3. Do not merge entities across partner boundaries on names, email similarity, location, or model output alone.
4. Identifier registration starts at `UNVERIFIED`. A successful lookup is not equivalent to proof of identity.
5. Cross-application and cross-partner identity linking requires a separately governed linking process, evidence, authorization, and audit trail.

## Identifier privacy

`IdentifierHashService` applies HMAC-SHA256 to a normalized identifier type and value. Only the hash is stored. Configure `BIZ_IDENTITY_HASH_SECRET` to a strong stable secret held in a secret manager in non-local environments. Keep it stable for lookups; key rotation needs a versioned migration/re-hashing workflow. HMAC hashing is pseudonymization, not anonymization.

## Context

The original event context remains unchanged in `biz_event.context_data`. `ContextNormalizer` creates a separate JSONB representation and normalizes a small, explicit set of known dimensions. Unknown source fields are retained. More complex geography resolution and partner-specific schemas are future work.

## Consumer reliability

The consumer runs database writes in one transaction. `biz_observation.event_id` is unique, so Kafka redelivery does not create another observation. A crash after the database transaction commits but before the Kafka offset commits can cause redelivery; this is expected and handled through idempotency. Failed processing is retried by the listener error handler and then published to a `.DLQ` topic.

The consumer DLQ is distinct from the Phase 19 outbox publisher's `biz.events.dlq` topic, which handles failures publishing outbox messages to the main event topic.

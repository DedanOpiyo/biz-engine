# Phase 23 — Insights and Evidence

## Purpose

An insight is a deterministic interpretation of an observed trend. It is not a raw event, observation, or signal, and it does not establish causality. Phase 23 generates partner-scope insights from the latest eligible partner-scope trend snapshots.

## Initial rule

The seeded rules identify an increase or decrease when the trend has at least five signals and at least three distinct entities in the current window. These are initial engineering thresholds, not validated business or statistical thresholds. Rule changes must be reviewed and versioned through a Flyway migration.

The `confidence` field is a deterministic evidence-sufficiency score. It is **not** a probability that the interpretation is true, a significance test, or a causal claim. `evidenceQuality` is LOW, MEDIUM, or HIGH based on documented sample-size and entity-diversity thresholds.

## Lifecycle and deduplication

Insights are deduplicated by partner, scope, signal code, window, and rule code. Re-running generation updates the same insight and attaches the latest trend snapshot as evidence. When an opposite-direction insight becomes active, the earlier direction for the same partner/scope/signal/window is marked `SUPERSEDED`. Active insights expire when `expires_at` is reached. Evidence links are retained for audit and review.

## API

`GET /api/v1/insights?status=ACTIVE&limit=50`

- Requires the standard Ed25519 partner signature.
- The partner ID is taken from the authenticated server-side request context, never from a query parameter.
- `status` is optional and may be `ACTIVE`, `EXPIRED`, or `SUPERSEDED`.
- `limit` defaults to 50 and must be 1–100.
- The response includes evidence references to trend snapshots and their counts, time window, distinct entity count, direction, and timestamp.
- The initial endpoint exposes only the authenticated partner's own partner-scope insights. Broader scopes require explicit authorization and privacy/aggregation policies.

## Scheduled generation

The job re-evaluates eligible trend snapshots from the most recent eight days every 15 minutes by default. Candidate selection uses the newest eligible snapshot per partner/scope/signal/window. Database uniqueness and upserts make repeated runs idempotent.

Configuration:

- `BIZ_INSIGHT_GENERATION_ENABLED` (default `true`)
- `BIZ_INSIGHT_GENERATION_DELAY_MS` (default `900000`)

## Migration

`V9__create_insights_and_evidence.sql` creates the rule, insight, and evidence tables and seeds two version-1 directional rules.

## Limitations and next steps

- Initial rules are trend-based and deterministic; no AI is used to generate or authorize insights.
- Evidence currently points to trend snapshots. Later phases can add typed links to signals, observations, and events while preserving tenant boundaries.
- The score thresholds and rule thresholds require validation against real domain data before business-critical use.
- The endpoint is partner-scoped; application, regional, country, and global insights are not exposed yet.

# Phase 21 — Deterministic signal generation

## Purpose

Signals are reproducible derived records, not raw facts and not AI conclusions. Each signal references an observation and its originating event, plus a versioned deterministic rule.

## Starter rules

Migration `V7__create_deterministic_signals.sql` seeds conservative examples for `content.viewed`, `content.liked`, `content.bookmarked`, and `purchase.completed`. Unknown event types intentionally produce no signal. Rules are reviewed/versioned in Flyway migrations; partners and AI cannot silently author or change authoritative rules.

## Scope

The starter implementation emits `ENTITY` scope when an entity was resolved, and `APPLICATION` and `PARTNER` scopes for every matching rule. Country, region, domain, and cross-partner aggregation are intentionally not implemented until geography, visibility policy, and privacy-safe aggregation are ready.

## Evidence and idempotency

Every signal stores `observation_id`, `evidence_event_id`, `rule_code`, and `rule_version`. A unique constraint prevents duplicate signals for the same observation/rule/version/scope. Signal creation is invoked after a new observation is inserted, inside the same database transaction. A rollback therefore rolls back the observation and its signals together.

## Recency

`base_strength` is a rule-defined initial weight in [0, 1]. `half_life_hours` records the decay parameter, and `expires_at` defines the maximum lifetime. `SignalRecencyService.currentWeight` calculates:

`weight = baseStrength × 0.5 ^ (elapsedHours / halfLifeHours)`

Future-dated observations are treated as zero elapsed time rather than gaining weight. Consumers should ignore expired signals. The base record is immutable evidence of the original deterministic calculation; recency can be recomputed at query time.

## Run and validate

```bash
cp .env.example .env
docker compose up -d postgres kafka
mvn test
mvn spring-boot:run
```

Build and integration tests should be run in an environment with Java 21, Maven, Docker, and PostgreSQL/Kafka available. This archive was assembled but Maven tests were not executed in the generation environment.

## Limitations

- Starter rules use exact event-type matching, not fuzzy inference.
- Signal strength values are illustrative rule configuration and must be calibrated against real outcomes before product decisions rely on them.
- The initial model does not yet aggregate counts/trends across time windows or emit country/region signals.
- Signal records are generated from observations but no public signal-query API is exposed in this phase.

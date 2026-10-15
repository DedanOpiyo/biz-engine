# Phase 22 — Trend aggregation

## Read model

`biz_trend_snapshot` is a derived read model. Do not treat it as a source of truth; rebuild snapshots from `biz_signal` when needed. Each row describes one scope, signal code, window length, and snapshot time.

## Calculation

For each signal code and scope, the job compares:

- current window: `[snapshot_at - window, snapshot_at)`
- previous window: `[snapshot_at - 2 * window, snapshot_at - window)`

`current_signal_count` and `previous_signal_count` count signal rows in those windows. `current_distinct_entities` counts distinct non-null entity IDs in the current window. Weighted strength is the sum of each signal's `base_strength * 0.5^(age_seconds / half_life_seconds)` at the snapshot time.

`change_ratio = (current_count - previous_count) / previous_count` when the previous count is non-zero. Otherwise it is `NULL`. Direction is based on counts rather than weighted strength and does not imply causation.

Expired signals are still included if their `observed_at` falls within a comparison window. Their TTL determines whether they are active for other uses; it must not erase historical activity from trend comparisons.

## API and access boundary

`GET /api/v1/trends?windowHours=24&limit=50` requires the standard Ed25519 partner request signature. The authenticated partner ID is taken from the server-side request attribute, never from a caller-supplied `partnerId`. The initial API exposes only that partner's own `PARTNER` scope. Application and entity snapshots are calculated but are not exposed by this endpoint.

## Operational notes

- Default job delay: 900,000 ms (15 minutes).
- Windows: 1, 24, 168, and 720 hours.
- Migration: `V8__create_trend_aggregation.sql`.
- PostgreSQL remains authoritative for signals; snapshots are reproducible projections.
- Trend rows are created only for scope/signal combinations with at least one signal in the two-window lookback.
- Before adding country/region output, implement privacy thresholds, small-cell suppression, approved geographic mappings, and visibility policy enforcement.

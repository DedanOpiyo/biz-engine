# Recommendations API and lifecycle (Phase 24)

## Read recommendations

`GET /api/v1/recommendations?status=PROPOSED&limit=50`

The request must be signed using the Phase 16 Ed25519 partner authentication protocol. The authenticated partner must have an active `RECOMMENDATION_READ` grant scoped to its own partner ID. The partner ID is taken from verified authentication context, not from a query parameter.

Supported statuses: `PROPOSED`, `ACCEPTED`, `DISMISSED`, `EXPIRED`, `SUPERSEDED`. The first implementation exposes read-only retrieval; controlled accept/dismiss endpoints are intentionally deferred until decision and feedback semantics are designed.

## Generation rules

The scheduled generator selects active insights that have not expired, whose confidence is at least `0.55`, and whose evidence quality is `MEDIUM` or `HIGH`. It creates a deterministic review prompt based on the insight direction:

- `UP`: review the context behind the increase and compare with the previous period.
- `DOWN`: check data quality and relevant product/content changes before deciding whether intervention is warranted.

These are review prompts, not causal claims. They do not authorize or execute any partner action.

## Priority

The starter priority rule assigns `HIGH` only when confidence is at least `0.80` and evidence quality is `HIGH`; `MEDIUM` when confidence is at least `0.65`; otherwise `LOW`. These thresholds are initial engineering defaults and require calibration before production use.

## Idempotency and lifecycle

The database unique key `(partner_id, source_insight_id, recommendation_code)` prevents duplicate recommendations for the same insight/rule. Reevaluation refreshes the rationale and expiry. Existing `ACCEPTED` and `DISMISSED` statuses are preserved; other matching records return to `PROPOSED`. Proposed recommendations expire at the earlier of the source insight expiry and seven days from evaluation.

## Boundary

Recommendation generation is not policy approval, a decision, an action, or an outcome. A future phase should add explicit policy evaluation and decision records before any action can be executed. Partner systems remain responsible for application-specific execution.

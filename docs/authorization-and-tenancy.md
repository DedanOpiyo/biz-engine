# Phase 17 — Authorization and tenant isolation

## What this phase adds

- Explicit permission names for partner operations (event ingestion, context writes, intelligence reads, feedback writes, analytics reads, and administration).
- Scope types: partner, application, domain, department, country, region, and global.
- Persisted partner access grants with `ALLOW` / `DENY`, enabled state, creation time, and optional expiry.
- Fail-closed authorization service: no grant means denied; an applicable `DENY` overrides an `ALLOW`.
- Tenant isolation independent of grants: a partner grant cannot authorize access to another partner's tenant.
- Consistent API error responses for authorization denials.
- Flyway migration `V3__create_partner_access_grants.sql`.

## Security boundary

Phase 16 proves the caller's partner identity. Phase 17 evaluates whether that authenticated partner may perform a requested operation in a scope. These are separate steps. Callers must never be allowed to choose their own authenticated partner ID; take it from `PartnerAuthenticationFilter.AUTHENTICATED_PARTNER_ID_ATTRIBUTE` after successful signature verification.

`PartnerAuthorizationService.requirePermission(...)` requires both the authenticated partner and the resource-owning partner. It rejects cross-tenant access before checking grants. `GLOBAL` means broad scope **inside the authenticated partner's own tenant**, not permission to read every partner's data.

## Grant matching

- `APPLICATION`, `DOMAIN`, `DEPARTMENT`, `COUNTRY`, and `REGION` grants match the requested scope type and exact scope ID.
- A `PARTNER` grant matches that partner's own UUID and can be used as a tenant-wide grant for a permission.
- `GLOBAL` with scope ID `*` matches any requested scope inside the same partner tenant.
- Expired and disabled grants do not apply.
- If matching grants contain a `DENY`, the request is denied even if an `ALLOW` also matches.
- If no matching `ALLOW` exists, the request is denied.

## Provisioning grants

No public grant-management endpoint is exposed in this phase. Grant provisioning is intentionally an operator-controlled deployment/administrative task until a separate operator identity and authorization model exists. Never give a partner endpoint the ability to create its own grants.

Example for a known partner UUID and application scope ID:

```sql
INSERT INTO partner_access_grant
  (id, partner_id, scope_type, scope_id, permission, effect, enabled, created_at, expires_at)
VALUES
  (gen_random_uuid(), '<partner-uuid>', 'APPLICATION', 'interhub',
   'EVENT_INGEST', 'ALLOW', TRUE, now(), NULL);
```

Ensure the PostgreSQL deployment provides `gen_random_uuid()` (available in supported modern PostgreSQL versions). Replace placeholders with validated values. Keep grant changes auditable in the deployment process.

## Integration pattern for upcoming modules

Before accepting an event for an application, the event service should:

1. Read the authenticated partner ID from the trusted request context.
2. Resolve the application and verify it belongs to that partner.
3. Call `requirePermission(partnerId, applicationOwnerPartnerId, EVENT_INGEST, APPLICATION, applicationId)`.
4. Only then validate idempotency and persist the event and outbox row in one transaction.

Do not treat an `applicationId`, `country`, or `scopeId` supplied by the client as proof of authorization. Resolve ownership server-side.

## Known limits

- This phase supplies the policy model and reusable enforcement service; it does not yet wire grants into event or intelligence endpoints because those APIs are introduced in later phases.
- Operator identities, role administration, ABAC attributes, purpose-of-processing checks, and policy change audit workflows remain future work.
- The grant service uses relational queries and in-memory evaluation; if grant volume grows, move matching logic into indexed queries and add cache invalidation only with a defined consistency model.

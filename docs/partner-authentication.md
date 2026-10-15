# Partner request authentication (Phase 16)

## Protocol

Partner-facing `/api/v1/partners/**` requests are authenticated with Ed25519 signatures. TLS/HTTPS is required in every non-local environment. The partner keeps its Ed25519 private key; Biz-Engine stores only the public key in `partner_credential`.

Required headers:

- `X-Biz-Partner-Id`: registered partner UUID
- `X-Biz-Key-Id`: active key ID provisioned for that partner
- `X-Biz-Timestamp`: Unix epoch seconds
- `X-Biz-Nonce`: fresh random URL-safe token, 16–128 characters (`A-Z`, `a-z`, `0-9`, `_`, `-`)
- `X-Biz-Signature`: URL-safe Base64 without padding of the Ed25519 signature

The signature is over this UTF-8 canonical message, with LF (`\n`) separators and no final newline:

```text
BIZ-ENGINE-REQUEST-V1
{partnerId}
{keyId}
{timestamp}
{nonce}
{HTTP method in uppercase}
{raw request URI plus ?query if present}
{lowercase hex SHA-256 of exact HTTP body bytes}
```

An empty body hashes as SHA-256 of zero bytes. The signature must cover the exact bytes sent; JSON must not be reformatted after signing. The timestamp tolerance defaults to 300 seconds. Nonces are unique per partner, stored transactionally in PostgreSQL, and removed after 15 minutes. A nonce is consumed only after signature verification succeeds.

## Generate a key pair

Use OpenSSL on the partner's secure machine. Never upload or send `partner-private.pem` to Biz-Engine.

```bash
openssl genpkey -algorithm ED25519 -out partner-private.pem
openssl pkey -in partner-private.pem -pubout -out partner-public.pem
chmod 600 partner-private.pem
```

Keep `partner-private.pem` in a secret manager or protected key store, not in source control, logs, container images, or client applications.

## Provisioning is an operator task in Phase 16

Credential enrollment endpoints are intentionally not exposed yet. An operator should create the partner and public-key credential using an authenticated internal deployment/DB workflow. Example only; replace all placeholders and run using a restricted database role:

```sql
INSERT INTO partner (id, code, name, status, created_at)
VALUES ('<partner-uuid>', 'EXAMPLE_PARTNER', 'Example Partner', 'ACTIVE', now());

INSERT INTO partner_credential
    (id, partner_id, key_id, algorithm, public_key_pem, status, created_at, activated_at)
VALUES
    (gen_random_uuid(), '<partner-uuid>', 'example-key-2026-01', 'ED25519',
     $pem$-----BEGIN PUBLIC KEY-----
REPLACE_WITH_LINES_FROM_partner-public.pem
-----END PUBLIC KEY-----$pem$,
     'ACTIVE', now(), now());
```

The PEM block must be the exact public key file contents. Do not use this placeholder SQL unchanged. Ensure the database role, audit trail, backups, and operational access are controlled.

## Partner operations currently allowed

After a valid signature, a partner may retrieve its own record using `GET /api/v1/partners/{its-own-uuid}`. Other partner-management operations (registration, listing all partners, and status changes) return `403 OPERATOR_ACCESS_REQUIRED`; they must be exposed only after operator authorization is implemented. Phase 17 expands authorization and tenant isolation.

The system status endpoint is intentionally public and does not accept partner identity as proof of anything.

## Python signing example

Requires `cryptography` (`python -m pip install cryptography`). This is illustrative client code; use a production HTTP client with TLS verification enabled.

```python
import base64, hashlib, secrets, time
from cryptography.hazmat.primitives import serialization

partner_id = "<partner-uuid>"
key_id = "example-key-2026-01"
method = "GET"
path_and_query = f"/api/v1/partners/{partner_id}"
body = b""
timestamp = str(int(time.time()))
nonce = secrets.token_urlsafe(24).replace("=", "")
body_hash = hashlib.sha256(body).hexdigest()
canonical = "\n".join([
    "BIZ-ENGINE-REQUEST-V1", partner_id, key_id, timestamp, nonce,
    method, path_and_query, body_hash
]).encode("utf-8")

with open("partner-private.pem", "rb") as f:
    private_key = serialization.load_pem_private_key(f.read(), password=None)
signature = base64.urlsafe_b64encode(private_key.sign(canonical)).rstrip(b"=").decode("ascii")
headers = {
    "X-Biz-Partner-Id": partner_id,
    "X-Biz-Key-Id": key_id,
    "X-Biz-Timestamp": timestamp,
    "X-Biz-Nonce": nonce,
    "X-Biz-Signature": signature,
}
# Send exactly `body` to exactly `path_and_query`, with these headers over HTTPS.
```

## Operational notes

- Revocation: set the credential `status` to `REVOKED` and populate `revoked_at`; changes take effect on the next request.
- Rotation: provision a second active key ID, deploy it to the partner, confirm use, then revoke the old key. Do not reuse key IDs.
- Partner suspension/revocation disables its credentials immediately at authentication time.
- The request body limit defaults to 1 MiB. Configure `BIZ_PARTNER_MAX_BODY_BYTES` carefully.
- `BIZ_PARTNER_AUTH_ENABLED=false` exists for isolated local tests only. Do not disable authentication in shared or production environments.
- This phase is not a complete production security program: Phase 17 must add operator authentication/authorization, broader tenant-scoped access, and policy enforcement before additional partner-facing routes are exposed.

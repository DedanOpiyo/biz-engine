CREATE TABLE partner_access_grant (
                                      id UUID PRIMARY KEY,
                                      partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE RESTRICT,
                                      scope_type VARCHAR(24) NOT NULL,
                                      scope_id VARCHAR(200) NOT NULL,
                                      permission VARCHAR(40) NOT NULL,
                                      effect VARCHAR(10) NOT NULL,
                                      enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                      created_at TIMESTAMPTZ NOT NULL,
                                      expires_at TIMESTAMPTZ,
                                      CONSTRAINT chk_partner_access_scope_type CHECK
                                          (scope_type IN ('PARTNER', 'APPLICATION', 'DOMAIN', 'DEPARTMENT', 'COUNTRY', 'REGION', 'GLOBAL')),
                                      CONSTRAINT chk_partner_access_permission CHECK
                                          (permission IN ('PARTNER_READ', 'PARTNER_ADMIN', 'CREDENTIAL_MANAGE', 'EVENT_INGEST', 'CONTEXT_WRITE',
                                                          'INTELLIGENCE_READ', 'RECOMMENDATION_READ', 'FEEDBACK_WRITE', 'ANALYTICS_READ')),
                                      CONSTRAINT chk_partner_access_effect CHECK (effect IN ('ALLOW', 'DENY')),
                                      CONSTRAINT chk_partner_access_expiry CHECK (expires_at IS NULL OR expires_at > created_at)
);
CREATE INDEX idx_partner_access_grant_lookup
    ON partner_access_grant(partner_id, permission, enabled, scope_type, scope_id);
CREATE INDEX idx_partner_access_grant_expiry
    ON partner_access_grant(expires_at) WHERE expires_at IS NOT NULL;

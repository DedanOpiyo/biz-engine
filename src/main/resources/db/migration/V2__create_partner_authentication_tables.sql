CREATE TABLE partner_credential (
                                    id UUID PRIMARY KEY,
                                    partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE RESTRICT,
                                    key_id VARCHAR(100) NOT NULL UNIQUE,
                                    algorithm VARCHAR(30) NOT NULL,
                                    public_key_pem TEXT NOT NULL,
                                    status VARCHAR(20) NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL,
                                    activated_at TIMESTAMPTZ,
                                    revoked_at TIMESTAMPTZ,
                                    expires_at TIMESTAMPTZ,
                                    CONSTRAINT chk_partner_credential_algorithm CHECK (algorithm IN ('ED25519')),
                                    CONSTRAINT chk_partner_credential_status CHECK (status IN ('ACTIVE', 'REVOKED')),
                                    CONSTRAINT chk_partner_credential_dates CHECK ((status = 'ACTIVE' AND revoked_at IS NULL) OR (status = 'REVOKED' AND revoked_at IS NOT NULL))
);
CREATE INDEX idx_partner_credential_partner_status ON partner_credential(partner_id, status);

CREATE TABLE partner_request_nonce (
                                       id BIGSERIAL PRIMARY KEY,
                                       partner_id UUID NOT NULL REFERENCES partner(id) ON DELETE CASCADE,
                                       nonce VARCHAR(128) NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL,
                                       CONSTRAINT uk_partner_request_nonce UNIQUE (partner_id, nonce)
);
CREATE INDEX idx_partner_request_nonce_created_at ON partner_request_nonce(created_at);

CREATE TABLE partner (
                         id UUID PRIMARY KEY,
                         code VARCHAR(100) NOT NULL UNIQUE,
                         name VARCHAR(255) NOT NULL,
                         status VARCHAR(30) NOT NULL,
                         created_at TIMESTAMPTZ NOT NULL,
                         CONSTRAINT chk_partner_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);
CREATE INDEX idx_partner_status ON partner(status);

CREATE TABLE biz_event (
                           id UUID PRIMARY KEY,
                           partner_id UUID NOT NULL REFERENCES partner(id),
                           application_id UUID NOT NULL,
                           idempotency_key VARCHAR(128) NOT NULL,
                           request_hash VARCHAR(64) NOT NULL,
                           event_type VARCHAR(120) NOT NULL,
                           schema_version INTEGER NOT NULL CHECK (schema_version >= 1),
                           occurred_at TIMESTAMPTZ NOT NULL,
                           subject_type VARCHAR(100),
                           subject_reference VARCHAR(200),
                           context_data JSONB,
                           payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
                           received_at TIMESTAMPTZ NOT NULL,
                           CONSTRAINT uk_biz_event_partner_idempotency UNIQUE (partner_id, idempotency_key)
);

CREATE INDEX idx_biz_event_partner_received ON biz_event(partner_id, received_at DESC);
CREATE INDEX idx_biz_event_application_occurred ON biz_event(application_id, occurred_at DESC);
CREATE INDEX idx_biz_event_type_occurred ON biz_event(event_type, occurred_at DESC);

CREATE TABLE biz_signal_rule (
                                 id UUID PRIMARY KEY,
                                 rule_code VARCHAR(100) NOT NULL UNIQUE,
                                 rule_version INTEGER NOT NULL CHECK (rule_version >= 1),
                                 event_type VARCHAR(120) NOT NULL,
                                 signal_code VARCHAR(120) NOT NULL,
                                 base_strength NUMERIC(8,5) NOT NULL CHECK (base_strength >= 0 AND base_strength <= 1),
                                 half_life_hours INTEGER NOT NULL CHECK (half_life_hours > 0),
                                 ttl_hours INTEGER NOT NULL CHECK (ttl_hours > 0),
                                 active BOOLEAN NOT NULL DEFAULT TRUE,
                                 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                 CONSTRAINT uk_biz_signal_rule_version UNIQUE (rule_code, rule_version)
);
CREATE INDEX idx_biz_signal_rule_event_active ON biz_signal_rule(event_type, active);

CREATE TABLE biz_signal (
                            id UUID PRIMARY KEY,
                            observation_id UUID NOT NULL REFERENCES biz_observation(id),
                            evidence_event_id UUID NOT NULL REFERENCES biz_event(id),
                            partner_id UUID NOT NULL REFERENCES partner(id),
                            application_id UUID NOT NULL,
                            entity_id UUID,
                            scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('ENTITY', 'APPLICATION', 'PARTNER')),
                            scope_id UUID NOT NULL,
                            signal_code VARCHAR(120) NOT NULL,
                            rule_code VARCHAR(100) NOT NULL,
                            rule_version INTEGER NOT NULL CHECK (rule_version >= 1),
                            base_strength NUMERIC(8,5) NOT NULL CHECK (base_strength >= 0 AND base_strength <= 1),
                            half_life_hours INTEGER NOT NULL CHECK (half_life_hours > 0),
                            signal_data JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(signal_data) = 'object'),
                            observed_at TIMESTAMPTZ NOT NULL,
                            generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                            expires_at TIMESTAMPTZ NOT NULL,
                            CONSTRAINT uk_biz_signal_observation_rule_scope UNIQUE (observation_id, rule_code, rule_version, scope_type, scope_id),
                            CONSTRAINT fk_biz_signal_entity_scope FOREIGN KEY (entity_id, partner_id, application_id)
                                REFERENCES biz_entity(id, partner_id, application_id)
);
CREATE INDEX idx_biz_signal_scope_time ON biz_signal(scope_type, scope_id, observed_at DESC);
CREATE INDEX idx_biz_signal_partner_code_time ON biz_signal(partner_id, signal_code, observed_at DESC);
CREATE INDEX idx_biz_signal_expiry ON biz_signal(expires_at);

-- Conservative starter rules. Extend through reviewed migrations/configuration, not arbitrary AI output.
INSERT INTO biz_signal_rule (id, rule_code, rule_version, event_type, signal_code, base_strength, half_life_hours, ttl_hours)
VALUES
    ('21000000-0000-4000-8000-000000000001', 'content-viewed', 1, 'content.viewed', 'CONTENT_VIEW', 0.35000, 24, 168),
    ('21000000-0000-4000-8000-000000000002', 'content-liked', 1, 'content.liked', 'CONTENT_LIKE', 0.70000, 72, 720),
    ('21000000-0000-4000-8000-000000000003', 'content-bookmarked', 1, 'content.bookmarked', 'CONTENT_BOOKMARK', 0.80000, 168, 2160),
    ('21000000-0000-4000-8000-000000000004', 'purchase-completed', 1, 'purchase.completed', 'PURCHASE_COMPLETED', 1.00000, 336, 4320)
    ON CONFLICT (rule_code, rule_version) DO NOTHING;

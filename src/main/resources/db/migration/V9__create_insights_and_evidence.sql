CREATE TABLE biz_insight_rule (
                                  id UUID PRIMARY KEY,
                                  rule_code VARCHAR(100) NOT NULL,
                                  rule_version INTEGER NOT NULL CHECK (rule_version >= 1),
                                  signal_code VARCHAR(120) NOT NULL,
                                  direction VARCHAR(10) NOT NULL CHECK (direction IN ('UP', 'DOWN')),
                                  minimum_signal_count BIGINT NOT NULL CHECK (minimum_signal_count >= 1),
                                  minimum_distinct_entities BIGINT NOT NULL CHECK (minimum_distinct_entities >= 1),
                                  active BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  CONSTRAINT uk_biz_insight_rule_version UNIQUE (rule_code, rule_version)
);

CREATE TABLE biz_insight (
                             id UUID PRIMARY KEY,
                             partner_id UUID NOT NULL REFERENCES partner(id),
                             scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('PARTNER')),
                             scope_id UUID NOT NULL,
                             signal_code VARCHAR(120) NOT NULL,
                             window_hours INTEGER NOT NULL CHECK (window_hours IN (1, 24, 168, 720)),
                             rule_code VARCHAR(100) NOT NULL,
                             rule_version INTEGER NOT NULL CHECK (rule_version >= 1),
                             title VARCHAR(240) NOT NULL,
                             summary TEXT NOT NULL,
                             direction VARCHAR(10) NOT NULL CHECK (direction IN ('UP', 'DOWN')),
                             confidence NUMERIC(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
                             evidence_quality VARCHAR(10) NOT NULL CHECK (evidence_quality IN ('LOW', 'MEDIUM', 'HIGH')),
                             status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'SUPERSEDED')),
                             source_trend_id UUID NOT NULL REFERENCES biz_trend_snapshot(id),
                             first_observed_at TIMESTAMPTZ NOT NULL,
                             last_evaluated_at TIMESTAMPTZ NOT NULL,
                             expires_at TIMESTAMPTZ NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                             CONSTRAINT uk_biz_insight_dedupe UNIQUE (partner_id, scope_type, scope_id, signal_code, window_hours, rule_code)
);
CREATE INDEX idx_biz_insight_partner_status ON biz_insight(partner_id, status, updated_at DESC);
CREATE INDEX idx_biz_insight_expiry ON biz_insight(status, expires_at);

CREATE TABLE biz_insight_evidence (
                                      id UUID PRIMARY KEY,
                                      insight_id UUID NOT NULL REFERENCES biz_insight(id) ON DELETE CASCADE,
                                      trend_snapshot_id UUID NOT NULL REFERENCES biz_trend_snapshot(id),
                                      evidence_role VARCHAR(30) NOT NULL CHECK (evidence_role IN ('PRIMARY_TREND')),
                                      recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                      CONSTRAINT uk_biz_insight_evidence UNIQUE (insight_id, trend_snapshot_id, evidence_role)
);
CREATE INDEX idx_biz_insight_evidence_insight ON biz_insight_evidence(insight_id, recorded_at DESC);

INSERT INTO biz_insight_rule
(id, rule_code, rule_version, signal_code, direction, minimum_signal_count, minimum_distinct_entities)
VALUES
    ('23000000-0000-4000-8000-000000000001', 'SIGNAL_ACTIVITY_INCREASE', 1, '*', 'UP', 5, 3),
    ('23000000-0000-4000-8000-000000000002', 'SIGNAL_ACTIVITY_DECREASE', 1, '*', 'DOWN', 5, 3)
    ON CONFLICT (rule_code, rule_version) DO NOTHING;

-- Event-level evidence is retained separately so an insight can be traced through
-- signal -> observation -> originating event without treating a trend as raw evidence.
CREATE TABLE biz_insight_source_evidence (
                                             id UUID PRIMARY KEY,
                                             insight_id UUID NOT NULL REFERENCES biz_insight(id) ON DELETE CASCADE,
                                             signal_id UUID NOT NULL REFERENCES biz_signal(id),
                                             observation_id UUID NOT NULL REFERENCES biz_observation(id),
                                             event_id UUID NOT NULL REFERENCES biz_event(id),
                                             recorded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                             CONSTRAINT uk_biz_insight_source_signal UNIQUE (insight_id, signal_id)
);
CREATE INDEX idx_biz_insight_source_evidence_insight ON biz_insight_source_evidence(insight_id, recorded_at DESC);

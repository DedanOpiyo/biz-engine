CREATE TABLE biz_trend_snapshot (
                                    id UUID PRIMARY KEY,
                                    scope_type VARCHAR(20) NOT NULL CHECK (scope_type IN ('ENTITY', 'APPLICATION', 'PARTNER')),
                                    scope_id UUID NOT NULL,
                                    partner_id UUID NOT NULL REFERENCES partner(id),
                                    signal_code VARCHAR(120) NOT NULL,
                                    window_hours INTEGER NOT NULL CHECK (window_hours IN (1, 24, 168, 720)),
                                    snapshot_at TIMESTAMPTZ NOT NULL,
                                    current_window_start TIMESTAMPTZ NOT NULL,
                                    previous_window_start TIMESTAMPTZ NOT NULL,
                                    current_signal_count BIGINT NOT NULL CHECK (current_signal_count >= 0),
                                    previous_signal_count BIGINT NOT NULL CHECK (previous_signal_count >= 0),
                                    current_distinct_entities BIGINT NOT NULL CHECK (current_distinct_entities >= 0),
                                    current_weighted_strength NUMERIC(18,6) NOT NULL CHECK (current_weighted_strength >= 0),
                                    previous_weighted_strength NUMERIC(18,6) NOT NULL CHECK (previous_weighted_strength >= 0),
                                    change_ratio NUMERIC(18,6),
                                    direction VARCHAR(10) NOT NULL CHECK (direction IN ('UP', 'DOWN', 'STABLE')),
                                    algorithm_version INTEGER NOT NULL DEFAULT 1 CHECK (algorithm_version >= 1),
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    CONSTRAINT uk_biz_trend_snapshot UNIQUE (scope_type, scope_id, signal_code, window_hours, snapshot_at)
);
CREATE INDEX idx_biz_trend_partner_latest ON biz_trend_snapshot(partner_id, window_hours, snapshot_at DESC);
CREATE INDEX idx_biz_trend_scope_latest ON biz_trend_snapshot(scope_type, scope_id, signal_code, window_hours, snapshot_at DESC);

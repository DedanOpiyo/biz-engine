CREATE TABLE biz_recommendation (
                                    id UUID PRIMARY KEY,
                                    partner_id UUID NOT NULL REFERENCES partner(id),
                                    source_insight_id UUID NOT NULL REFERENCES biz_insight(id),
                                    recommendation_code VARCHAR(100) NOT NULL,
                                    objective VARCHAR(100) NOT NULL,
                                    title VARCHAR(240) NOT NULL,
                                    rationale TEXT NOT NULL,
                                    suggested_action TEXT NOT NULL,
                                    priority VARCHAR(10) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
                                    confidence NUMERIC(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
                                    evidence_quality VARCHAR(10) NOT NULL CHECK (evidence_quality IN ('LOW', 'MEDIUM', 'HIGH')),
                                    status VARCHAR(20) NOT NULL CHECK (status IN ('PROPOSED', 'ACCEPTED', 'DISMISSED', 'EXPIRED', 'SUPERSEDED')),
                                    valid_until TIMESTAMPTZ NOT NULL,
                                    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                    CONSTRAINT uk_biz_recommendation_dedupe UNIQUE (partner_id, source_insight_id, recommendation_code)
);
CREATE INDEX idx_biz_recommendation_partner_status ON biz_recommendation(partner_id, status, updated_at DESC);
CREATE INDEX idx_biz_recommendation_expiry ON biz_recommendation(status, valid_until);

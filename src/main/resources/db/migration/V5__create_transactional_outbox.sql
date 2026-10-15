CREATE TABLE biz_outbox_event (
                                  id UUID PRIMARY KEY,
                                  aggregate_id UUID NOT NULL REFERENCES biz_event(id) ON DELETE RESTRICT,
                                  event_type VARCHAR(120) NOT NULL,
                                  topic VARCHAR(249) NOT NULL,
                                  message_key VARCHAR(249) NOT NULL,
                                  payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
                                  status VARCHAR(16) NOT NULL DEFAULT 'PENDING'
                                      CHECK (status IN ('PENDING', 'PUBLISHED', 'DEAD')),
                                  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
                                  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                  published_at TIMESTAMPTZ,
                                  dead_lettered_at TIMESTAMPTZ,
                                  last_error VARCHAR(2000),
                                  CONSTRAINT ck_outbox_terminal_timestamps CHECK (
                                      (status <> 'PUBLISHED' OR published_at IS NOT NULL)
                                          AND (status <> 'DEAD' OR dead_lettered_at IS NOT NULL)
                                      )
);

CREATE INDEX idx_biz_outbox_pending ON biz_outbox_event(next_attempt_at, created_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_biz_outbox_aggregate ON biz_outbox_event(aggregate_id, created_at);

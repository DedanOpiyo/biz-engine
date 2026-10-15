CREATE TABLE biz_entity (
                            id UUID PRIMARY KEY,
                            partner_id UUID NOT NULL REFERENCES partner(id),
                            application_id UUID NOT NULL,
                            entity_type VARCHAR(100) NOT NULL,
                            external_reference VARCHAR(200) NOT NULL,
                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'MERGED', 'DELETED')),
                            created_at TIMESTAMPTZ NOT NULL,
                            updated_at TIMESTAMPTZ NOT NULL,
                            CONSTRAINT uk_biz_entity_external_ref UNIQUE (partner_id, application_id, entity_type, external_reference),
                            CONSTRAINT uk_biz_entity_id_partner_app UNIQUE (id, partner_id, application_id),
                            CONSTRAINT uk_biz_entity_id_partner UNIQUE (id, partner_id)
);
CREATE INDEX idx_biz_entity_partner_application ON biz_entity(partner_id, application_id);

-- Raw identifier values (such as email addresses) are deliberately not stored.
-- HMAC-SHA256 hashes are keyed with a deployment secret and scoped by partner.
CREATE TABLE biz_identifier (
                                id UUID PRIMARY KEY,
                                entity_id UUID NOT NULL,
                                partner_id UUID NOT NULL REFERENCES partner(id),
                                identifier_type VARCHAR(60) NOT NULL,
                                identifier_hash VARCHAR(64) NOT NULL,
                                verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED'
                                    CHECK (verification_status IN ('UNVERIFIED', 'VERIFIED', 'REVOKED')),
                                created_at TIMESTAMPTZ NOT NULL,
                                verified_at TIMESTAMPTZ,
                                revoked_at TIMESTAMPTZ,
                                CONSTRAINT uk_biz_identifier_partner_type_hash UNIQUE (partner_id, identifier_type, identifier_hash),
                                CONSTRAINT fk_biz_identifier_entity_partner FOREIGN KEY (entity_id, partner_id) REFERENCES biz_entity(id, partner_id)
);
CREATE INDEX idx_biz_identifier_entity ON biz_identifier(entity_id);

CREATE TABLE biz_context (
                             id UUID PRIMARY KEY,
                             event_id UUID NOT NULL UNIQUE REFERENCES biz_event(id),
                             partner_id UUID NOT NULL REFERENCES partner(id),
                             application_id UUID NOT NULL,
                             context_version INTEGER NOT NULL DEFAULT 1 CHECK (context_version >= 1),
                             normalized_context JSONB NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(normalized_context) = 'object'),
                             normalization_status VARCHAR(20) NOT NULL DEFAULT 'NORMALIZED'
                                 CHECK (normalization_status IN ('NORMALIZED', 'PARTIAL', 'UNAVAILABLE')),
                             created_at TIMESTAMPTZ NOT NULL,
                             CONSTRAINT uk_biz_context_id_partner_app UNIQUE (id, partner_id, application_id)
);
CREATE INDEX idx_biz_context_partner_application ON biz_context(partner_id, application_id, created_at DESC);

CREATE TABLE biz_observation (
                                 id UUID PRIMARY KEY,
                                 event_id UUID NOT NULL UNIQUE REFERENCES biz_event(id),
                                 entity_id UUID REFERENCES biz_entity(id),
                                 context_id UUID NOT NULL REFERENCES biz_context(id),
                                 partner_id UUID NOT NULL REFERENCES partner(id),
                                 application_id UUID NOT NULL,
                                 observation_type VARCHAR(120) NOT NULL,
                                 observed_at TIMESTAMPTZ NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,
                                 processing_version INTEGER NOT NULL DEFAULT 1 CHECK (processing_version >= 1),
                                 CONSTRAINT fk_biz_observation_entity_scope FOREIGN KEY (entity_id, partner_id, application_id)
                                     REFERENCES biz_entity(id, partner_id, application_id),
                                 CONSTRAINT fk_biz_observation_context_scope FOREIGN KEY (context_id, partner_id, application_id)
                                     REFERENCES biz_context(id, partner_id, application_id)
);
CREATE INDEX idx_biz_observation_entity_time ON biz_observation(entity_id, observed_at DESC);
CREATE INDEX idx_biz_observation_partner_time ON biz_observation(partner_id, observed_at DESC);

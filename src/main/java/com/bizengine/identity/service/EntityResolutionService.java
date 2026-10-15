package com.bizengine.identity.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Resolves partner-scoped external subject references; it never merges across tenants. */
@Service
public class EntityResolutionService {
    private final JdbcTemplate jdbcTemplate;

    public EntityResolutionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID resolveOrCreate(UUID partnerId, UUID applicationId, String entityType, String externalReference) {
        if (partnerId == null || applicationId == null || entityType == null || entityType.isBlank()
                || externalReference == null || externalReference.isBlank()) {
            return null;
        }
        String safeType = entityType.trim().toLowerCase(java.util.Locale.ROOT);
        String safeReference = externalReference.trim();
        List<UUID> inserted = jdbcTemplate.query("""
                INSERT INTO biz_entity (id, partner_id, application_id, entity_type, external_reference,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                ON CONFLICT (partner_id, application_id, entity_type, external_reference) DO NOTHING
                RETURNING id
                """, (rs, rowNum) -> rs.getObject(1, UUID.class), UUID.randomUUID(), partnerId,
                applicationId, safeType, safeReference, Instant.now(), Instant.now());
        if (!inserted.isEmpty()) return inserted.get(0);
        return jdbcTemplate.queryForObject("""
                SELECT id FROM biz_entity
                WHERE partner_id = ? AND application_id = ? AND entity_type = ? AND external_reference = ?
                  AND status = 'ACTIVE'
                """, UUID.class, partnerId, applicationId, safeType, safeReference);
    }
}

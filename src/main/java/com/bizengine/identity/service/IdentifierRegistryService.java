package com.bizengine.identity.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentifierRegistryService {
    private final JdbcTemplate jdbcTemplate;
    private final IdentifierHashService hashService;

    public IdentifierRegistryService(JdbcTemplate jdbcTemplate, IdentifierHashService hashService) {
        this.jdbcTemplate = jdbcTemplate;
        this.hashService = hashService;
    }

    @Transactional
    public UUID register(UUID partnerId, UUID entityId, String type, String value) {
        if (partnerId == null || entityId == null) throw new IllegalArgumentException("Partner and entity are required.");
        String normalizedType = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedType.isBlank()) throw new IllegalArgumentException("Identifier type is required.");
        String hash = hashService.hash(normalizedType, value);
        Integer ownedEntity = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM biz_entity WHERE id = ? AND partner_id = ?", Integer.class, entityId, partnerId);
        if (ownedEntity == null || ownedEntity != 1) {
            throw new IllegalArgumentException("Entity does not belong to the specified partner.");
        }
        List<UUID> rows = jdbcTemplate.query("""
                INSERT INTO biz_identifier (id, entity_id, partner_id, identifier_type, identifier_hash,
                    verification_status, created_at)
                SELECT ?, e.id, e.partner_id, ?, ?, 'UNVERIFIED', ?
                FROM biz_entity e WHERE e.id = ? AND e.partner_id = ?
                ON CONFLICT (partner_id, identifier_type, identifier_hash) DO NOTHING
                RETURNING id
                """, (rs, rowNum) -> rs.getObject(1, UUID.class), UUID.randomUUID(), normalizedType,
                hash, Instant.now(), entityId, partnerId);
        if (!rows.isEmpty()) return rows.get(0);
        List<IdentifierOwner> existing = jdbcTemplate.query("""
                SELECT id, entity_id FROM biz_identifier
                WHERE partner_id = ? AND identifier_type = ? AND identifier_hash = ?
                """, (rs, rowNum) -> new IdentifierOwner(rs.getObject("id", UUID.class),
                rs.getObject("entity_id", UUID.class)), partnerId, normalizedType, hash);
        if (existing.isEmpty()) throw new IllegalStateException("Identifier registration did not persist.");
        if (!existing.get(0).entityId().equals(entityId)) {
            throw new IllegalStateException("Identifier is already assigned to a different entity in this partner scope.");
        }
        return existing.get(0).id();
    }

    private record IdentifierOwner(UUID id, UUID entityId) { }

    public Optional<UUID> findEntity(UUID partnerId, String type, String value) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        String hash = hashService.hash(normalizedType, value);
        List<UUID> ids = jdbcTemplate.query("""
                SELECT i.entity_id FROM biz_identifier i
                JOIN biz_entity e ON e.id = i.entity_id
                WHERE i.partner_id = ? AND i.identifier_type = ? AND i.identifier_hash = ?
                  AND i.verification_status <> 'REVOKED' AND e.status = 'ACTIVE'
                """, (rs, rowNum) -> rs.getObject(1, UUID.class), partnerId, normalizedType, hash);
        if (ids.size() > 1) throw new IllegalStateException("Identifier uniqueness invariant violated.");
        return ids.stream().findFirst();
    }
}

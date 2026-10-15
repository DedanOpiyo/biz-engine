package com.bizengine.auth.repository;

import com.bizengine.auth.domain.PartnerRequestNonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface PartnerRequestNonceRepository extends JpaRepository<PartnerRequestNonce, Long> {
    @Modifying
    @Query(value = "INSERT INTO partner_request_nonce(partner_id, nonce, created_at) VALUES (:partnerId, :nonce, :createdAt) ON CONFLICT (partner_id, nonce) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("partnerId") UUID partnerId, @Param("nonce") String nonce,
                       @Param("createdAt") Instant createdAt);

    int deleteByCreatedAtBefore(Instant cutoff);
}

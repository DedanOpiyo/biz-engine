package com.bizengine.auth.repository;

import com.bizengine.auth.domain.PartnerCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerCredentialRepository extends JpaRepository<PartnerCredential, UUID> {
    Optional<PartnerCredential> findByPartner_IdAndKeyId(UUID partnerId, String keyId);
    boolean existsByKeyId(String keyId);
}

package com.bizengine.businessuser.repository;

import com.bizengine.businessuser.model.ExternalPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalPartnerRepository extends JpaRepository<ExternalPartner, Long> {
    Optional<ExternalPartner> findByIssuerAndActiveTrue(String issuer);
}

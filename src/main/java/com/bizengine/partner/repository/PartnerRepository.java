package com.bizengine.partner.repository;

import com.bizengine.partner.domain.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    boolean existsByCode(String code);;
    Optional<Partner> findByCode(String code);
}
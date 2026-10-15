package com.bizengine.authorization.repository;

import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.PartnerAccessGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerAccessGrantRepository extends JpaRepository<PartnerAccessGrant, UUID> {
    List<PartnerAccessGrant> findByPartner_IdAndPermissionAndEnabledTrue(UUID partnerId,
                                                                         AuthorizationPermission permission);
}

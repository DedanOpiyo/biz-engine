package com.bizengine.businessuser.repository;

import com.bizengine.businessuser.model.BusinessUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BusinessUserRepository extends JpaRepository<BusinessUser, Long> {
    Optional<BusinessUser> findByExternalUserId(String externalUserId);
    java.util.Optional<BusinessUser> findByEmail(String email);
}

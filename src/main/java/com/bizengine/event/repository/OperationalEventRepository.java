package com.bizengine.event.repository;

import com.bizengine.event.domain.OperationalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperationalEventRepository extends JpaRepository<OperationalEvent, UUID> {
    Optional<OperationalEvent> findByPartnerIdAndIdempotencyKey(UUID partnerId, String idempotencyKey);
}

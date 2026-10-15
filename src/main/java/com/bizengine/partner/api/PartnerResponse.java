package com.bizengine.partner.api;

import com.bizengine.partner.domain.Partner;

import java.time.Instant;
import java.util.UUID;

public record PartnerResponse(
        UUID id,
        String code,
        String name,
        String status,
        Instant createdAt
) {

    public static PartnerResponse from(Partner partner) {
        return new PartnerResponse(
                partner.getId(),
                partner.getCode(),
                partner.getName(),
                partner.getStatus().name(),
                partner.getCreatedAt()
        );
    }
}
package com.bizengine.partner.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record PartnerPageResponse(List<PartnerResponse> content, int page, int size,
                                  long totalElements, int totalPages) {
    public static PartnerPageResponse from(Page<PartnerResponse> page) {
        return new PartnerPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}

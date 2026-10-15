package com.bizengine.partner.service;

import com.bizengine.partner.api.PartnerPageResponse;
import com.bizengine.partner.api.PartnerResponse;
import com.bizengine.partner.domain.Partner;
import com.bizengine.partner.domain.PartnerStatus;
import com.bizengine.partner.repository.PartnerRepository;
import com.bizengine.shared.error.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    @Transactional
    public Partner register(String code, String name) {

        partnerRepository.findByCode(code)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Partner already exists: " + code
                    );
                });

        Partner partner = new Partner(code, name);

        return partnerRepository.save(partner);
    }

    @Transactional(readOnly = true)
    public PartnerResponse get(UUID id) {
        return partnerRepository.findById(id).map(PartnerResponse::from)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional(readOnly = true)
    public PartnerPageResponse list(int page, int size) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be zero or greater.");
        if (size < 1 || size > 100) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE", "Page size must be between 1 and 100.");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PartnerResponse> result = partnerRepository.findAll(pageable).map(PartnerResponse::from);
        return PartnerPageResponse.from(result);
    }

    @Transactional
    public PartnerResponse changeStatus(UUID id, PartnerStatus status) {
        Partner partner = partnerRepository.findById(id).orElseThrow(() -> notFound(id));
        if (partner.getStatus() == PartnerStatus.REVOKED && status != PartnerStatus.REVOKED) {
            throw new ApiException(HttpStatus.CONFLICT, "PARTNER_REVOKED",
                    "A revoked partner cannot be reactivated. Register a new partner instead.");
        }
        partner.changeStatus(status);
        return PartnerResponse.from(partnerRepository.save(partner));
    }

    private ApiException notFound(UUID id) {
        return new ApiException(HttpStatus.NOT_FOUND, "PARTNER_NOT_FOUND", "No partner exists with id " + id + ".");
    }
}
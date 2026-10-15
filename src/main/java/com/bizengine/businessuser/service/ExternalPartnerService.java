package com.bizengine.businessuser.service;

import com.bizengine.businessuser.dto.EnlistPartnerRequest;
import com.bizengine.businessuser.model.ExternalPartner;
import com.bizengine.businessuser.repository.ExternalPartnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ExternalPartnerService {

    private final ExternalPartnerRepository repository;

    public ExternalPartnerService(ExternalPartnerRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ExternalPartner enlistPartner(EnlistPartnerRequest request) {
        // Enforce uniqueness on issuer to prevent security conflicts
        repository.findByIssuerAndActiveTrue(request.getIssuer()).ifPresent(p -> {
            throw new IllegalArgumentException("An active partner system with this issuer is already registered.");
        });

        ExternalPartner partner = new ExternalPartner();
        partner.setIssuer(request.getIssuer());
        partner.setJwkSetUri(request.getJwkSetUri());
        partner.setPartnerName(request.getPartnerName());
        partner.setActive(true);

        return repository.save(partner);
    }

    @Transactional(readOnly = true)
    public List<ExternalPartner> getAllActivePartners() {
        return repository.findAll().stream().filter(ExternalPartner::isActive).toList();
    }

    @Transactional
    public void revokePartnerAccess(Long id) {
        ExternalPartner partner = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Partner registration profile not found."));
        partner.setActive(false);
        repository.save(partner);
    }
}

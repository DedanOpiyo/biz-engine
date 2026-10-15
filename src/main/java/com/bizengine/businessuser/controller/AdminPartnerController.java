package com.bizengine.businessuser.controller;

import com.bizengine.businessuser.dto.EnlistPartnerRequest;
import com.bizengine.businessuser.model.ExternalPartner;
import com.bizengine.businessuser.service.ExternalPartnerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/partners")
public class AdminPartnerController {

    private final ExternalPartnerService partnerService;

    public AdminPartnerController(ExternalPartnerService partnerService) {
        this.partnerService = partnerService;
    }

    // 🛡️ Safe: SecurityConfig blocks non-admins before this logic is ever reached
    @PostMapping
    public ResponseEntity<ExternalPartner> enlistNewPartner(
            @Valid @RequestBody EnlistPartnerRequest request) { // 🟢 Add @Valid here
        ExternalPartner savedPartner = partnerService.enlistPartner(request);
        return ResponseEntity.ok(savedPartner);
    }

    @GetMapping
    public ResponseEntity<List<ExternalPartner>> listActivePartners() {
        return ResponseEntity.ok(partnerService.getAllActivePartners());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokePartner(@PathVariable Long id) {
        partnerService.revokePartnerAccess(id);
        return ResponseEntity.noContent().build();
    }
}

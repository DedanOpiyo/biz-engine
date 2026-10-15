package com.bizengine.partner.api;

import com.bizengine.partner.domain.Partner;
import com.bizengine.partner.service.PartnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
public class PartnerController {

    private final PartnerService partnerService;

    public PartnerController(PartnerService partnerService) {
        this.partnerService = partnerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartnerResponse create(
            @Valid @RequestBody CreatePartnerRequest request
    ) {

        Partner partner = partnerService.register(
                request.code(),
                request.name()
        );

        return PartnerResponse.from(partner);
    }

    @GetMapping("/{id}")
    public PartnerResponse get(@PathVariable UUID id) { return partnerService.get(id); }

    @GetMapping
    public PartnerPageResponse list(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return partnerService.list(page, size);
    }

    @PatchMapping("/{id}/status")
    public PartnerResponse changeStatus(@PathVariable UUID id,
                                        @Valid @RequestBody ChangePartnerStatusRequest request) {
        return partnerService.changeStatus(id, request.status());
    }
}
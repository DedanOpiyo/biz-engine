package com.bizengine.insight.api;

import com.bizengine.auth.web.PartnerAuthenticationFilter;
import com.bizengine.insight.domain.InsightStatus;
import com.bizengine.insight.repository.InsightRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {
    private final InsightRepository repository;

    public InsightController(InsightRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InsightResponse> list(
            @RequestParam(required = false) InsightStatus status,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100.");
        }
        Object value = request.getAttribute(PartnerAuthenticationFilter.AUTHENTICATED_PARTNER_ID_ATTRIBUTE);
        if (!(value instanceof UUID partnerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated partner context is missing.");
        }
        return repository.findForPartner(partnerId, status, limit).stream().map(InsightResponse::from).toList();
    }
}

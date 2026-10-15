package com.bizengine.trend.api;

import com.bizengine.auth.web.PartnerAuthenticationFilter;
import com.bizengine.trend.service.TrendAggregationService;
import com.bizengine.trend.repository.TrendRepository;
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
@RequestMapping("/api/v1/trends")
public class TrendController {
    private final TrendRepository repository;

    public TrendController(TrendRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TrendResponse> listPartnerTrends(
            @RequestParam(defaultValue = "24") int windowHours,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        if (!TrendAggregationService.SUPPORTED_WINDOWS_HOURS.contains(windowHours)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "windowHours must be one of 1, 24, 168, or 720.");
        }
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100.");
        }
        Object attribute = request.getAttribute(PartnerAuthenticationFilter.AUTHENTICATED_PARTNER_ID_ATTRIBUTE);
        if (!(attribute instanceof UUID partnerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated partner context is missing.");
        }
        return repository.findLatestForPartner(partnerId, windowHours, limit).stream()
                .map(TrendResponse::from).toList();
    }
}

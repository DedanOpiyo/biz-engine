package com.bizengine.recommendation.api;

import com.bizengine.auth.web.PartnerAuthenticationFilter;
import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.AuthorizationScopeType;
import com.bizengine.authorization.service.PartnerAuthorizationService;
import com.bizengine.recommendation.domain.RecommendationStatus;
import com.bizengine.recommendation.repository.RecommendationRepository;
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
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationRepository repository;
    private final PartnerAuthorizationService authorization;

    public RecommendationController(RecommendationRepository repository, PartnerAuthorizationService authorization) {
        this.repository = repository;
        this.authorization = authorization;
    }

    @GetMapping
    public List<RecommendationResponse> list(@RequestParam(required = false) RecommendationStatus status,
                                             @RequestParam(defaultValue = "50") int limit,
                                             HttpServletRequest request) {
        if (limit < 1 || limit > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 100.");
        }
        Object value = request.getAttribute(PartnerAuthenticationFilter.AUTHENTICATED_PARTNER_ID_ATTRIBUTE);
        if (!(value instanceof UUID partnerId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated partner context is missing.");
        }
        authorization.requirePermission(partnerId, partnerId, AuthorizationPermission.RECOMMENDATION_READ,
                AuthorizationScopeType.PARTNER, partnerId.toString());
        return repository.findForPartner(partnerId, status, limit).stream().map(RecommendationResponse::from).toList();
    }
}

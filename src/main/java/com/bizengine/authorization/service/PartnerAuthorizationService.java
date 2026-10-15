package com.bizengine.authorization.service;

import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.AuthorizationScopeType;
import com.bizengine.authorization.domain.GrantEffect;
import com.bizengine.authorization.domain.PartnerAccessGrant;
import com.bizengine.authorization.repository.PartnerAccessGrantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fail-closed authorization policy for partner-scoped resources.
 * The authenticated partner ID must come from the Phase 16 authentication context,
 * never from a caller-controlled request body or query parameter.
 */
@Service
public class PartnerAuthorizationService {
    private final PartnerAccessGrantRepository grants;

    public PartnerAuthorizationService(PartnerAccessGrantRepository grants) {
        this.grants = grants;
    }

    @Transactional(readOnly = true)
    public void requirePermission(UUID authenticatedPartnerId, UUID resourceOwnerPartnerId,
                                  AuthorizationPermission permission,
                                  AuthorizationScopeType requestedScopeType, String requestedScopeId) {
        if (authenticatedPartnerId == null) {
            throw new AuthorizationDeniedException("AUTHENTICATION_REQUIRED", "An authenticated partner is required.");
        }
        if (resourceOwnerPartnerId == null || !authenticatedPartnerId.equals(resourceOwnerPartnerId)) {
            // A partner grant never authorizes access to another partner's tenant.
            throw new AuthorizationDeniedException("TENANT_ACCESS_DENIED", "Cross-partner resource access is denied.");
        }
        if (permission == null || requestedScopeType == null || requestedScopeId == null || requestedScopeId.isBlank()) {
            throw new AuthorizationDeniedException("AUTHORIZATION_DENIED", "The requested permission or scope is invalid.");
        }

        List<PartnerAccessGrant> active = grants
                .findByPartner_IdAndPermissionAndEnabledTrue(authenticatedPartnerId, permission)
                .stream()
                .filter(g -> g.getExpiresAt() == null || g.getExpiresAt().isAfter(Instant.now()))
                .filter(g -> scopeMatches(g, requestedScopeType, requestedScopeId))
                .toList();

        // Explicit DENY always overrides any matching ALLOW.
        if (active.stream().anyMatch(g -> g.getEffect() == GrantEffect.DENY)) {
            throw new AuthorizationDeniedException("POLICY_DENIED", "An explicit policy denies this operation.");
        }
        if (active.stream().noneMatch(g -> g.getEffect() == GrantEffect.ALLOW)) {
            throw new AuthorizationDeniedException("AUTHORIZATION_DENIED", "No active grant permits this operation.");
        }
    }


    /**
     * Requires a grant on the exact requested scope. Unlike general scope checks,
     * this deliberately does not let PARTNER or GLOBAL grants authorize ingestion
     * into an arbitrary application identifier.
     */
    @Transactional(readOnly = true)
    public void requireExactScopePermission(UUID authenticatedPartnerId, UUID resourceOwnerPartnerId,
                                            AuthorizationPermission permission,
                                            AuthorizationScopeType requestedScopeType, String requestedScopeId) {
        if (authenticatedPartnerId == null) {
            throw new AuthorizationDeniedException("AUTHENTICATION_REQUIRED", "An authenticated partner is required.");
        }
        if (resourceOwnerPartnerId == null || !authenticatedPartnerId.equals(resourceOwnerPartnerId)) {
            throw new AuthorizationDeniedException("TENANT_ACCESS_DENIED", "Cross-partner resource access is denied.");
        }
        if (permission == null || requestedScopeType == null || requestedScopeId == null || requestedScopeId.isBlank()) {
            throw new AuthorizationDeniedException("AUTHORIZATION_DENIED", "The requested permission or scope is invalid.");
        }
        List<PartnerAccessGrant> active = grants
                .findByPartner_IdAndPermissionAndEnabledTrue(authenticatedPartnerId, permission)
                .stream()
                .filter(g -> g.getExpiresAt() == null || g.getExpiresAt().isAfter(Instant.now()))
                .toList();
        // Broader DENY grants remain applicable, but only an exact-scope ALLOW can authorize ingestion.
        if (active.stream().anyMatch(g -> g.getEffect() == GrantEffect.DENY
                && scopeMatches(g, requestedScopeType, requestedScopeId))) {
            throw new AuthorizationDeniedException("POLICY_DENIED", "An explicit policy denies this operation.");
        }
        if (active.stream().noneMatch(g -> g.getEffect() == GrantEffect.ALLOW
                && g.getScopeType() == requestedScopeType && requestedScopeId.equals(g.getScopeId()))) {
            throw new AuthorizationDeniedException("AUTHORIZATION_DENIED", "No active exact-scope grant permits this operation.");
        }
    }

    private boolean scopeMatches(PartnerAccessGrant grant, AuthorizationScopeType requestedType, String requestedId) {
        if (grant.getScopeType() == AuthorizationScopeType.GLOBAL) {
            return "*".equals(grant.getScopeId());
        }
        if (grant.getScopeType() == AuthorizationScopeType.PARTNER) {
            return grant.getScopeId().equals(grant.getPartner().getId().toString());
        }
        return grant.getScopeType() == requestedType && Objects.equals(grant.getScopeId(), requestedId);
    }
}

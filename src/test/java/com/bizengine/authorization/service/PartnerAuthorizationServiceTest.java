package com.bizengine.authorization.service;

import com.bizengine.authorization.domain.AuthorizationPermission;
import com.bizengine.authorization.domain.AuthorizationScopeType;
import com.bizengine.authorization.domain.GrantEffect;
import com.bizengine.authorization.domain.PartnerAccessGrant;
import com.bizengine.authorization.repository.PartnerAccessGrantRepository;
import com.bizengine.partner.domain.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerAuthorizationServiceTest {
    @Mock PartnerAccessGrantRepository repository;
    PartnerAuthorizationService service;
    UUID partnerId;
    Partner partner;

    @BeforeEach
    void setUp() {
        service = new PartnerAuthorizationService(repository);
        partnerId = UUID.randomUUID();
        partner = new Partner("test-partner", "Test Partner");
    }

    @Test
    void deniesWhenNoGrantExists() {
        when(repository.findByPartner_IdAndPermissionAndEnabledTrue(partnerId, AuthorizationPermission.EVENT_INGEST))
                .thenReturn(List.of());
        AuthorizationDeniedException ex = assertThrows(AuthorizationDeniedException.class, () ->
                service.requirePermission(partnerId, partnerId, AuthorizationPermission.EVENT_INGEST,
                        AuthorizationScopeType.APPLICATION, "app-1"));
        assertEquals("AUTHORIZATION_DENIED", ex.getCode());
    }

    @Test
    void deniesCrossTenantEvenIfPartnerHasBroadGrant() {
        AuthorizationDeniedException ex = assertThrows(AuthorizationDeniedException.class, () ->
                service.requirePermission(partnerId, UUID.randomUUID(), AuthorizationPermission.EVENT_INGEST,
                        AuthorizationScopeType.GLOBAL, "*"));
        assertEquals("TENANT_ACCESS_DENIED", ex.getCode());
    }

    @Test
    void matchingActiveAllowPermitsOperation() {
        PartnerAccessGrant grant = new PartnerAccessGrant(partner, AuthorizationScopeType.APPLICATION, "app-1",
                AuthorizationPermission.EVENT_INGEST, GrantEffect.ALLOW, true, null);
        when(repository.findByPartner_IdAndPermissionAndEnabledTrue(partnerId, AuthorizationPermission.EVENT_INGEST))
                .thenReturn(List.of(grant));
        // The service checks the grant's owning partner ID for PARTNER scope only; app scope is an exact match.
        assertDoesNotThrow(() -> service.requirePermission(partnerId, partnerId, AuthorizationPermission.EVENT_INGEST,
                AuthorizationScopeType.APPLICATION, "app-1"));
    }

    @Test
    void explicitDenyOverridesAllow() {
        PartnerAccessGrant allow = new PartnerAccessGrant(partner, AuthorizationScopeType.APPLICATION, "app-1",
                AuthorizationPermission.EVENT_INGEST, GrantEffect.ALLOW, true, null);
        PartnerAccessGrant deny = new PartnerAccessGrant(partner, AuthorizationScopeType.APPLICATION, "app-1",
                AuthorizationPermission.EVENT_INGEST, GrantEffect.DENY, true, null);
        when(repository.findByPartner_IdAndPermissionAndEnabledTrue(partnerId, AuthorizationPermission.EVENT_INGEST))
                .thenReturn(List.of(allow, deny));
        AuthorizationDeniedException ex = assertThrows(AuthorizationDeniedException.class, () ->
                service.requirePermission(partnerId, partnerId, AuthorizationPermission.EVENT_INGEST,
                        AuthorizationScopeType.APPLICATION, "app-1"));
        assertEquals("POLICY_DENIED", ex.getCode());
    }
}

package com.bizengine.authorization.domain;

import com.bizengine.partner.domain.Partner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_access_grant")
public class PartnerAccessGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 24)
    private AuthorizationScopeType scopeType;

    // Scope IDs intentionally remain strings because scope targets are polymorphic.
    @Column(name = "scope_id", nullable = false, length = 200)
    private String scopeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuthorizationPermission permission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GrantEffect effect;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected PartnerAccessGrant() { }

    public PartnerAccessGrant(Partner partner, AuthorizationScopeType scopeType, String scopeId,
                              AuthorizationPermission permission, GrantEffect effect,
                              boolean enabled, Instant expiresAt) {
        this.partner = partner;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.permission = permission;
        this.effect = effect;
        this.enabled = enabled;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public Partner getPartner() { return partner; }
    public AuthorizationScopeType getScopeType() { return scopeType; }
    public String getScopeId() { return scopeId; }
    public AuthorizationPermission getPermission() { return permission; }
    public GrantEffect getEffect() { return effect; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}

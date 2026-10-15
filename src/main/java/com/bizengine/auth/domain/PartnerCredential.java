package com.bizengine.auth.domain;

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
import jakarta.persistence.UniqueConstraint;
import com.bizengine.partner.domain.Partner;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_credential", uniqueConstraints =
@UniqueConstraint(name = "partner_credential_key_id_key", columnNames = "key_id"))
public class PartnerCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private Partner partner;

    @Column(name = "key_id", nullable = false, length = 100)
    private String keyId;

    @Column(nullable = false, length = 30)
    private String algorithm;

    @Column(name = "public_key_pem", nullable = false, columnDefinition = "text")
    private String publicKeyPem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CredentialStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected PartnerCredential() { }

    public PartnerCredential(Partner partner, String keyId, String publicKeyPem, Instant expiresAt) {
        this.partner = partner;
        this.keyId = keyId;
        this.algorithm = "ED25519";
        this.publicKeyPem = publicKeyPem;
        this.status = CredentialStatus.ACTIVE;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.status = CredentialStatus.REVOKED;
        this.revokedAt = Instant.now();
    }

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = CredentialStatus.ACTIVE;
        if (activatedAt == null && status == CredentialStatus.ACTIVE) activatedAt = createdAt;
    }

    public UUID getId() { return id; }
    public Partner getPartner() { return partner; }
    public String getKeyId() { return keyId; }
    public String getAlgorithm() { return algorithm; }
    public String getPublicKeyPem() { return publicKeyPem; }
    public CredentialStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getExpiresAt() { return expiresAt; }
}

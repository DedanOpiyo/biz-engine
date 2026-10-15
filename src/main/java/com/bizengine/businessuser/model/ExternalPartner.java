package com.bizengine.businessuser.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_partners")
public class ExternalPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique Identifier string (Matches the "iss" claim inside their JWT)
    @Column(name = "issuer", nullable = false, unique = true)
    private String issuer;

    // The remote HTTP URL where we fetch their valid public keys (JWKS URL)
    @Column(name = "jwk_set_uri", nullable = false)
    private String jwkSetUri;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getJwkSetUri() { return jwkSetUri; }
    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

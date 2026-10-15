package com.bizengine.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner_request_nonce")
public class PartnerRequestNonce {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(nullable = false, length = 128)
    private String nonce;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PartnerRequestNonce() { }
}

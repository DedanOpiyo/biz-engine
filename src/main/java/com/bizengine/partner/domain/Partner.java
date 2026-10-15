package com.bizengine.partner.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "partner")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Partner() {
    }

    public Partner(String code, String name) {
        this.code = code;
        this.name = name;
        this.status = PartnerStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = PartnerStatus.ACTIVE;
    }

    public void changeStatus(PartnerStatus newStatus) {
        if (newStatus == null) throw new IllegalArgumentException("Partner status is required.");
        this.status = newStatus;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public PartnerStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
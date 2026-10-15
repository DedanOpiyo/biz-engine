package com.bizengine.partner.api;

import com.bizengine.partner.domain.PartnerStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePartnerStatusRequest(@NotNull(message = "Partner status is required.") PartnerStatus status) { }

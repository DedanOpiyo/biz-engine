package com.bizengine.partner.api;

import jakarta.validation.constraints.NotBlank;

public record CreatePartnerRequest(

        @NotBlank
        String code,

        @NotBlank
        String name
) {
}
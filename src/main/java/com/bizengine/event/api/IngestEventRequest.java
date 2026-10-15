package com.bizengine.event.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record IngestEventRequest(
        @NotNull UUID applicationId,
        @NotBlank @Size(max = 120)
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._:-]*", message = "must be a simple event type code")
        String eventType,
        @NotNull @Min(1) Integer schemaVersion,
        @NotNull Instant occurredAt,
        @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9._:-]*") String subjectType,
        @Size(max = 200) String subjectReference,
        JsonNode context,
        @NotNull JsonNode payload
) {
}

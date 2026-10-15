package com.bizengine.context.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Conservative normalization: preserve source fields and add standardized fields only when present. */
@Component
public class ContextNormalizer {
    private final ObjectMapper objectMapper;

    public ContextNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode normalize(JsonNode source) {
        ObjectNode result = source != null && source.isObject()
                ? ((ObjectNode) source).deepCopy() : objectMapper.createObjectNode();
        copyText(result, "country", "countryCode", true);
        copyText(result, "region", "region", false);
        copyText(result, "county", "county", false);
        copyText(result, "subRegion", "subRegion", false);
        copyText(result, "domain", "domain", true);
        copyText(result, "surface", "surface", true);
        return result;
    }

    private void copyText(ObjectNode result, String sourceKey, String targetKey, boolean lowerOrUpper) {
        JsonNode value = result.get(sourceKey);
        if (value == null || !value.isTextual() || value.asText().isBlank()) return;
        String normalized = value.asText().trim();
        if (sourceKey.equals("country")) normalized = normalized.toUpperCase(Locale.ROOT);
        else if (lowerOrUpper) normalized = normalized.toLowerCase(Locale.ROOT);
        result.put(targetKey, normalized);
    }
}

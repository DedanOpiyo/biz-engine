package com.bizengine.context.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextNormalizerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ContextNormalizer normalizer = new ContextNormalizer(objectMapper);

    @Test
    void preservesSourceFieldsAndNormalizesKnownDimensions() throws Exception {
        var source = objectMapper.readTree("{\"country\":\" ke \",\"surface\":\" FEED \",\"customField\":7}");
        var result = normalizer.normalize(source);
        assertEquals("KE", result.get("countryCode").asText());
        assertEquals("feed", result.get("surface").asText());
        assertEquals(7, result.get("customField").asInt());
    }

    @Test
    void nullContextBecomesEmptyObject() {
        assertTrue(normalizer.normalize(null).isObject());
        assertEquals(0, normalizer.normalize(null).size());
    }
}

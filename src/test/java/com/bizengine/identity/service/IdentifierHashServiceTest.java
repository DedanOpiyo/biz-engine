package com.bizengine.identity.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierHashServiceTest {
    private final IdentifierHashService service = new IdentifierHashService("test-secret-that-is-at-least-32-characters-long");

    @Test
    void emailNormalizationMakesCaseAndOuterWhitespaceEquivalent() {
        assertEquals(service.hash("email", " Alice@Example.com "), service.hash("email", "alice@example.com"));
    }

    @Test
    void identifierTypesAreDomainSeparated() {
        assertNotEquals(service.hash("email", "12345"), service.hash("phone", "12345"));
    }

    @Test
    void rawIdentifierIsNotReturned() {
        String raw = "alice@example.com";
        assertNotEquals(raw, service.hash("email", raw));
        assertEquals(64, service.hash("email", raw).length());
    }

    @Test
    void rejectsBlankIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> service.hash("email", " "));
    }
}

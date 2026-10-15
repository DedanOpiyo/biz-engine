package com.bizengine.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/** Hashes normalized identifier values without persisting the original PII. */
@Service
public class IdentifierHashService {
    private final byte[] secret;

    public IdentifierHashService(@Value("${biz.identity.hash-secret:local-development-only-change-this-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("biz.identity.hash-secret must be at least 32 characters.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String identifierType, String rawValue) {
        if (identifierType == null || identifierType.isBlank() || rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Identifier type and value are required.");
        }
        String type = identifierType.trim().toLowerCase(Locale.ROOT);
        String normalized = normalize(type, rawValue);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((type + ":" + normalized).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash identifier.", ex);
        }
    }

    private String normalize(String type, String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC);
        if (type.equals("email")) return normalized.toLowerCase(Locale.ROOT);
        if (type.equals("phone")) return normalized.replaceAll("[\\s().-]", "");
        if (type.equals("external_user_id") || type.equals("partner_subject")) return normalized;
        return normalized.toLowerCase(Locale.ROOT);
    }
}

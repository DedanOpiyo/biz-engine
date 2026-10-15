package com.bizengine.auth.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerRequestAuthenticatorTest {
    @Test
    void canonicalRequestUsesStableOrderedFieldsAndBodyHash() {
        UUID partnerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String canonical = PartnerRequestAuthenticator.canonicalRequest(partnerId, "key-01", "1780000000",
                "random_nonce_123456", "get", "/api/v1/partners/11111111-1111-1111-1111-111111111111",
                new byte[0]);
        assertEquals("BIZ-ENGINE-REQUEST-V1\n" + partnerId + "\nkey-01\n1780000000\nrandom_nonce_123456\nGET\n" +
                "/api/v1/partners/11111111-1111-1111-1111-111111111111\n" +
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", canonical);
    }

    @Test
    void canonicalMessageCanBeSignedWithEd25519AndUrlSafeBase64() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String canonical = "BIZ-ENGINE-REQUEST-V1\nexample";
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        signer.update(canonical.getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());

        Signature verifier = Signature.getInstance("Ed25519");
        verifier.initVerify(pair.getPublic());
        verifier.update(canonical.getBytes(StandardCharsets.UTF_8));
        assertTrue(verifier.verify(Base64.getUrlDecoder().decode(encoded)));
    }
}

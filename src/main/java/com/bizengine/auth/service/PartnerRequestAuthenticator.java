package com.bizengine.auth.service;

import com.bizengine.auth.domain.CredentialStatus;
import com.bizengine.auth.domain.PartnerCredential;
import com.bizengine.auth.repository.PartnerCredentialRepository;
import com.bizengine.auth.repository.PartnerRequestNonceRepository;
import com.bizengine.partner.domain.Partner;
import com.bizengine.partner.domain.PartnerStatus;
import com.bizengine.partner.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PartnerRequestAuthenticator {
    public static final String PARTNER_ID_HEADER = "X-Biz-Partner-Id";
    public static final String KEY_ID_HEADER = "X-Biz-Key-Id";
    public static final String TIMESTAMP_HEADER = "X-Biz-Timestamp";
    public static final String NONCE_HEADER = "X-Biz-Nonce";
    public static final String SIGNATURE_HEADER = "X-Biz-Signature";

    private static final Pattern NONCE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{16,128}");
    private final PartnerCredentialRepository credentialRepository;
    private final PartnerRequestNonceRepository nonceRepository;
    private final PartnerRepository partnerRepository;
    private final long allowedClockSkewSeconds;

    public PartnerRequestAuthenticator(PartnerCredentialRepository credentialRepository,
                                       PartnerRequestNonceRepository nonceRepository,
                                       PartnerRepository partnerRepository,
                                       @Value("${biz.security.partner-authentication.allowed-clock-skew-seconds:300}")
                                       long allowedClockSkewSeconds) {
        this.credentialRepository = credentialRepository;
        this.nonceRepository = nonceRepository;
        this.partnerRepository = partnerRepository;
        this.allowedClockSkewSeconds = allowedClockSkewSeconds;
    }

    @Transactional
    public UUID authenticate(UUID partnerId, String keyId, String timestampHeader, String nonce,
                             String signatureHeader, String method, String pathAndQuery, byte[] body) {
        if (partnerId == null || blank(keyId) || blank(timestampHeader) || blank(nonce) || blank(signatureHeader)) {
            throw unauthorized("AUTHENTICATION_FAILED", "Partner authentication headers are required.");
        }
        if (!NONCE_PATTERN.matcher(nonce).matches()) {
            throw unauthorized("INVALID_NONCE", "The request nonce has an invalid format.");
        }

        final long epochSeconds;
        try {
            epochSeconds = Long.parseLong(timestampHeader);
        } catch (NumberFormatException ex) {
            throw unauthorized("INVALID_TIMESTAMP", "The request timestamp is invalid.");
        }
        long now = Instant.now().getEpochSecond();
        if (epochSeconds < now - allowedClockSkewSeconds || epochSeconds > now + allowedClockSkewSeconds) {
            throw unauthorized("STALE_REQUEST", "The request timestamp is outside the allowed time window.");
        }

        PartnerCredential credential = credentialRepository.findByPartner_IdAndKeyId(partnerId, keyId)
                .orElseThrow(() -> unauthorized("AUTHENTICATION_FAILED", "Partner credentials are invalid."));
        if (credential.getStatus() != CredentialStatus.ACTIVE ||
                (credential.getExpiresAt() != null && !credential.getExpiresAt().isAfter(Instant.now()))) {
            throw unauthorized("CREDENTIAL_INACTIVE", "The partner credential is not active.");
        }
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> unauthorized("AUTHENTICATION_FAILED", "Partner credentials are invalid."));
        if (partner.getStatus() != PartnerStatus.ACTIVE) {
            throw unauthorized("PARTNER_INACTIVE", "The partner is not active.");
        }

        String canonical = canonicalRequest(partnerId, keyId, timestampHeader, nonce, method, pathAndQuery, body);
        if (!verifySignature(credential, canonical, signatureHeader)) {
            throw unauthorized("AUTHENTICATION_FAILED", "Partner credentials are invalid.");
        }

        int inserted = nonceRepository.insertIfAbsent(partnerId, nonce, Instant.now());
        if (inserted != 1) {
            throw unauthorized("REPLAY_DETECTED", "This request nonce has already been used.");
        }
        return partnerId;
    }

    public static String canonicalRequest(UUID partnerId, String keyId, String timestamp, String nonce,
                                          String method, String pathAndQuery, byte[] body) {
        return "BIZ-ENGINE-REQUEST-V1\n" + partnerId + "\n" + keyId + "\n" + timestamp + "\n" + nonce + "\n" +
                method.toUpperCase() + "\n" + pathAndQuery + "\n" + sha256Hex(body);
    }

    private boolean verifySignature(PartnerCredential credential, String canonical, String signatureHeader) {
        try {
            byte[] keyBytes = decodePem(credential.getPublicKeyPem());
            PublicKey publicKey = KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(keyBytes));
            byte[] signatureBytes = Base64.getUrlDecoder().decode(signatureHeader);
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(canonical.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] decodePem(String pem) {
        String content = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(content);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes == null ? new byte[0] : bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private static PartnerAuthenticationException unauthorized(String code, String message) {
        return new PartnerAuthenticationException(HttpStatus.UNAUTHORIZED, code, message);
    }
}

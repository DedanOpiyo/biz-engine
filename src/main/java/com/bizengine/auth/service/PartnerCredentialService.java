package com.bizengine.auth.service;

import com.bizengine.auth.domain.CredentialStatus;
import com.bizengine.auth.domain.PartnerCredential;
import com.bizengine.auth.repository.PartnerCredentialRepository;
import com.bizengine.partner.domain.Partner;
import com.bizengine.partner.domain.PartnerStatus;
import com.bizengine.partner.repository.PartnerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PartnerCredentialService {
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{3,100}");
    private final PartnerRepository partnerRepository;
    private final PartnerCredentialRepository credentialRepository;

    public PartnerCredentialService(PartnerRepository partnerRepository,
                                    PartnerCredentialRepository credentialRepository) {
        this.partnerRepository = partnerRepository;
        this.credentialRepository = credentialRepository;
    }

    /** Internal operator workflow only. Do not expose directly as a public partner endpoint. */
    @Transactional
    public PartnerCredential provision(UUID partnerId, String keyId, String publicKeyPem, Instant expiresAt) {
        if (keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new PartnerAuthenticationException(HttpStatus.BAD_REQUEST, "INVALID_KEY_ID",
                    "Key ID must be 3-100 characters using letters, digits, dot, underscore, colon, or hyphen.");
        }
        if (credentialRepository.existsByKeyId(keyId)) {
            throw new PartnerAuthenticationException(HttpStatus.CONFLICT, "DUPLICATE_KEY_ID",
                    "This key ID is already registered.");
        }
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new PartnerAuthenticationException(HttpStatus.BAD_REQUEST, "INVALID_EXPIRY",
                    "Credential expiry must be in the future.");
        }
        validateEd25519PublicKey(publicKeyPem);
        Partner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new PartnerAuthenticationException(HttpStatus.NOT_FOUND,
                        "PARTNER_NOT_FOUND", "The partner does not exist."));
        if (partner.getStatus() != PartnerStatus.ACTIVE) {
            throw new PartnerAuthenticationException(HttpStatus.CONFLICT, "PARTNER_INACTIVE",
                    "Credentials can be provisioned only for an active partner.");
        }
        return credentialRepository.save(new PartnerCredential(partner, keyId, publicKeyPem, expiresAt));
    }

    @Transactional
    public void revoke(UUID partnerId, String keyId) {
        PartnerCredential credential = credentialRepository.findByPartner_IdAndKeyId(partnerId, keyId)
                .orElseThrow(() -> new PartnerAuthenticationException(HttpStatus.NOT_FOUND,
                        "CREDENTIAL_NOT_FOUND", "The credential does not exist for this partner."));
        if (credential.getStatus() != CredentialStatus.REVOKED) credential.revoke();
    }

    private void validateEd25519PublicKey(String pem) {
        try {
            if (pem == null || !pem.contains("-----BEGIN PUBLIC KEY-----") ||
                    !pem.contains("-----END PUBLIC KEY-----")) throw new IllegalArgumentException();
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(base64);
            KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (Exception ex) {
            throw new PartnerAuthenticationException(HttpStatus.BAD_REQUEST, "INVALID_PUBLIC_KEY",
                    "The supplied public key is not a valid Ed25519 SubjectPublicKeyInfo PEM key.");
        }
    }
}

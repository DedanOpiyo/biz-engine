package com.bizengine.businessuser.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.issuer}")
    private String issuer;

    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() throws Exception {
        // Reads private.pem file straight from your project root directory path
        String pemContent = new String(Files.readAllBytes(Paths.get("private.pem")))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] derBytes = Base64.getDecoder().decode(pemContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(derBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = (RSAPrivateKey) kf.generatePrivate(spec);
    }

    public String generateToken(String email, String externalUserId) {
        try {
            RSASSASigner signer = new RSASSASigner(privateKey);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(externalUserId)
                    .issuer(issuer)
                    .claim("email", email)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + 3600000)) // Expires in 1 hour
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claimsSet);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error signing JWT token asset parameters", e);
        }
    }
}

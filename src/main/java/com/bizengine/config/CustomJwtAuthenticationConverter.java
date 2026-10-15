package com.bizengine.config;

import com.bizengine.businessuser.model.BusinessUser;
import com.bizengine.businessuser.repository.BusinessUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Inject whichever repository/service manages your active users or merchants
// For this blueprint, we assume a database access point exists for your User profiles.
@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final BusinessUserRepository businessUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String localIssuer;

    public CustomJwtAuthenticationConverter(
            BusinessUserRepository businessUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.jwt.issuer}") String localIssuer) {
        this.businessUserRepository = businessUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.localIssuer = localIssuer;
    }

    @Override
    @Transactional // Ensures provisioning happens reliably within a transaction boundary
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String externalUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String issuer = jwt.getIssuer().toString();

        // 🟢 NODE.JS MIDDLEWARE EQUIVALENT:
        // 🟢 Check if user exists. If not, instantly provision a minimal profile (insert them into the DB instantly).
        // 1. Resolve or provision the Business User record
        BusinessUser businessUser = businessUserRepository.findByExternalUserId(externalUserId)
            .orElseGet(() -> {
                BusinessUser newUser = new BusinessUser();
                newUser.setExternalUserId(externalUserId);
                newUser.setEmail(email != null ? email : "provisioned-" + externalUserId + "@external.com");

                // Satisfy non-null password constraint for external provider profiles
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                // Fallback company name to issuer context if not provided
                newUser.setCompanyName(issuer);

                return businessUserRepository.save(newUser);
            });

        // Placeholder wrapper object holding user info for the controllers
        Object identityPrincipal = email;

        // 2. Establish security context authorization rules
        List<SimpleGrantedAuthority> authorities = Collections.emptyList();

        // 🔒 Only grant administrative power if issued internally by your platform
        if (localIssuer.equals(issuer)) {
            // Option A: Hardcode specific platform management email profiles
            if ("superadmin@superapp.com".equals(email)) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            // Option B: Or inspect a trusted custom claims block inside your local platform token:
            // List<String> roles = jwt.getClaimAsStringList("roles");
        }

        // Pack the managed DB entity instance securely into the security context token
        return new CustomUserAuthentication(
                businessUser,
                jwt.getTokenValue(),
                authorities // <-- Pass granted authorities instead. Collections.emptyList() // <-- This strips all roles, rendering you an anonymous user
        );
    }
}

package com.bizengine.config;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import com.bizengine.businessuser.model.ExternalPartner;
import com.bizengine.businessuser.repository.ExternalPartnerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.jwt.issuer}")
    private String localIssuer;

//    @Value("${app.jwt.secret}") // we now use public key
//    private String jwtSecret;

    @Value("${app.jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${external.provider.issuer}")
    private String externalIssuer;

    @Value("${external.provider.jwk-set-uri}")
    private String externalJwkSetUri;

    private final ResourceLoader resourceLoader;
    private final ExternalPartnerRepository partnerRepository;

    // In-memory cache to keep track of generated decoders so we don't recreate them every request
    private final ConcurrentHashMap<String, AuthenticationManager> managerCache = new ConcurrentHashMap<>();
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter; // <-- ADDED

    public SecurityConfig(ResourceLoader resourceLoader,
                          ExternalPartnerRepository partnerRepository,
                          CustomJwtAuthenticationConverter customJwtAuthenticationConverter) { // <-- INJECTED
        this.resourceLoader = resourceLoader;
        this.partnerRepository = partnerRepository;
        this.customJwtAuthenticationConverter = customJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF since APIs are stateless and use JWT tokens
                .csrf(csrf -> csrf.disable())

                // 2. Protect all business logic endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/merchants/**").permitAll() // .authenticated() (Public merchant endpoints for now)
                        .requestMatchers("/api/v1/business-users/login").permitAll() // Allow public login access
                        .requestMatchers("/api/v1/business-users/provision").permitAll()
                        .requestMatchers("/api/v1/system/**").permitAll()
                        .requestMatchers("/api/v1/partners", "/api/v1/partners/**").permitAll()

                        // ⚠️ CRITICAL: Lock down your administrative partner enlisting endpoints.
                        // In production, require a specific role like .hasRole("ADMIN") or restrict it via an internal IP network.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .requestMatchers("/api/v1/business-users/**").authenticated() // Protecting your new controller
                        .requestMatchers("/api/v1/coupons/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 3. Make the session entirely stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 🟢 ADD THIS BLOCK: Bridges Spring Security exceptions to standard JSON outputs
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Authentication signature check failed or token is expired.\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Access denied. Requires elevated platform role structures.\"}");
                        })
                )

                // 4. Configure this service as an OAuth2 Resource Server that validates JWTs
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(customAuthenticationManagerResolver())
                );

        return http.build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> customAuthenticationManagerResolver() {
//        Map<String, AuthenticationManager> managers = new HashMap<>();
//
//        // 1. Build manager for local/internal tokens using your public key
//        JwtDecoder localDecoder = NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
//        JwtAuthenticationProvider localProvider = new JwtAuthenticationProvider(localDecoder);
//        managers.put(localIssuer, new ProviderManager(localProvider));
//
//        // 2. Build manager for external partner tokens using their remote JWK Set Uri
//        JwtDecoder externalDecoder = NimbusJwtDecoder.withJwkSetUri(externalJwkSetUri).build();
//        JwtAuthenticationProvider externalProvider = new JwtAuthenticationProvider(externalDecoder);
//        managers.put(externalIssuer, new ProviderManager(externalProvider));
//
//        // Resolver reads the token unverified first to look up who issued it (the "iss" claim)
//        return request -> {
//            String token = resolveToken(request);
//            if (token != null) {
//                try {
//                    String issuer = com.nimbusds.jwt.JWTParser.parse(token).getJWTClaimsSet().getIssuer();
//                    if (managers.containsKey(issuer)) {
//                        return managers.get(issuer);
//                    }
//                } catch (Exception e) {
//                    throw new IllegalArgumentException("Invalid token format or unknown token issuer.");
//                }
//            }
//            // Fallback default manager if issuer is completely missing/unrecognized
//            return managers.get(localIssuer);
//        };

        return request -> {
            String token = resolveToken(request);
            if (token == null) {
                return getLocalManager();
            }

            try {
                String issuer = com.nimbusds.jwt.JWTParser.parse(token).getJWTClaimsSet().getIssuer();

                // 1. If it's our own local platform issuing it, run local public key verification
                if (localIssuer.equals(issuer)) {
                    return getLocalManager();
                }

                // 2. Dynamic DB verification check for dynamic whitelisting
                return managerCache.computeIfAbsent(issuer, iss -> {
                    ExternalPartner partner = partnerRepository.findByIssuerAndActiveTrue(iss)
                            .orElseThrow(() -> new IllegalArgumentException("Unauthorized token issuer system. Rejecting connection."));

                    JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(partner.getJwkSetUri()).build();
                    return createAuthenticationManager(decoder);
                });

            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid token format or unlisted API partner signature context.", e);
            }
        };
    }

    private AuthenticationManager getLocalManager() {
        return managerCache.computeIfAbsent(localIssuer, iss -> {
            JwtDecoder decoder = NimbusJwtDecoder.withPublicKey(loadPublicKey()).build();
            return createAuthenticationManager(decoder);
        });
    }

    private AuthenticationManager createAuthenticationManager(JwtDecoder decoder) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(customJwtAuthenticationConverter);
        return new ProviderManager(provider);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Helper utility method to read a local public key from your resources classpath folder
    private RSAPublicKey loadPublicKey() {
        try (InputStream is = resourceLoader.getResource(publicKeyPath).getInputStream()) {
            byte[] keyBytes = is.readAllBytes();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize local RSA public key for signature validation", e);
        }
    }

//    @Bean
//    public JwtDecoder jwtDecoder() {
//        // Build a decoder using the HMAC-SHA256 algorithm and the Node.js secret key
//        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
//        return NimbusJwtDecoder.withSecretKey(secretKey).build();
//    }

//    @Bean
//    public AuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService());
//import org.springframework.security.crypto.password.PasswordEncoder; // Now in class
//        authProvider.setPasswordEncoder(passwordEncoder());
//        return authProvider;
//    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        // Return a mock user details service or integrate with business user entities if they login locally
//        return username -> { throw new UsernameNotFoundException("Local login not supported. Use token validation."); };
//    }

//    @Bean
//    public PasswordEncoder passwordEncoder() { // Remove the old passwordEncoder()
//        return new BCryptPasswordEncoder();
//    }

//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
//        return config.getAuthenticationManager();
//    }
}

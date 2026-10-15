package com.bizengine.auth.web;

import com.bizengine.auth.service.PartnerAuthenticationException;
import com.bizengine.auth.service.PartnerRequestAuthenticator;
import com.bizengine.shared.error.ApiErrorResponse;
import com.bizengine.shared.web.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "biz.security.partner-authentication.enabled", havingValue = "true", matchIfMissing = true)
public class PartnerAuthenticationFilter extends OncePerRequestFilter {
    public static final String AUTHENTICATED_PARTNER_ID_ATTRIBUTE = "bizEngineAuthenticatedPartnerId";

    private final PartnerRequestAuthenticator authenticator;
    private final ObjectMapper objectMapper;
    private final int maxBodyBytes;

    public PartnerAuthenticationFilter(PartnerRequestAuthenticator authenticator, ObjectMapper objectMapper,
                                       @Value("${biz.security.partner-authentication.max-body-bytes:1048576}") int maxBodyBytes) {
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
        this.maxBodyBytes = maxBodyBytes;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/api/v1/partners") || path.startsWith("/api/v1/partners/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final CachedBodyHttpServletRequest wrapped;
        final UUID authenticatedPartnerId;
        try {
            wrapped = new CachedBodyHttpServletRequest(request, maxBodyBytes);
            UUID partnerId = parsePartnerId(request.getHeader(PartnerRequestAuthenticator.PARTNER_ID_HEADER));
            String keyId = request.getHeader(PartnerRequestAuthenticator.KEY_ID_HEADER);
            String timestamp = request.getHeader(PartnerRequestAuthenticator.TIMESTAMP_HEADER);
            String nonce = request.getHeader(PartnerRequestAuthenticator.NONCE_HEADER);
            String signature = request.getHeader(PartnerRequestAuthenticator.SIGNATURE_HEADER);
            String pathAndQuery = request.getRequestURI() +
                    (request.getQueryString() == null ? "" : "?" + request.getQueryString());
            authenticatedPartnerId = authenticator.authenticate(partnerId, keyId, timestamp, nonce,
                    signature, request.getMethod(), pathAndQuery, wrapped.getCachedBody());
        } catch (CachedBodyHttpServletRequest.PayloadTooLargeException ex) {
            writeError(response, request, HttpStatus.PAYLOAD_TOO_LARGE, "REQUEST_BODY_TOO_LARGE",
                    "The request body exceeds the configured size limit.");
            return;
        } catch (PartnerAuthenticationException ex) {
            writeError(response, request, ex.getStatus(), ex.getCode(), ex.getMessage());
            return;
        } catch (IllegalArgumentException ex) {
            writeError(response, request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                    "Partner authentication headers are invalid.");
            return;
        }

        wrapped.setAttribute(AUTHENTICATED_PARTNER_ID_ATTRIBUTE, authenticatedPartnerId);
        if (!isAllowedPartnerOperation(wrapped, authenticatedPartnerId)) {
            writeError(response, wrapped, HttpStatus.FORBIDDEN, "OPERATOR_ACCESS_REQUIRED",
                    "This partner-management operation requires an authorized Biz-Engine operator.");
            return;
        }
        filterChain.doFilter(wrapped, response);
    }

    private UUID parsePartnerId(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid partner id."); }
    }

    private boolean isAllowedPartnerOperation(HttpServletRequest request, UUID partnerId) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        String prefix = "/api/v1/partners/";
        String path = request.getRequestURI();
        if (!path.startsWith(prefix)) return false;
        String suffix = path.substring(prefix.length());
        if (suffix.contains("/")) return false;
        try { return partnerId.equals(UUID.fromString(suffix)); }
        catch (IllegalArgumentException ex) { return false; }
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                            String code, String message) throws IOException {
        if (response.isCommitted()) return;
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), code, message,
                request.getRequestURI(), requestId == null ? null : requestId.toString(), List.of());
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

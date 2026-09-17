package com.huicang.wise.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Value("${security.headers.x-frame-options:DENY}")
    private String xFrameOptions;

    @Value("${security.headers.x-content-type-options:nosniff}")
    private String xContentTypeOptions;

    @Value("${security.headers.x-xss-protection:1; mode=block}")
    private String xXssProtection;

    @Value("${security.headers.content-security-policy:default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; form-action 'self';}")
    private String contentSecurityPolicy;

    @Value("${security.headers.strict-transport-security:max-age=31536000; includeSubDomains; preload}")
    private String strictTransportSecurity;

    @Value("${security.headers.referrer-policy:no-referrer-when-downgrade}")
    private String referrerPolicy;

    @Value("${security.headers.permissions-policy:geolocation=(), microphone=(), camera=()}")
    private String permissionsPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        response.setHeader("X-Frame-Options", xFrameOptions);
        response.setHeader("X-Content-Type-Options", xContentTypeOptions);
        response.setHeader("X-XSS-Protection", xXssProtection);
        response.setHeader("Content-Security-Policy", contentSecurityPolicy);
        response.setHeader("Strict-Transport-Security", strictTransportSecurity);
        response.setHeader("Referrer-Policy", referrerPolicy);
        response.setHeader("Permissions-Policy", permissionsPolicy);
        
        filterChain.doFilter(request, response);
    }
}

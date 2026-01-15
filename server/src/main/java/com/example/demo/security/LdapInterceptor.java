package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LdapInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LdapInterceptor.class);
    private static final String LDAP_HEADER = "X-LDAP";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS preflight 요청(OPTIONS)은 인증 없이 통과
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            logger.debug("OPTIONS request - skipping LDAP check");
            return true;
        }

        String ldap = request.getHeader(LDAP_HEADER);

        if (ldap == null || ldap.trim().isEmpty()) {
            logger.warn("Missing X-LDAP header for request: {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"X-LDAP header is required\"}}");
            return false;
        }

        // Normalize LDAP to lowercase for case-insensitive comparison
        String normalizedLdap = ldap.toLowerCase().trim();
        logger.debug("X-LDAP header found: {} (normalized to: {})", ldap, normalizedLdap);
        LdapContext.setLdap(normalizedLdap);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LdapContext.clear();
    }
}

package com.graduation.schedulingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.security.trusted-services.user-service}")
    private String userServiceApiKey;

    private static final String API_KEY_HEADER = "X-Internal-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Only check internal endpoints
        if (requestPath.startsWith("/api/internal/")) {
            String apiKey = request.getHeader(API_KEY_HEADER);

            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("Missing API key for internal endpoint: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Missing internal API key\"}");
                return;
            }

            if (!apiKey.equals(userServiceApiKey)) {
                log.warn("Invalid API key for internal endpoint: {}", requestPath);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Invalid internal API key\"}");
                return;
            }

            log.debug("Valid API key for internal endpoint: {}", requestPath);
        }

        filterChain.doFilter(request, response);
    }
}
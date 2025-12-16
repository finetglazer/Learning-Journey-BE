package com.graduation.forumservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String AUTHENTICATED_HEADER = "X-Authenticated";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip authentication for internal endpoints (already protected by
        // InternalApiKeyFilter)
        if (requestPath.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Read user context from headers (set by Gateway)
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String authenticated = request.getHeader(AUTHENTICATED_HEADER);

        if (userIdHeader != null && userEmail != null && "true".equals(authenticated)) {
            try {
                Long userId = Long.parseLong(userIdHeader);

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("User authenticated from headers: userId={}, email={}", userId, userEmail);

            } catch (NumberFormatException e) {
                log.warn("Invalid user ID format in header: {}", userIdHeader);
            }
        } else {
            log.debug("No authentication headers found for path: {}", requestPath);
        }

        filterChain.doFilter(request, response);
    }
}

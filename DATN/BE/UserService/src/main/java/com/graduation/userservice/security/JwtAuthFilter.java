package com.graduation.userservice.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService blacklistService;
//    private final UserDetailsService userDetailsService; // Optional: if you have UserDetailsService

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                // 1) Check if token is blacklisted
                if (blacklistService.isTokenBlacklisted(jwt)) {
                    log.debug("Token is blacklisted, skipping authentication");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2) Validate JWT token
                if (jwtProvider.validateToken(jwt)) {
                    String userEmail = jwtProvider.getEmailFromToken(jwt);

                    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        // 3) Create authentication token
                        // Option 1: Simple authentication with email as principal
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userEmail,
                                        null,
                                        Collections.emptyList() // No roles for now, you can add them later
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        // Option 2: If you have UserDetailsService (uncomment below and comment above)
                        /*
                        try {
                            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                            UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        } catch (Exception e) {
                            log.error("Error loading user details for: {}", userEmail, e);
                        }
                        */

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Successfully authenticated user: {}", userEmail);
                    }
                } else {
                    log.debug("Invalid JWT token");
                }
            }
        } catch (JwtException ex) {
            log.error("JWT token validation failed: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error in JWT authentication filter: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from the Authorization header
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
package com.graduation.apigatewayservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProvider jwtProvider;
    private final TokenBlacklistService blacklistService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Value("${app.public-endpoints}")
    private String publicEndpointsConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token
        String token = extractToken(exchange.getRequest());

        if (token == null) {
            log.debug("No JWT token found for path: {}", path);
            return unauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
        }

        // Validate token asynchronously
        return blacklistService.isTokenBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.debug("Token is blacklisted");
                        return unauthorized(exchange.getResponse(), "Token has been revoked");
                    }

                    // Validate JWT
                    if (!jwtProvider.validateToken(token)) {
                        log.debug("Invalid JWT token");
                        return unauthorized(exchange.getResponse(), "Invalid or expired token");
                    }

                    // Extract user info
                    String userId = jwtProvider.getUserIdFromToken(token);
                    String email = jwtProvider.getEmailFromToken(token);

                    if (userId == null || email == null) {
                        log.error("Failed to extract user info from token");
                        return unauthorized(exchange.getResponse(), "Invalid token claims");
                    }

                    // Add user context to headers
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .header("X-Authenticated", "true")
                            .build();

                    log.debug("JWT validated for user: {} (ID: {})", email, userId);

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(error -> {
                    log.error("Error during JWT validation: {}", error.getMessage());
                    return unauthorized(exchange.getResponse(), "Authentication failed");
                });
    }

    /**
     * Check if path is public (no JWT required)
     */
    private boolean isPublicEndpoint(String path) {
        List<String> publicEndpoints = Arrays.asList(publicEndpointsConfig.split(","));

        return publicEndpoints.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    /**
     * Extract JWT from Authorization header
     */
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Return 401 Unauthorized response
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
            {
              "error": "Unauthorized",
              "message": "%s",
              "status": 401
            }
            """, message);

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return 2; // After rate limiting (1), before routing
    }
}
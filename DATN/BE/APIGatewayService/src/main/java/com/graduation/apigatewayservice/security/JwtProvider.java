package com.graduation.apigatewayservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Validate JWT token signature and expiration
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.debug("JWT token expired: {}", ex.getMessage());
            return false;
        } catch (UnsupportedJwtException ex) {
            log.error("JWT token is unsupported: {}", ex.getMessage());
            return false;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            return false;
        } catch (SecurityException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
            return false;
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
            return false;
        } catch (JwtException ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject();
        } catch (JwtException ex) {
            log.error("Failed to extract email from token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extract userId from token
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parseClaims(token);
            Object userIdObj = claims.get("userId");
            return userIdObj != null ? userIdObj.toString() : null;
        } catch (JwtException ex) {
            log.error("Failed to extract userId from token: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("type", String.class);
        } catch (JwtException ex) {
            log.error("Failed to extract token type: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Get remaining expiry time in seconds
     */
    public long getRemainingExpiry(String token) {
        try {
            Claims claims = parseClaims(token);
            long now = System.currentTimeMillis();
            long expiryTime = claims.getExpiration().getTime();
            long diffMillis = expiryTime - now;
            return diffMillis > 0 ? diffMillis / 1000 : 0;
        } catch (JwtException ex) {
            log.error("Failed to get remaining expiry: {}", ex.getMessage());
            return 0;
        }
    }

    /**
     * Parse JWT claims
     */
    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
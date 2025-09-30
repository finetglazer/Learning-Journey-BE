package com.graduation.userservice.security;

import com.graduation.userservice.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMillis:86400000}") // default 24 hours
    private long jwtExpirationMillis;

    // Change the generateToken to accept User object and store email
    public String generateToken(User user) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiryDate = new Date(now + jwtExpirationMillis);

        return Jwts.builder()
                .setSubject(user.getEmail())     // Store EMAIL as subject
                .claim("userId", user.getId())   // Store USER ID as claim
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate the token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // If parsing succeeds, token is valid
            return true;
        } catch (JwtException ex) {
            // Token is invalid or expired
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject(); // Returns email
    }

    public String getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", String.class); // Returns userId from claim
    }

    /**
     * Return how many seconds remain until token expiration.
     */
    // Get how many seconds remain until expiry
    public long getRemainingExpiry(String token) {
        Claims claims = parseClaims(token);
        long now = System.currentTimeMillis();
        long expiryTime = claims.getExpiration().getTime();
        long diffMillis = expiryTime - now;
        if (diffMillis < 0) return 0;
        return diffMillis / 1000;
    }

    /**
     * Parse and return the Claims.
     * NOTE: We use the same secret key for validation that was used for signing.
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

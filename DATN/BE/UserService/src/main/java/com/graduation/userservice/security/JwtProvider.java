package com.graduation.userservice.security;

import com.graduation.userservice.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:900000}") // 15 minutes default
    private long accessTokenExpirationMillis;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7 days default
    private long refreshTokenExpirationMillis;

    // Generate short-lived access token (15 min)
    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpirationMillis, "access");
    }

    // Generate long-lived refresh token (7 days)
    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpirationMillis, "refresh");
    }

    // Keep old method for backward compatibility (uses access token settings)
    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    private String generateToken(User user, long expirationMillis, String tokenType) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiryDate = new Date(now + expirationMillis);

        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("userId", user.getId())
                .claim("type", tokenType) // "access" or "refresh"
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", String.class);
    }

    public String getTokenType(String token) {
        Claims claims = parseClaims(token);
        return claims.get("type", String.class);
    }

    public long getRemainingExpiry(String token) {
        Claims claims = parseClaims(token);
        long now = System.currentTimeMillis();
        long expiryTime = claims.getExpiration().getTime();
        long diffMillis = expiryTime - now;
        if (diffMillis < 0) return 0;
        return diffMillis / 1000;
    }

    private Claims parseClaims(String token) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
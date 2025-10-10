package com.graduation.apigatewayservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Add token to blacklist with expiry
     */
    public Mono<Void> blacklistToken(String token, long expirySeconds) {
        String key = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.opsForValue()
                .set(key, "1", Duration.ofSeconds(expirySeconds))
                .doOnSuccess(success -> log.debug("Token blacklisted: {}", key))
                .doOnError(error -> log.error("Failed to blacklist token: {}", error.getMessage()))
                .then();
    }

    /**
     * Check if token is blacklisted
     */
    public Mono<Boolean> isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return reactiveRedisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnNext(exists -> {
                    if (exists) {
                        log.debug("Token is blacklisted: {}", key);
                    }
                })
                .doOnError(error -> log.error("Failed to check blacklist: {}", error.getMessage()));
    }
}
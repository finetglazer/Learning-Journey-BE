package com.graduation.apigatewayservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConnectionWarmup implements CommandLineRunner {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Override
    public void run(String... args) {
        log.info("Warming up Redis connection...");
        reactiveRedisTemplate.opsForValue().get("warmup")
                .timeout(Duration.ofSeconds(5))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2)))
                .doOnSuccess(val -> log.info("Redis connection warmed up successfully"))
                .doOnError(err -> log.error("Failed to warm up Redis connection after retries: {}", err.getMessage()))
                .subscribe(
                        success -> {
                        },
                        error -> log.error("Error during Redis warmup subscription: {}", error.getMessage()));
    }
}

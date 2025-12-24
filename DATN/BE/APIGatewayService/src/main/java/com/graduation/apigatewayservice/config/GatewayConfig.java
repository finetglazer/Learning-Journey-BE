package com.graduation.apigatewayservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class GatewayConfig {

    @Component
    public static class RateLimitGatewayFilter extends AbstractGatewayFilterFactory<RateLimitGatewayFilter.Config> {
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public RateLimitGatewayFilter() {
            super(Config.class);
        }

        @Override
        public GatewayFilter apply(Config config) {
            return (exchange, chain) -> {
                String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                if (userId == null) userId = "anonymous";

                Bucket bucket = buckets.computeIfAbsent(userId, key -> createNewBucket());
                if (bucket.tryConsume(1)) {
                    return chain.filter(exchange);
                } else {
                    // Return 429 if the bucket is empty
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    return exchange.getResponse().setComplete();
                }
            };
        }

        private Bucket createNewBucket() {
            // PROOF OF STRATEGY:
            // Refill: 10 tokens per second (sustained rate)
            // Capacity: 100 tokens (allows a "BURST" of 100 requests at once)
            Refill refill = Refill.intervally(10, Duration.ofSeconds(1));
            Bandwidth limit = Bandwidth.classic(100, refill);
            return Bucket.builder().addLimit(limit).build();
        }

        public static class Config {
            // Configuration properties if needed
        }
    }
}
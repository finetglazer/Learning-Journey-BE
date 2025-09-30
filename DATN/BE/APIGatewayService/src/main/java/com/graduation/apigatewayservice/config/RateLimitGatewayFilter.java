package com.graduation.apigatewayservice.config;

import com.graduation.apigatewayservice.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitGatewayFilter extends AbstractGatewayFilterFactory<RateLimitGatewayFilter.Config> {

    private final Map<String, ClientRateLimit> clientLimits = new ConcurrentHashMap<>();

    public RateLimitGatewayFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientId = getClientId(exchange);

            if (isRateLimited(clientId, config)) {
                log.warn("Rate limit exceeded for client: {} | Config hashcode: {}",
                        clientId, config.getSafeHashCode());

                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(config.getMaxRequests()));
                exchange.getResponse().getHeaders().add("X-RateLimit-Window", String.valueOf(config.getWindowSizeInSeconds()));

                String body = String.format("""
                    {
                      "error": "Rate limit exceeded",
                      "message": "Too many requests. Please try again later.",
                      "limit": %d,
                      "window": "%d seconds"
                    }
                    """, config.getMaxRequests(), config.getWindowSizeInSeconds());

                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return exchange.getResponse().writeWith(Mono.just(buffer));
            }

            return chain.filter(exchange);
        };
    }

    private String getClientId(org.springframework.web.server.ServerWebExchange exchange) {
        // Use IP address as client identifier
        String remoteAddress = exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                "unknown";

        // Log client identification but exclude sensitive data from hashcode
        log.debug("Identifying client for rate limiting: {}",
                Constant.shouldExcludeFromLogging("remoteAddress") ? "[FILTERED]" : remoteAddress);

        return remoteAddress;
    }

    private boolean isRateLimited(String clientId, Config config) {
        ClientRateLimit limit = clientLimits.computeIfAbsent(clientId, k -> new ClientRateLimit());

        long now = Instant.now().getEpochSecond();
        long windowStart = now - config.getWindowSizeInSeconds();

        // Clean old entries
        limit.getRequestTimes().entrySet().removeIf(entry -> entry.getKey() < windowStart);

        // Count current requests in window
        int currentRequests = limit.getRequestTimes().values().stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        if (currentRequests >= config.getMaxRequests()) {
            log.debug("Rate limit check failed for client: {} | Current requests: {} | Limit: {} | ClientLimit hashcode: {}",
                    clientId, currentRequests, config.getMaxRequests(), limit.getSafeHashCode());
            return true;
        }

        // Add current request
        limit.getRequestTimes().computeIfAbsent(now, k -> new AtomicInteger(0)).incrementAndGet();

        log.debug("Rate limit check passed for client: {} | Current requests: {} | Limit: {}",
                clientId, currentRequests + 1, config.getMaxRequests());

        return false;
    }

    public static class Config {
        private int maxRequests = 100; // requests per window
        private int windowSizeInSeconds = 60; // 1 minute window

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public int getWindowSizeInSeconds() {
            return windowSizeInSeconds;
        }

        public void setWindowSizeInSeconds(int windowSizeInSeconds) {
            this.windowSizeInSeconds = windowSizeInSeconds;
        }

        @Override
        public int hashCode() {
            // Include both configuration values in hashcode as they define the rate limit behavior
            return Objects.hash(maxRequests, windowSizeInSeconds);
        }

        public int getSafeHashCode() {
            // For logging, include both values as they are configuration settings
            return Objects.hash(maxRequests, windowSizeInSeconds);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Config config = (Config) obj;
            return maxRequests == config.maxRequests &&
                    windowSizeInSeconds == config.windowSizeInSeconds;
        }

        @Override
        public String toString() {
            return String.format("Config{maxRequests=%d, windowSizeInSeconds=%d}",
                    maxRequests, windowSizeInSeconds);
        }
    }

    private static class ClientRateLimit {
        private final Map<Long, AtomicInteger> requestTimes = new ConcurrentHashMap<>();

        public Map<Long, AtomicInteger> getRequestTimes() {
            return requestTimes;
        }

        @Override
        public int hashCode() {
            // Exclude requestTimes from hashcode as it changes frequently
            // Use a constant for consistent hashcode for same client
            return Objects.hash("ClientRateLimit");
        }

        public int getSafeHashCode() {
            // For logging, use size of request times map instead of actual timestamps
            return Objects.hash("ClientRateLimit", requestTimes.size());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            // Two ClientRateLimit objects are equal if they are the same instance
            return false;
        }

        @Override
        public String toString() {
            return String.format("ClientRateLimit{activeTimeSlots=%d}", requestTimes.size());
        }
    }
}
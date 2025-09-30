package com.graduation.apigatewayservice.config;

import com.graduation.apigatewayservice.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {

        // Pre-filter: Log incoming request
        RequestLogInfo requestInfo = logRequest(exchange);

        // Record start time
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    // Post-filter: Log response
                    long duration = System.currentTimeMillis() - startTime;
                    logResponse(exchange, duration, requestInfo);
                });
    }

    private RequestLogInfo logRequest(ServerWebExchange exchange) {
        var request = exchange.getRequest();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        RequestLogInfo requestInfo = new RequestLogInfo(
                request.getMethod().toString(),
                request.getPath().value(),
                request.getRemoteAddress() != null ?
                        request.getRemoteAddress().getAddress().getHostAddress() : "unknown",
                timestamp
        );

        log.info("=== INCOMING REQUEST === [{}] | Request hashcode: {}",
                timestamp, requestInfo.getSafeHashCode());
        log.info("Method: {} | Path: {} | Remote Address: {}",
                requestInfo.getMethod(),
                requestInfo.getPath(),
                requestInfo.getRemoteAddress()
        );

        // Log headers (excluding sensitive ones)
        Map<String, Object> safeHeaders = request.getHeaders().entrySet().stream()
                .filter(entry -> !isSensitiveHeader(entry.getKey()))
                .filter(entry -> !Constant.shouldExcludeFromLogging(entry.getKey().toLowerCase()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        log.info("Headers: {}", safeHeaders);

        // Log query parameters (excluding sensitive ones)
        if (!request.getQueryParams().isEmpty()) {
            Map<String, Object> safeParams = request.getQueryParams().entrySet().stream()
                    .filter(entry -> !Constant.shouldExcludeFromLogging(entry.getKey().toLowerCase()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
            log.info("Query Params: {}", safeParams);
        }

        return requestInfo;
    }

    private void logResponse(ServerWebExchange exchange, long duration, RequestLogInfo requestInfo) {
        var response = exchange.getResponse();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        ResponseLogInfo responseInfo = new ResponseLogInfo(
                response.getStatusCode() != null ? response.getStatusCode().value() : 0,
                duration,
                requestInfo.getPath(),
                timestamp
        );

        log.info("=== OUTGOING RESPONSE === [{}] | Response hashcode: {}",
                timestamp, responseInfo.getSafeHashCode());
        log.info("Status: {} | Duration: {}ms | Path: {}",
                responseInfo.getStatus(),
                responseInfo.getDuration(),
                responseInfo.getPath()
        );

        // Log response headers (excluding sensitive ones)
        Map<String, Object> safeResponseHeaders = response.getHeaders().entrySet().stream()
                .filter(entry -> !isSensitiveHeader(entry.getKey()))
                .filter(entry -> !Constant.shouldExcludeFromLogging(entry.getKey().toLowerCase()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        log.info("Response Headers: {}", safeResponseHeaders);
        log.info("========================");
    }

    private boolean isSensitiveHeader(String headerName) {
        String lowerCaseName = headerName.toLowerCase();

        // Use constants for consistent checking
        if (Constant.shouldExcludeFromLogging(lowerCaseName)) {
            return true;
        }

        // Additional gateway-specific sensitive headers
        return lowerCaseName.contains("authorization") ||
                lowerCaseName.contains("cookie") ||
                lowerCaseName.contains("token") ||
                lowerCaseName.contains("password") ||
                lowerCaseName.contains("key") ||
                lowerCaseName.contains("secret");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * Request logging information with hashcode exclusion
     */
    private static class RequestLogInfo {
        private final String method;
        private final String path;
        private final String remoteAddress;
        private final String timestamp;

        public RequestLogInfo(String method, String path, String remoteAddress, String timestamp) {
            this.method = method;
            this.path = path;
            this.remoteAddress = remoteAddress;
            this.timestamp = timestamp;
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getRemoteAddress() { return remoteAddress; }
        public String getTimestamp() { return timestamp; }

        @Override
        public int hashCode() {
            // Exclude timestamp and remoteAddress from hashcode as they change frequently
            return Objects.hash(method, path);
        }

        public int getSafeHashCode() {
            // For logging, only include method and path
            return Objects.hash(method, path);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RequestLogInfo that = (RequestLogInfo) obj;
            return Objects.equals(method, that.method) &&
                    Objects.equals(path, that.path);
        }
    }

    /**
     * Response logging information with hashcode exclusion
     */
    private static class ResponseLogInfo {
        private final int status;
        private final long duration;
        private final String path;
        private final String timestamp;

        public ResponseLogInfo(int status, long duration, String path, String timestamp) {
            this.status = status;
            this.duration = duration;
            this.path = path;
            this.timestamp = timestamp;
        }

        public int getStatus() { return status; }
        public long getDuration() { return duration; }
        public String getPath() { return path; }
        public String getTimestamp() { return timestamp; }

        @Override
        public int hashCode() {
            // Exclude duration and timestamp from hashcode as they change frequently
            return Objects.hash(status, path);
        }

        public int getSafeHashCode() {
            // For logging, only include status and path
            return Objects.hash(status, path);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ResponseLogInfo that = (ResponseLogInfo) obj;
            return status == that.status &&
                    Objects.equals(path, that.path);
        }
    }
}
package com.graduation.apigatewayservice.config;

import com.graduation.apigatewayservice.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-1) // Higher priority than default error handler
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {

        ErrorResponse errorResponse = createErrorResponse(ex, exchange);

        // Log error but exclude sensitive information from hashcode
        log.error("Gateway error occurred: {} for path: {} | ErrorResponse hashcode: {}",
                ex.getMessage(),
                exchange.getRequest().getPath().value(),
                errorResponse.getSafeHashCode(),
                ex);

        exchange.getResponse().setStatusCode(errorResponse.getStatus());
        exchange.getResponse().getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        String body = buildErrorResponseBody(errorResponse, exchange);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private ErrorResponse createErrorResponse(Throwable ex, ServerWebExchange exchange) {
        if (ex instanceof TimeoutException) {
            return new ErrorResponse(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Gateway Timeout",
                    Constant.msg.get("TIMEOUT_ERROR"));
        }

        if (ex instanceof ResponseStatusException rse) {
            return new ErrorResponse(
                    (HttpStatus) rse.getStatusCode(),
                    "Service Error",
                    rse.getReason() != null ? rse.getReason() : Constant.msg.get("GENERIC_ERROR"));
        }

        if (ex instanceof org.springframework.web.reactive.function.client.WebClientException ||
                ex.getCause() instanceof java.net.ConnectException) {
            return new ErrorResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Service Unavailable",
                    "The requested service is currently unavailable. Please try again later.");
        }

        // Default case
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                Constant.msg.get("GENERIC_ERROR"));
    }

    private String buildErrorResponseBody(ErrorResponse errorResponse, ServerWebExchange exchange) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String path = exchange.getRequest().getPath().value();

        return String.format("""
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s",
                  "path": "%s"
                }
                """,
                timestamp,
                errorResponse.getStatus().value(),
                errorResponse.getError(),
                errorResponse.getMessage(),
                path);
    }

    private static class ErrorResponse {
        private final HttpStatus status;
        private final String error;
        private final String message;

        public ErrorResponse(HttpStatus status, String error, String message) {
            this.status = status;
            this.error = error;
            this.message = message;
        }

        public HttpStatus getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        /**
         * Generate hashcode excluding sensitive fields
         */
        @Override
        public int hashCode() {
            // Only include status and error in hashcode, exclude message which might
            // contain sensitive data
            return Objects.hash(status, error);
        }

        /**
         * Safe hashcode for logging that excludes sensitive information
         */
        public int getSafeHashCode() {
            // For logging purposes, use only non-sensitive fields
            return Objects.hash(status != null ? status.value() : null, error);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            ErrorResponse that = (ErrorResponse) obj;
            return Objects.equals(status, that.status) &&
                    Objects.equals(error, that.error);
            // Intentionally exclude message from equals as it might contain sensitive data
        }

        @Override
        public String toString() {
            // Safe toString that doesn't expose potentially sensitive message content
            return String.format("ErrorResponse{status=%s, error='%s'}",
                    status, error);
        }
    }
}
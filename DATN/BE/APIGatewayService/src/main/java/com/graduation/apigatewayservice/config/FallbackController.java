package com.graduation.apigatewayservice.config;

import com.graduation.apigatewayservice.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping(value = "/order-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> orderServiceFallback() {
        log.error("Order service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "Order Service",
                        Constant.msg.get("ORDER_SERVICE_FALLBACK")
                ));
    }

    @RequestMapping(value = "/payment-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> paymentServiceFallback() {
        log.error("Payment service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "Payment Service",
                        Constant.msg.get("PAYMENT_SERVICE_FALLBACK")
                ));
    }

    @RequestMapping(value = "/product-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> productServiceFallback() {
        log.error("Product service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "Product Service",
                        Constant.msg.get("PRODUCT_SERVICE_FALLBACK")
                ));
    }

    @RequestMapping(value = "/user-service", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        log.error("User service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createFallbackResponse(
                        "User Service",
                        Constant.msg.get("USER_SERVICE_FALLBACK")
                ));
    }

    private Map<String, Object> createFallbackResponse(String serviceName, String message) {
        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message,
                "service", serviceName,
                "path", "fallback"
        );

        // Log the response but exclude sensitive fields from hashcode calculation
        log.warn("Fallback response generated for {}: {}", serviceName,
                response.entrySet().stream()
                        .filter(entry -> !Constant.HASHCODE_EXCLUSION_FIELDS.contains(entry.getKey()))
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )));

        return response;
    }
}
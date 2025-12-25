package com.graduation.apigatewayservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ConfigurableApplicationContext;

import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class ApiGatewayServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ApiGatewayServiceApplication.class, args);

        // Log the routes on startup for debugging
        RouteLocator routeLocator = context.getBean(RouteLocator.class);
        routeLocator.getRoutes().subscribe(route -> log.info("Loaded route: {} -> {}", route.getId(), route.getUri()));

        log.info("API Gateway Service started successfully on port 8080");
        log.info("Available endpoints:");
        log.info("  - User Service: http://localhost:8080/api/users/**");
        log.info("  - Payment Service: http://localhost:8080/api/payments/**");
        log.info("  - Health Check: http://localhost:8080/health");
        log.info("  - Actuator: http://localhost:8080/actuator/health");
    }
}
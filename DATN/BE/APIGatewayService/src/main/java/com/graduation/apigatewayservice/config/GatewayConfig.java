package com.graduation.apigatewayservice.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    // Basic gateway configuration
    // Rate limiting is handled by RateLimitGatewayFilter (in-memory)
    // No Redis dependencies required

}
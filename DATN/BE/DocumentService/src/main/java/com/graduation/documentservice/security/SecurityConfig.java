package com.graduation.documentservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final InternalApiKeyFilter internalApiKeyFilter;
    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Allow Actuator (Health checks)
                        .requestMatchers("/actuator/**").permitAll()

                        // 2. Allow Internal APIs (InternalApiKeyFilter handles security)
                        .requestMatchers("/api/internal/**").permitAll()

                        // 3. Allow All Public APIs (HeaderAuthenticationFilter & Gateway handle security)
                        // This fixes the 403 Forbidden on /api/document/**
                        .anyRequest().permitAll()
                )
                // Filter Order matches ProjectService:
                // 1. Internal Key Filter (Priority 1)
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // 2. Header Auth Filter (Priority 2)
                .addFilterAfter(headerAuthenticationFilter, InternalApiKeyFilter.class);

        return http.build();
    }
}
package com.graduation.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    // No need to inject host and port here anymore.
    // Also, the redisConnectionFactory() bean is removed.
    // Spring Boot will create it automatically from your application.properties.

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        // This 'connectionFactory' is now the one Spring Boot auto-configured for you,
        // complete with SSL and password from your URL.
        template.setConnectionFactory(connectionFactory);

        // Your serializer configuration is good and can stay exactly as it is.
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
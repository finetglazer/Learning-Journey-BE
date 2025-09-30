package com.graduation.userservice.config; // Use your own base package

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisConnectionTest implements CommandLineRunner {

    private final RedisTemplate<String, String> redisTemplate;

    // Spring injects the RedisTemplate you configured in RedisConfig
    public RedisConnectionTest(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("--- Testing Redis Connection ---");
            // Set a test key-value pair
            redisTemplate.opsForValue().set("connection_test", "success");

            // Read the value back
            String value = redisTemplate.opsForValue().get("connection_test");

            if ("success".equals(value)) {
                System.out.println("✅ Successfully connected to Redis and performed a test operation.");
            } else {
                System.err.println("❌ Could not verify Redis connection. Value read was: " + value);
            }

            // Clean up the test key
            redisTemplate.delete("connection_test");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to Redis. Exception: " + e.getMessage());
            // This will print the full error if the connection fails
            e.printStackTrace();
        }
        System.out.println("--------------------------------");
    }
}
package com.graduation.schedulingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based distributed lock service for saga coordination
 * Enhanced with atomic lock operations and saga-specific locking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final RedisTemplate<String, String> redisTemplate;
    private final String serviceInstanceId = UUID.randomUUID().toString().substring(0, 8);

    // Lua script for atomic lock release
    private static final String RELEASE_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('DEL', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    // Lua script for atomic lock extension
    private static final String EXTEND_LOCK_SCRIPT =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                    "else " +
                    "    return 0 " +
                    "end";

    /**
     * Acquire a distributed lock
     * @param lockKey The key to lock
     * @param ttl Time to live for the lock
     * @param timeUnit Time unit for TTL
     * @return true if lock acquired, false otherwise
     */
    public boolean acquireLock(String lockKey, long ttl, TimeUnit timeUnit) {
        try {
            String lockValue = serviceInstanceId + ":" + Instant.now().toEpochMilli();

            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(timeUnit.toSeconds(ttl)));

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Lock acquired successfully: key={}, value={}, ttl={}s",
                        lockKey, lockValue, timeUnit.toSeconds(ttl));
                return true;
            } else {
                String existingValue = redisTemplate.opsForValue().get(lockKey);
                log.warn("Failed to acquire lock: key={}, existing_holder={}", lockKey, existingValue);
                return false;
            }

        } catch (Exception e) {
            log.error("Error acquiring lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Try to acquire a distributed lock (non-blocking)
     * This is essentially an alias for acquireLock but with clearer semantics for atomic operations
     * @param lockKey The key to lock
     * @param ttl Time to live for the lock
     * @param timeUnit Time unit for TTL
     * @return true if lock acquired immediately, false otherwise
     */
    public boolean tryLock(String lockKey, long ttl, TimeUnit timeUnit) {
        log.debug("Attempting to acquire lock atomically: key={}", lockKey);
        return acquireLock(lockKey, ttl, timeUnit);
    }

    /**
     * Release a distributed lock
     * @param lockKey The key to unlock
     * @return true if lock released, false if not held by this instance
     */
    public boolean releaseLock(String lockKey) {
        try {
            String expectedValue = getCurrentLockValue(lockKey);
            if (expectedValue == null) {
                log.warn("Attempted to release lock not held by this instance: key={}", lockKey);
                return false;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(lockKey), expectedValue);

            boolean released = result != null && result == 1L;
            if (released) {
                log.info("Lock released successfully: key={}", lockKey);
            } else {
                log.warn("Failed to release lock (not owned by this instance): key={}", lockKey);
            }

            return released;

        } catch (Exception e) {
            log.error("Error releasing lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Check if a lock is currently held
     * @param lockKey The key to check
     * @return true if lock exists, false otherwise
     */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("Error checking lock status: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Get the current lock holder information
     * @param lockKey The key to check
     * @return Lock holder information or null if not locked
     */
    public String getLockHolder(String lockKey) {
        try {
            return redisTemplate.opsForValue().get(lockKey);
        } catch (Exception e) {
            log.error("Error getting lock holder: key={}", lockKey, e);
            return null;
        }
    }

    /**
     * Extend an existing lock TTL
     * @param lockKey The key to extend
     * @param ttl New TTL
     * @param timeUnit Time unit for TTL
     * @return true if extended, false if not held by this instance
     */
    public boolean extendLock(String lockKey, long ttl, TimeUnit timeUnit) {
        try {
            String expectedValue = getCurrentLockValue(lockKey);
            if (expectedValue == null) {
                return false;
            }

            DefaultRedisScript<Long> script = new DefaultRedisScript<>(EXTEND_LOCK_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockKey),
                    expectedValue,
                    String.valueOf(timeUnit.toSeconds(ttl)));

            boolean extended = result != null && result == 1L;
            if (extended) {
                log.info("Lock extended successfully: key={}, new_ttl={}s", lockKey, timeUnit.toSeconds(ttl));
            }

            return extended;

        } catch (Exception e) {
            log.error("Error extending lock: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * Get all locks held by this service instance
     */
    public Set<String> getLocksHeldByThisInstance() {
        try {
            Set<String> allLockKeys = redisTemplate.keys("saga:lock:*");
            return allLockKeys.stream()
                    .filter(key -> {
                        String value = redisTemplate.opsForValue().get(key);
                        return value != null && value.startsWith(serviceInstanceId + ":");
                    })
                    .collect(java.util.stream.Collectors.toSet());
        } catch (Exception e) {
            log.error("Error getting locks held by this instance", e);
            return Collections.emptySet();
        }
    }

    /**
     * Release all locks held by this service instance (for graceful shutdown)
     */
    public void releaseAllLocksForInstance() {
        try {
            Set<String> heldLocks = getLocksHeldByThisInstance();
            for (String lockKey : heldLocks) {
                releaseLock(lockKey);
            }
            log.info("Released {} locks during shutdown", heldLocks.size());
        } catch (Exception e) {
            log.error("Error releasing locks during shutdown", e);
        }
    }

    // ===================== Lock Key Builders =====================

    /**
     * Build standard lock key for order payment operations
     */
    public static String buildPaymentLockKey(String orderId) {
        return "saga:lock:order:" + orderId + ":payment";
    }

    /**
     * Build standard lock key for order operations
     */
    public static String buildOrderLockKey(String orderId) {
        return "saga:lock:order:" + orderId + ":order";
    }

    /**
     * Build standard lock key for saga operations
     * This will be used for distributed saga coordination in Phase 2
     */
    public static String buildSagaLockKey(String sagaId) {
        return "saga:lock:saga:" + sagaId;
    }

    // ===================== Helper Methods =====================

    /**
     * Get current lock value for this instance
     */
    private String getCurrentLockValue(String lockKey) {
        String currentValue = redisTemplate.opsForValue().get(lockKey);
        if (currentValue != null && currentValue.startsWith(serviceInstanceId + ":")) {
            return currentValue;
        }
        return null;
    }
}
package com.graduation.schedulingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Simple service to monitor lock health
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LockMonitoringService {

    private final RedisLockService redisLockService;

    /**
     * Monitor lock health every 2 minutes (simple monitoring)
     */
    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void monitorLockHealth() {
        try {
            var heldLocks = redisLockService.getLocksHeldByThisInstance();

            if (!heldLocks.isEmpty()) {
                log.info("Lock health check - currently holding {} locks: {}",
                        heldLocks.size(), heldLocks);
            } else {
                log.debug("Lock health check - no locks currently held");
            }

        } catch (Exception e) {
            log.error("Error during lock health monitoring", e);
        }
    }
}
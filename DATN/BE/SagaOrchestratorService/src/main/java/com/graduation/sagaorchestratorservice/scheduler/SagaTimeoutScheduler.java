//package com.graduation.sagaorchestratorservice.scheduler;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
//import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
//import com.graduation.sagaorchestratorservice.service.SagaMonitoringService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.Duration;
//import java.time.Instant;
//import java.util.Optional;
//import java.util.concurrent.atomic.AtomicLong;
//
///**
// * Scheduler for saga timeout checking and cleanup operations
// */
//@Slf4j
//@Component
//@EnableScheduling
//@RequiredArgsConstructor
//public class SagaTimeoutScheduler {
//
//    private final SagaMonitoringService sagaMonitoringService;
//    private final OrderPurchaseSagaService sagaService;
//
//    // Configuration values from properties
//    @Value("${saga.timeout.default-minutes:10}")
//    private int defaultTimeoutMinutes;
//
//    @Value("${saga.timeout.payment-minutes:5}")
//    private int paymentTimeoutMinutes;
//
//    @Value("${saga.timeout.order-minutes:15}")
//    private int orderTimeoutMinutes;
//
//    // Metrics for monitoring scheduler health
//    private final AtomicLong timeoutCheckCount = new AtomicLong(0);
//    private final AtomicLong timedOutSagasCount = new AtomicLong(0);
//    private Instant lastSuccessfulCheck = Instant.now();
//
//    /**
//     * Check for timed-out sagas every 30 seconds
//     */
//    @Scheduled(fixedRate = 30000)
//    public void checkForTimeouts() {
//        long checkNumber = timeoutCheckCount.incrementAndGet();
//        log.debug(Constant.LOG_TIMEOUT_CHECK_RUNNING, checkNumber);
//
//        try {
//            Instant startTime = Instant.now();
//
//            // For now, we'll implement basic timeout logic
//            // In a full implementation, this would check against actual saga state repository
//            int timedOutCount = performTimeoutCheck();
//
//            // Update metrics
//            lastSuccessfulCheck = Instant.now();
//            long duration = Duration.between(startTime, lastSuccessfulCheck).toMillis();
//
//            if (timedOutCount > 0) {
//                log.warn(Constant.LOG_TIMEOUT_CHECK_COMPLETED,
//                        checkNumber, duration, timedOutCount);
//                timedOutSagasCount.addAndGet(timedOutCount);
//            } else {
//                log.debug(Constant.LOG_TIMEOUT_CHECK_NO_TIMEOUTS,
//                        checkNumber, duration);
//            }
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_TIMEOUT_CHECK, checkNumber, e);
//        }
//    }
//
//    /**
//     * Perform the actual timeout check logic
//     */
//    private int performTimeoutCheck() {
//        int timedOutCount = 0;
//
//        try {
//            // Get current health status to check for long-running sagas
//            SagaMonitoringService.HealthStatus health = sagaMonitoringService.getHealthStatus();
//
//            if (health.activeCount > 0) {
//                log.debug(Constant.LOG_CHECKING_ACTIVE_SAGAS, health.activeCount);
//
//                // Actual timeout logic implementation
//                timedOutCount = checkActualTimeouts();
//            }
//
//        } catch (Exception e) {
//            log.error("Error performing timeout check", e);
//            throw e;
//        }
//
//        return timedOutCount;
//    }
//
//    /**
//     * Check for actual timed-out sagas using repository queries
//     */
//    private int checkActualTimeouts() {
//        int timedOutCount = 0;
//
//        try {
//            // Check for different timeout scenarios
//            timedOutCount += checkPaymentTimeouts();
//            timedOutCount += checkOrderTimeouts();
//            timedOutCount += checkDefaultTimeouts();
//
//        } catch (Exception e) {
//            log.error("Error checking for timed-out sagas", e);
//            throw e;
//        }
//
//        return timedOutCount;
//    }
//
//    /**
//     * Check for payment step timeouts (5 minutes)
//     */
//    private int checkPaymentTimeouts() {
//        try {
//            return sagaService.checkForTimeoutsWithDuration(Duration.ofMinutes(paymentTimeoutMinutes));
//        } catch (Exception e) {
//            log.error("Error checking payment timeouts", e);
//            return 0;
//        }
//    }
//
//    /**
//     * Check for order step timeouts (15 minutes)
//     */
//    private int checkOrderTimeouts() {
//        try {
//            return sagaService.checkForTimeoutsWithDuration(Duration.ofMinutes(orderTimeoutMinutes));
//        } catch (Exception e) {
//            log.error("Error checking order timeouts", e);
//            return 0;
//        }
//    }
//
//    /**
//     * Check for default timeouts (10 minutes)
//     */
//    private int checkDefaultTimeouts() {
//        try {
//            return sagaService.checkForTimeoutsWithDuration(Duration.ofMinutes(defaultTimeoutMinutes));
//        } catch (Exception e) {
//            log.error("Error checking default timeouts", e);
//            return 0;
//        }
//    }
//
//    /**
//     * Get timeout duration for a specific saga type
//     */
//    public Duration getTimeoutForSagaType(String sagaType) {
//        if (sagaType == null) {
//            return Duration.ofMinutes(defaultTimeoutMinutes);
//        }
//
//        return switch (sagaType.toLowerCase()) {
//            case "payment", "payment-processing" -> Duration.ofMinutes(paymentTimeoutMinutes);
//            case "order", "order-processing" -> Duration.ofMinutes(orderTimeoutMinutes);
//            default -> Duration.ofMinutes(defaultTimeoutMinutes);
//        };
//    }
//
//    /**
//     * Check if a saga has timed out based on its start time and type
//     */
//    public boolean hasSagaTimedOut(String sagaType, Instant sagaStartTime) {
//        if (sagaStartTime == null) {
//            return false;
//        }
//
//        Duration timeout = getTimeoutForSagaType(sagaType);
//        Instant timeoutThreshold = Instant.now().minus(timeout);
//
//        return sagaStartTime.isBefore(timeoutThreshold);
//    }
//
//    /**
//     * Manual timeout check for specific saga
//     */
//    public boolean checkSagaTimeout(String sagaId, String sagaType, Instant startTime) {
//        if (hasSagaTimedOut(sagaType, startTime)) {
//            log.warn("Saga {} of type {} has timed out (started at {})",
//                    sagaId, sagaType, startTime);
//
//            // Here we would trigger compensation logic
//            triggerSagaCompensation(sagaId, "TIMEOUT");
//
//            return true;
//        }
//
//        return false;
//    }
//
//    /**
//     * Trigger compensation for a timed-out saga
//     */
//    private void triggerSagaCompensation(String sagaId, String reason) {
//        log.warn(Constant.LOG_TRIGGERING_COMPENSATION, sagaId, reason);
//
//        try {
//            // Find and process timeout through the saga service
//            Optional<OrderPurchaseSagaState> optionalSaga = sagaService.findById(sagaId);
//
//            if (optionalSaga.isPresent()) {
//                OrderPurchaseSagaState saga = optionalSaga.get();
//
//                // Check if saga is still in a state where it can be timed out
//                if (saga.getStatus().isActive()) {
//                    // Trigger timeout handling through the saga service
//                    sagaService.handleSagaTimeoutManually(sagaId, reason);
//                    log.info(Constant.LOG_TIMEOUT_COMPENSATION_TRIGGERED, sagaId);
//                } else {
//                    log.info("Saga {} is no longer active, skipping timeout compensation", sagaId);
//                }
//            } else {
//                log.warn("Saga {} not found for timeout compensation", sagaId);
//            }
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_TRIGGERING_COMPENSATION, sagaId, e.getMessage());
//            // Record the failure in monitoring
//            sagaMonitoringService.recordSagaFailed(sagaId, reason + " - Compensation trigger failed: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Cleanup task that runs daily at 3 AM
//     */
//    @Scheduled(cron = "0 0 3 * * ?")
//    public void performDailyCleanup() {
//        log.info(Constant.LOG_STARTING_DAILY_CLEANUP);
//
//        try {
//            // Reset metrics counters (keep daily snapshots)
//            long totalTimeoutChecks = timeoutCheckCount.get();
//            long totalTimedOutSagas = timedOutSagasCount.get();
//
//            log.info(Constant.LOG_DAILY_SUMMARY,
//                    totalTimeoutChecks, totalTimedOutSagas);
//
//            // Additional cleanup tasks can be added here:
//            // - Archive old saga records
//            // - Clean up temporary data
//            // - Generate reports
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_DAILY_CLEANUP, e);
//        }
//    }
//
//    /**
//     * Get scheduler health information
//     */
//    public SchedulerHealth getHealthInfo() {
//        Duration timeSinceLastCheck = Duration.between(lastSuccessfulCheck, Instant.now());
//        boolean healthy = timeSinceLastCheck.toMinutes() < 2; // Consider unhealthy if no check in 2 minutes
//
//        return new SchedulerHealth(
//                healthy,
//                timeoutCheckCount.get(),
//                timedOutSagasCount.get(),
//                lastSuccessfulCheck,
//                timeSinceLastCheck
//        );
//    }
//
//    /**
//     * Health information for the scheduler
//     */
//    public static class SchedulerHealth {
//        public final boolean healthy;
//        public final long totalChecks;
//        public final long totalTimeouts;
//        public final Instant lastCheckTime;
//        public final Duration timeSinceLastCheck;
//
//        public SchedulerHealth(boolean healthy, long totalChecks, long totalTimeouts,
//                               Instant lastCheckTime, Duration timeSinceLastCheck) {
//            this.healthy = healthy;
//            this.totalChecks = totalChecks;
//            this.totalTimeouts = totalTimeouts;
//            this.lastCheckTime = lastCheckTime;
//            this.timeSinceLastCheck = timeSinceLastCheck;
//        }
//    }
//}
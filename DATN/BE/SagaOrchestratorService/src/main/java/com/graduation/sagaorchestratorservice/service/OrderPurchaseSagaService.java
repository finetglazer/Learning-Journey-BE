//package com.graduation.sagaorchestratorservice.service;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
//import com.graduation.sagaorchestratorservice.model.SagaEvent;
//import com.graduation.sagaorchestratorservice.model.enums.*;
//import com.graduation.sagaorchestratorservice.repository.OrderPurchaseSagaStateRepository;
//
//import com.graduation.sagaorchestratorservice.utils.SagaIdGenerator;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.time.Instant;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.locks.ReentrantLock;
//
///**
// * Service for managing Order Purchase Saga workflow
// * Handles the complete lifecycle from order creation to completion/compensation
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderPurchaseSagaService {
//
//    private final OrderPurchaseSagaStateRepository sagaRepository;
//    private final KafkaMessagePublisher messagePublisher;
//    private final IdempotencyService idempotencyService;
//    private final SagaMonitoringService monitoringService;
//    private final RedisLockService redisLockService;
//
//    @Value("${saga.retry.max-attempts:3}")
//    private int maxRetries;
//
//    @Value("${saga.timeout.default-minutes:10}")
//    private long defaultTimeoutMinutes;
//
//    // Kafka topic configuration
//    @Value("${kafka.topics.order-commands}")
//    private String orderCommandsTopic;
//
//    @Value("${kafka.topics.payment-commands}")
//    private String paymentCommandsTopic;
//
//    @Value("${saga.compensation.max-retries:3}")
//    private int defaultMaxCompensationRetries;
//
//    @Value("${saga.retry.delay-seconds:5}")
//    private int baseRetryDelaySeconds;
//
//    // Synchronization for preventing race conditions
//    private final ConcurrentHashMap<String, ReentrantLock> sagaLocks = new ConcurrentHashMap<>();
//
//    /**
//     * Start a new order purchase saga
//     * Called when an order is created and needs payment processing
//     */
//    @Transactional
//    public OrderPurchaseSagaState startSaga(String userId, Long orderId, String userEmail,
//                                            String userName, String orderDescription,
//                                            BigDecimal totalAmount, String existingSagaId) {
//
//        log.info(Constant.LOG_STARTING_ORDER_PURCHASE_SAGA, orderId, userId);
//
//        // Check if saga already exists for this order
//        Optional<OrderPurchaseSagaState> existingSaga = sagaRepository.findByOrderId(orderId);
//        if (existingSaga.isPresent()) {
//            log.warn(Constant.LOG_SAGA_ALREADY_EXISTS, orderId);
//            return existingSaga.get();
//        }
//
//        // Use the sagaId from the order (generated during order creation)
//        String sagaId = existingSagaId != null ? existingSagaId :
//                SagaIdGenerator.generateForType("ORDER_PURCHASE");
//
//        // Create and save the saga
//        OrderPurchaseSagaState saga = OrderPurchaseSagaState.initiate(
//                sagaId, userId, orderId, userEmail, userName, orderDescription, totalAmount);
//
//        sagaRepository.save(saga);
//
//        // Record in monitoring
//        monitoringService.recordSagaStarted(sagaId, "ORDER_PURCHASE");
//
//        // Process the first step (PROCESS_PAYMENT)
//        processNextStep(saga);
//
//        log.info(Constant.LOG_ORDER_PURCHASE_SAGA_STARTED, sagaId);
//        return saga;
//    }
//
//    /**
//     * Process the next step in the saga
//     */
//    @Transactional
//    public void processNextStep(OrderPurchaseSagaState saga) {
//        log.debug(Constant.LOG_PROCESSING_STEP, saga.getCurrentStep(), saga.getSagaId());
//
//        // Handle completion
//        if (saga.getCurrentStep() == OrderPurchaseSagaStep.COMPLETE_SAGA) {
//            completeSaga(saga);
//            return;
//        }
//
//        try {
//            // Create command message for current step
//            Map<String, Object> command = createCommandForCurrentStep(saga);
//            if (command == null) {
//                log.warn("No command created for step: {} in saga: {}",
//                        saga.getCurrentStep(), saga.getSagaId());
//                return;
//            }
//
//            // Save saga state before sending command
//            sagaRepository.save(saga);
//
//            // Determine target topic and publish command
//            String targetTopic = getTopicForCommand(saga.getCurrentStep().getCommandType());
//
//            messagePublisher.publishSagaStepCommand(
//                    saga.getSagaId(),
//                    saga.getCurrentStep().getStepNumber(),
//                    saga.getCurrentStep().getCommandType().name(),
//                    command,
//                    targetTopic
//            );
//
//            log.info(Constant.LOG_PUBLISHED_COMMAND,
//                    saga.getCurrentStep().getCommandType(), saga.getSagaId(), targetTopic);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_STEP, saga.getCurrentStep(), saga.getSagaId(), e);
//            handleStepFailure(saga, "Failed to process step: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Handle incoming event messages from services
//     */
//    @Transactional
//    public void handleEventMessage(Map<String, Object> eventData) {
//        String sagaId = (String) eventData.get(Constant.FIELD_SAGA_ID);
//        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
//        Boolean success = (Boolean) eventData.get(Constant.FIELD_SUCCESS);
//
//        log.debug(Constant.LOG_HANDLING_EVENT, eventType, sagaId);
//
//        // Use saga-specific lock to prevent race conditions
//        ReentrantLock lock = getSagaLock(sagaId);
//        lock.lock();
//        try {
//            // Find the saga
//            Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
//            if (optionalSaga.isEmpty()) {
//                log.warn("Received event for unknown saga: {}", sagaId);
//                return;
//            }
//
//            OrderPurchaseSagaState saga = optionalSaga.get();
//
//            // Check idempotency
//            String messageId = (String) eventData.get(Constant.FIELD_MESSAGE_ID);
//            ActionType actionType = isCompensationEvent(eventType) ? ActionType.COMPENSATION : ActionType.FORWARD;
//
//            if (idempotencyService.isProcessed(messageId, sagaId,
//                    saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null,
//                    eventType, actionType)) {
//                log.info(Constant.LOG_EVENT_ALREADY_PROCESSED, eventType, sagaId);
//                return;
//            }
//
//            try {
//                // Validate event matches current step
//                if (!isEventForCurrentStep(saga, eventType)) {
//                    log.warn(Constant.LOG_EVENT_IGNORED_WRONG_STEP,
//                            eventType, saga.getCurrentStep(), sagaId);
//                    recordEventProcessing(eventData, saga, "Event ignored - doesn't match current step");
//                    return;
//                }
//
//                // Process based on success/failure
//                if (Boolean.TRUE.equals(success)) {
//                    processSuccessEvent(saga, eventData);
//                } else {
//                    processFailureEvent(saga, eventData);
//                }
//
//                // Record successful processing
//                recordEventProcessing(eventData, saga, "Event processed successfully");
//
//            } catch (Exception e) {
//                log.error("Error handling event {} for saga {}", eventType, sagaId, e);
//                recordEventProcessing(eventData, saga, "Error processing event: " + e.getMessage());
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    // Helper method to determine if event is compensation
//    private boolean isCompensationEvent(String eventType) {
//        return eventType.contains("FAILED") || eventType.contains("COMPENSATION") || eventType.contains("ROLLBACK");
//    }
//
//    /**
//     * Process a successful event
//     */
//    private void processSuccessEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
//        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
//        log.info(Constant.LOG_PROCESSING_SUCCESS_EVENT, eventType, saga.getSagaId());
//
//        // Update saga with event data
//        updateSagaWithEventData(saga, eventData);
//
//        // Record monitoring
//        long processingTime = System.currentTimeMillis(); // Simple timing
//        monitoringService.recordMessageProcessed(saga.getSagaId(), eventType, processingTime);
//
//        if (saga.getStatus() == SagaStatus.COMPENSATING) {
//            // Handle compensation step success
//            handleCompensationStepSuccess(saga);
//        } else {
//            // Normal flow - move to next step
//            saga.addEvent(SagaEvent.stepCompleted(saga.getCurrentStep().getDescription()));
//            saga.moveToNextStep();
//        }
//
//        // Save and continue
//        sagaRepository.save(saga);
//
//        // Process next step if still active
//        if (saga.getStatus() == SagaStatus.IN_PROGRESS || saga.getStatus() == SagaStatus.COMPENSATING) {
//            processNextStep(saga);
//        }
//    }
//
//    /**
//     * Process a failure event
//     */
//    private void processFailureEvent(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
//        String eventType = (String) eventData.get(Constant.FIELD_TYPE);
//        String errorMessage = (String) eventData.get(Constant.FIELD_ERROR_MESSAGE);
//
//        log.warn(Constant.LOG_PROCESSING_FAILURE_EVENT, eventType, saga.getSagaId(), errorMessage);
//
//        // Record monitoring
//        monitoringService.recordMessageFailed(saga.getSagaId(), eventType, errorMessage);
//
//        // Check if this is a compensation step failure
//        if (saga.getStatus() == SagaStatus.COMPENSATING) {
//            // Handle compensation step failure
//            handleCompensationStepFailure(saga, errorMessage != null ? errorMessage : Constant.ERROR_STEP_FAILED);
//        } else {
//            // Handle normal step failure
//            handleStepFailure(saga, errorMessage != null ? errorMessage : Constant.ERROR_STEP_FAILED);
//        }
//    }
//
//    /**
//     * Handle step failure and start compensation if needed
//     */
//    private void handleStepFailure(OrderPurchaseSagaState saga, String reason) {
//        log.warn(Constant.LOG_HANDLING_STEP_FAILURE, saga.getSagaId(), reason);
//
//        saga.handleFailure(reason, saga.getCurrentStep().name());
//
//        // Start compensation if this step requires it
//        if (saga.getCurrentStep().requiresCompensation()) {
//            startCompensation(saga);
//        } else {
//            // For non-compensatable steps, just mark as failed
//            saga.setStatus(SagaStatus.COMPENSATION_COMPLETED);
//            saga.setEndTime(Instant.now());
//            monitoringService.recordSagaFailed(saga.getSagaId(), reason);
//        }
//
//        sagaRepository.save(saga);
//    }
//
//    /**
//     * Start compensation process
//     */
//    private void startCompensation(OrderPurchaseSagaState saga) {
//        log.info(Constant.LOG_STARTING_COMPENSATION, saga.getSagaId());
//
//        saga.startCompensation();
//        sagaRepository.save(saga);
//
//        // Process compensation steps
//        processNextStep(saga);
//    }
//
//    /**
//     * Handle successful completion of a compensation step
//     */
//    private void handleCompensationStepSuccess(OrderPurchaseSagaState saga) {
//        OrderPurchaseSagaStep currentStep = saga.getCurrentStep();
//        log.debug(Constant.LOG_COMPENSATION_STEP_COMPLETED, currentStep, saga.getSagaId());
//
//        // Add compensation step to completed steps
//        saga.getCompletedSteps().add(currentStep.name());
//
//        // Get next compensation step
//        OrderPurchaseSagaStep nextStep = currentStep.getNextCompensationStep();
//
//        if (nextStep == OrderPurchaseSagaStep.COMPLETE_SAGA) {
//            // Compensation completed
//            saga.completeCompensation();
//            monitoringService.recordSagaFailed(saga.getSagaId(), "Compensated: " + saga.getFailureReason());
//        } else {
//            // Move to next compensation step
//            saga.setCurrentStep(nextStep);
//            saga.setCurrentStepStartTime(Instant.now());
//            saga.setLastUpdatedTime(Instant.now());
//            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_STEP,
//                    "Moving to compensation step: " + nextStep.getDescription()));
//        }
//    }
//
//
//    /**
//     * Handle failed compensation step
//     */
//    private void handleCompensationStepFailure(OrderPurchaseSagaState saga, String reason) {
//        log.warn(Constant.LOG_COMPENSATION_STEP_FAILED, saga.getSagaId(), reason);
//
//        // Retry compensation or mark as compensation failed
//        if (saga.getCompensationRetryCount() < saga.getMaxCompensationRetries()) {
//            // Retry compensation step
//            saga.incrementCompensationRetryCount();
//            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_RETRY,
//                    "Retrying compensation step " + saga.getCurrentStep() + " (attempt " + saga.getCompensationRetryCount() + ")"));
//
//            log.info(Constant.LOG_RETRYING_COMPENSATION,
//                    saga.getSagaId(), saga.getCompensationRetryCount(), saga.getMaxCompensationRetries());
//
//            sagaRepository.save(saga);
//
//            // Retry the current compensation step
//            processNextStep(saga);
//
//        } else {
//            // Mark as compensation failed - manual intervention needed
//            log.error(Constant.LOG_COMPENSATION_FAILED_FINAL,
//                    saga.getSagaId(), saga.getMaxCompensationRetries());
//
//            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
//            saga.setEndTime(Instant.now());
//            saga.addEvent(SagaEvent.of(Constant.SAGA_EVENT_COMPENSATION_FAILED,
//                    String.format(Constant.DESC_COMPENSATION_FAILED, saga.getMaxCompensationRetries(), reason)));
//
//            sagaRepository.save(saga);
//
//            // Record the compensation failure
//            monitoringService.recordSagaFailed(saga.getSagaId(),
//                    String.format(Constant.ERROR_COMPENSATION_FAILED, reason));
//        }
//    }
//
//
//    /**
//     * Complete saga successfully
//     */
//    private void completeSaga(OrderPurchaseSagaState saga) {
//        log.info("Completing saga successfully: {}", saga.getSagaId());
//
//        saga.setStatus(SagaStatus.COMPLETED);
//        saga.setEndTime(Instant.now());
//        saga.addEvent(SagaEvent.sagaCompleted());
//
//        sagaRepository.save(saga);
//        monitoringService.recordSagaCompleted(saga.getSagaId());
//
//        // Clean up locks for completed saga
//        cleanupCompletedSagaLocks(saga.getSagaId());
//    }
//
//    /**
//     * Create command for the current step
//     */
//    private Map<String, Object> createCommandForCurrentStep(OrderPurchaseSagaState saga) {
//        Map<String, Object> command = new HashMap<>();
//        command.put(Constant.FIELD_SAGA_ID, saga.getSagaId());
//        command.put(Constant.FIELD_MESSAGE_ID, UUID.randomUUID().toString());
//        command.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
//
//        // Add step-specific payload
//        switch (saga.getCurrentStep()) {
//            case PROCESS_PAYMENT:
//                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
//                command.put(Constant.FIELD_USER_ID, saga.getUserId());
//                command.put(Constant.FIELD_TOTAL_AMOUNT, saga.getTotalAmount());
//                break;
//
//            case UPDATE_ORDER_STATUS_CONFIRMED:
//                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
//                command.put(Constant.FIELD_NEW_STATUS, Constant.ORDER_STATUS_CONFIRMED);
//                command.put(Constant.FIELD_REASON, Constant.REASON_PAYMENT_PROCESSED_SUCCESS);
//                break;
//
//            case UPDATE_ORDER_STATUS_DELIVERED:
//                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
//                command.put(Constant.FIELD_NEW_STATUS, Constant.ORDER_STATUS_DELIVERED);
//                command.put(Constant.FIELD_REASON, Constant.REASON_ORDER_CONFIRMED_SUCCESS);
//                break;
//
//            case CANCEL_PAYMENT:
//                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
//                command.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, saga.getPaymentTransactionId());
//                command.put(Constant.FIELD_REASON, saga.getFailureReason());
//                break;
//
//            case CANCEL_ORDER:
//                command.put(Constant.FIELD_ORDER_ID, saga.getOrderId().toString());
//                command.put(Constant.FIELD_REASON, saga.getFailureReason());
//                command.put(Constant.FIELD_CANCELLED_BY, Constant.ACTOR_SAGA_COMPENSATION);
//                break;
//
//            default:
//                log.warn("No command mapping for step: {}", saga.getCurrentStep());
//                return null;
//        }
//
//        return command;
//    }
//
//    /**
//     * Get Kafka topic for command type
//     */
//    private String getTopicForCommand(CommandType commandType) {
//        return switch (commandType) {
//            case PAYMENT_PROCESS, PAYMENT_REVERSE -> paymentCommandsTopic;
//            case ORDER_UPDATE_CONFIRMED, ORDER_UPDATE_DELIVERED, ORDER_CANCEL -> orderCommandsTopic;
//            default -> "saga.dlq"; // Dead letter queue fallback
//        };
//    }
//
//    /**
//     * Check if event matches current saga step
//     */
//    private boolean isEventForCurrentStep(OrderPurchaseSagaState saga, String eventType) {
//        if (saga.getCurrentStep() == null) {
//            return false;
//        }
//
//        try {
//            EventType event = EventType.valueOf(eventType);
//            CommandType expectedCommandType = saga.getCurrentStep().getCommandType();
//            CommandType eventCommandType = event.getAssociatedCommandType();
//
//            return eventCommandType == expectedCommandType;
//        } catch (Exception e) {
//            log.error("Error checking event match for saga {}: {}", saga.getSagaId(), e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Update saga with data from event
//     */
//    private void updateSagaWithEventData(OrderPurchaseSagaState saga, Map<String, Object> eventData) {
//        String eventType = (String) eventData.get("type");
//
//        // Extract specific data based on event type
//        switch (eventType) {
//            case "PAYMENT_PROCESSED":
//                Object paymentTransactionId = eventData.get("paymentTransactionId");
//                if (paymentTransactionId != null) {
//                    saga.setPaymentTransactionId(Long.valueOf(paymentTransactionId.toString()));
//                }
//                break;
//            // Add other event data extraction as needed
//        }
//    }
//
//    /**
//     * Record event processing for idempotency
//     */
//    private void recordEventProcessing(Map<String, Object> eventData, OrderPurchaseSagaState saga, String result) {
//        String messageId = (String) eventData.get("messageId");
//        String eventType = (String) eventData.get("type");
//        Integer stepId = saga.getCurrentStep() != null ? saga.getCurrentStep().getStepNumber() : null;
//
//        Map<String, Object> processingResult = new HashMap<>();
//        processingResult.put("result", result);
//        processingResult.put("sagaStatus", saga.getStatus().name());
//        processingResult.put("currentStep", saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");
//
//        idempotencyService.recordProcessing(messageId, saga.getSagaId(), stepId, eventType, processingResult);
//    }
//
//    /**
//     * Check for timed-out sagas and handle them
//     */
//    public void checkForTimeouts() {
//        log.debug("Checking for timed-out saga steps");
//
//        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
//        Instant cutoffTime = Instant.now().minus(Duration.ofMinutes(defaultTimeoutMinutes));
//
//        List<OrderPurchaseSagaState> timedOutSagas = sagaRepository.findSagasWithStepTimeout(activeStatuses, cutoffTime);
//
//        for (OrderPurchaseSagaState saga : timedOutSagas) {
//            handleSagaTimeout(saga);
//        }
//    }
//
//    /**
//     * Check for timed-out sagas with specific duration and handle them
//     */
//    public int checkForTimeoutsWithDuration(Duration timeout) {
//        log.debug("Checking for timed-out saga steps with timeout: {}", timeout);
//
//        List<SagaStatus> activeStatuses = Arrays.asList(SagaStatus.STARTED, SagaStatus.IN_PROGRESS, SagaStatus.COMPENSATING);
//        Instant cutoffTime = Instant.now().minus(timeout);
//
//        List<OrderPurchaseSagaState> timedOutSagas = sagaRepository.findSagasWithStepTimeout(activeStatuses, cutoffTime);
//
//        log.debug("Found {} timed-out sagas with timeout {}", timedOutSagas.size(), timeout);
//
//        for (OrderPurchaseSagaState saga : timedOutSagas) {
//            handleSagaTimeout(saga);
//        }
//
//        return timedOutSagas.size();
//    }
//
//    /**
//     * Handle a timed-out saga
//     */
//    private void handleSagaTimeout(OrderPurchaseSagaState saga) {
//        log.warn("Saga step timed out: {}", saga.getSagaId());
//
//        if (saga.getRetryCount() < saga.getMaxRetries()) {
//            // Retry the step with exponential backoff delay
//            saga.incrementRetryCount();
//            saga.addEvent(SagaEvent.of("RETRY", "Retrying step " + saga.getCurrentStep() + " after timeout (attempt " + saga.getRetryCount() + ")"));
//            sagaRepository.save(saga);
//
//            // Apply exponential backoff delay
//            scheduleRetryWithDelay(saga);
//        } else {
//            // Exceeded retries, start compensation
//            handleStepFailure(saga, "Step timed out after " + saga.getMaxRetries() + " retries");
//        }
//    }
//
//    /**
//     * Handle a timed-out saga manually (called from scheduler)
//     */
//    @Transactional
//    public void handleSagaTimeoutManually(String sagaId, String reason) {
//        log.warn("Handling manual timeout for saga: {} - {}", sagaId, reason);
//
//        // Use saga-specific lock to prevent race conditions
//        ReentrantLock lock = getSagaLock(sagaId);
//        lock.lock();
//        try {
//            Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
//            if (optionalSaga.isPresent()) {
//                OrderPurchaseSagaState saga = optionalSaga.get();
//
//                // Only handle timeout if saga is still active
//                if (saga.getStatus().isActive()) {
//                    handleSagaTimeout(saga);
//                } else {
//                    log.info("Saga {} is no longer active, skipping timeout handling", sagaId);
//                }
//            } else {
//                log.warn("Saga {} not found for manual timeout handling", sagaId);
//            }
//        } finally {
//            lock.unlock();
//        }
//    }
//
//    /**
//     * Schedule a retry with exponential backoff delay
//     */
//    private void scheduleRetryWithDelay(OrderPurchaseSagaState saga) {
//        long delayMs = calculateRetryDelay(saga.getRetryCount());
//
//        log.info("Scheduling retry for saga {} with delay of {}ms (attempt {})",
//                saga.getSagaId(), delayMs, saga.getRetryCount());
//
//        // Use CompletableFuture for async delay
//        java.util.concurrent.CompletableFuture.delayedExecutor(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
//            .execute(() -> {
//                try {
//                    // Reload saga to ensure we have the latest state
//                    Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(saga.getSagaId());
//                    if (optionalSaga.isPresent()) {
//                        OrderPurchaseSagaState currentSaga = optionalSaga.get();
//                        // Only retry if still in a retryable state
//                        if (currentSaga.getStatus().isActive() && currentSaga.getRetryCount() <= currentSaga.getMaxRetries()) {
//                            log.info("Executing delayed retry for saga {} (attempt {})",
//                                    currentSaga.getSagaId(), currentSaga.getRetryCount());
//                            processNextStep(currentSaga);
//                        } else {
//                            log.warn("Saga {} state changed during retry delay, skipping retry", saga.getSagaId());
//                        }
//                    } else {
//                        log.warn("Saga {} not found during delayed retry", saga.getSagaId());
//                    }
//                } catch (Exception e) {
//                    log.error("Error during delayed retry for saga {}", saga.getSagaId(), e);
//                }
//            });
//    }
//
//    /**
//     * Calculate retry delay using exponential backoff
//     * Base delay * (2^(retryCount-1)) with jitter
//     */
//    private long calculateRetryDelay(int retryCount) {
//        // Base delay in milliseconds
//        long baseDelayMs = baseRetryDelaySeconds * 1000L;
//
//        // Exponential backoff: base * 2^(retryCount-1)
//        long exponentialDelay = baseDelayMs * (1L << (retryCount - 1));
//
//        // Add jitter (±20% random variation)
//        double jitter = 0.8 + (Math.random() * 0.4); // 0.8 to 1.2
//        long delayWithJitter = Math.round(exponentialDelay * jitter);
//
//        // Cap at maximum delay (60 seconds)
//        long maxDelayMs = 60 * 1000L;
//        return Math.min(delayWithJitter, maxDelayMs);
//    }
//
//    /**
//     * Get or create a saga-specific lock for synchronization
//     */
//    private ReentrantLock getSagaLock(String sagaId) {
//        return sagaLocks.computeIfAbsent(sagaId, k -> new ReentrantLock());
//    }
//
//    /**
//     * Clean up locks for completed sagas to prevent memory leaks
//     */
//    private void cleanupCompletedSagaLocks(String sagaId) {
//        sagaLocks.remove(sagaId);
//    }
//
//    // Repository access methods
//    public Optional<OrderPurchaseSagaState> findById(String sagaId) {
//        return sagaRepository.findById(sagaId);
//    }
//
//    public Optional<OrderPurchaseSagaState> findByOrderId(Long orderId) {
//        return sagaRepository.findByOrderId(orderId);
//    }
//
//    public List<OrderPurchaseSagaState> findByUserId(String userId) {
//        return sagaRepository.findByUserId(userId);
//    }
//
//    public List<OrderPurchaseSagaState> findActiveSagas() {
//        return sagaRepository.findActiveSagas();
//    }
//
//    /**
//     * Cancel saga by user request with Redis lock checking
//     * Uses existing compensation strategy and infrastructure
//     */
//    @Transactional
//    public boolean cancelSagaByUser(String sagaId, String orderId, String reason) {
//        log.info("Processing user cancellation request: sagaId={}, orderId={}, reason={}", sagaId, orderId, reason);
//
//        // Step 1: Check if payment is currently being processed (Redis lock check)
//        String paymentLockKey = RedisLockService.buildPaymentLockKey(orderId);
//
//        if (redisLockService.isLocked(paymentLockKey)) {
//            String lockHolder = redisLockService.getLockHolder(paymentLockKey);
//            log.warn("Cannot cancel saga - payment in progress: sagaId={}, orderId={}, lockHolder={}",
//                    sagaId, orderId, lockHolder);
//
//            return false;
//        }
//
//        // Step 2: Use existing ReentrantLock for saga-specific thread safety
//        ReentrantLock sagaLock = getSagaLock(sagaId);
//        sagaLock.lock();
//        try {
//            // Step 3: Validate saga is in active state and proceed
//            Optional<OrderPurchaseSagaState> optionalSaga = sagaRepository.findById(sagaId);
//            if (optionalSaga.isEmpty()) {
//                log.warn("Saga not found for cancellation: sagaId={}", sagaId);
//                return false;
//            }
//
//            OrderPurchaseSagaState saga = optionalSaga.get();
//
//            // Validate saga is in active state
//            if (saga.getStatus() == SagaStatus.COMPLETED || saga.getStatus() == SagaStatus.FAILED) {
//                log.warn("Cannot cancel saga in final state: sagaId={}, status={}", sagaId, saga.getStatus());
//                return false;
//            }
//
//            log.info("Payment not in progress, proceeding with saga cancellation: sagaId={}", sagaId);
//
//            // Step 4: Set failure reason for user cancellation
//            saga.setFailureReason("User cancellation: " + reason);
//            saga.addEvent(SagaEvent.of("USER_CANCELLATION_REQUESTED",
//                    "User requested cancellation: " + reason));
//
//            // Step 5: Use existing compensation strategy and infrastructure
//            // Your existing startCompensation() method will:
//            // - Determine compensation strategy based on completed steps
//            // - Execute compensation steps in reverse order (payment → order)
//            // - Use existing processNextStep() for step execution
//            startCompensation(saga);
//
//            log.info("User cancellation initiated successfully: sagaId={}, orderId={}", sagaId, orderId);
//            return true;
//
//        } catch (Exception e) {
//            log.error("Error during saga cancellation: sagaId={}, orderId={}", sagaId, orderId, e);
//            return false;
//        } finally {
//            sagaLock.unlock();
//        }
//    }
//}
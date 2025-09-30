package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.PaymentTransaction;
import com.graduation.schedulingservice.model.ProcessedMessage;
import com.graduation.schedulingservice.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling payment commands from the Saga Orchestrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandHandlerService {

    private final PaymentTransactionRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdempotencyService idempotencyService;
    private final RedisLockService redisLockService;

    /**
     * Handle process payment command WITH DISTRIBUTED LOCKING
     * Mock implementation that randomly succeeds or fails
     * Step: idempotency check -> acquire lock -> logic solving -> record processing -> publish event -> release lock
     */
    @Transactional
    public void handleProcessPayment(Map<String, Object> command) {
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);

        // 1. PRESERVE EXISTING: Check if the command has already been processed
        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_COMMAND_ALREADY_PROCESSED, sagaId, messageId);
            return; // Skip processing if already handled
        }

        // 2. PRESERVE EXISTING: Extract the payload which contains the actual command data
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        if (payload == null) {
            log.error(Constant.LOG_COMMAND_PAYLOAD_NULL, sagaId);
            publishPaymentEvent(sagaId, null, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_INVALID_COMMAND_FORMAT);
            return;
        }

        String orderId = (String) payload.get(Constant.FIELD_ORDER_ID);
        String userId = (String) payload.get(Constant.FIELD_USER_ID);
        BigDecimal amount = new BigDecimal(payload.get(Constant.FIELD_TOTAL_AMOUNT).toString());
        String paymentMethod = (String) payload.getOrDefault(Constant.FIELD_PAYMENT_METHOD, Constant.DEFAULT_PAYMENT_METHOD);

        // 3. PRESERVE EXISTING: Validate above fields from the payload
        if (orderId == null || orderId.isEmpty() || userId == null || userId.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error(Constant.LOG_INVALID_PAYMENT_COMMAND_DATA, sagaId, orderId, userId, amount);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_INVALID_PAYMENT_DATA);
            return;
        }

        // 4. NEW: Acquire distributed lock before payment processing
        String lockKey = RedisLockService.buildPaymentLockKey(orderId);

        log.info("Attempting to acquire payment lock: orderId={}, sagaId={}, lockKey={}",
                orderId, sagaId, lockKey);

        if (redisLockService.acquireLock(lockKey, 1, TimeUnit.MINUTES)) {
            try {
                log.info("Payment lock acquired, processing payment: orderId={}, sagaId={}", orderId, sagaId);

                // 5. PRESERVE EXISTING: Call existing payment processing logic
                processPaymentLogic(sagaId, messageId, orderId, userId, amount, paymentMethod);

            } finally {
                // 6. NEW: Always release lock in finally block
                boolean released = redisLockService.releaseLock(lockKey);
                log.info("Payment lock released: orderId={}, sagaId={}, released={}", orderId, sagaId, released);
            }
        } else {
            // 7. NEW: Lock acquisition failed - payment already in progress
            String lockHolder = redisLockService.getLockHolder(lockKey);
            String errorMessage = String.format(
                    "Payment processing already in progress for order %s. Lock held by: %s",
                    orderId, lockHolder);

            log.warn("Payment lock acquisition failed: orderId={}, sagaId={}, lockHolder={}", orderId, sagaId, lockHolder);

            // Record processing failure and publish event
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishPaymentEvent(sagaId, orderId, null, Constant.EVENT_PAYMENT_FAILED, false,
                    null, errorMessage);
        }
    }

    /**
     * PRESERVE EXISTING: Extracted payment processing logic (all existing logic preserved)
     */
    private void processPaymentLogic(String sagaId, String messageId, String orderId,
                                     String userId, BigDecimal amount, String paymentMethod) {

        log.info(Constant.LOG_PROCESSING_PAYMENT_ORDER, orderId, amount, sagaId);

        try {
            // PRESERVE EXISTING: Create payment transaction
            PaymentTransaction transaction = PaymentTransaction.createForSaga(
                    orderId, userId, amount, sagaId, paymentMethod);

            // PRESERVE EXISTING: Process payment (mock implementation)
            transaction.processPayment();

            // PRESERVE EXISTING: Save transaction
            PaymentTransaction savedTransaction = paymentRepository.save(transaction);

            // PRESERVE EXISTING: Publish event based on payment result
            if (savedTransaction.getStatus().isSuccessful()) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_PROCESSED, true,
                        Constant.PAYMENT_PROCESSED_SUCCESS, null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                String reason = savedTransaction.getMockDecisionReason() != null
                        ? savedTransaction.getMockDecisionReason()
                        : Constant.REASON_PAYMENT_DECLINED;

                publishPaymentEvent(sagaId, orderId, savedTransaction.getId(),
                        Constant.EVENT_PAYMENT_FAILED, false,
                        null, reason);
            }

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_COMMAND, e.getMessage(), e);
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);

            // PRESERVE EXISTING: Publish failure event
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_FAILED, false,
                    null, Constant.ERROR_TECHNICAL_PREFIX + e.getMessage());
        }
    }

    /**
     * Handle reverse payment command WITH DISTRIBUTED LOCKING
     * PRESERVE ALL EXISTING LOGIC
     */
    @Transactional
    public void handleReversePayment(Map<String, Object> command) {
        // PRESERVE EXISTING: Processed message check
        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);

        if (idempotencyService.isProcessed(messageId, sagaId)) {
            log.info(Constant.LOG_COMMAND_ALREADY_PROCESSED, sagaId, messageId);
            return; // Skip processing if already handled
        }

        // PRESERVE EXISTING: Extract and validate payload
        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
        String orderId = (String) payload.get(Constant.FIELD_ORDER_ID);
        String reason = (String) payload.get(Constant.FIELD_REASON);
        String paymentTransactionId = String.valueOf(payload.get(Constant.FIELD_PAYMENT_TRANSACTION_ID));

        // PRESERVE EXISTING: Check if payload is valid
        if (orderId == null || orderId.isEmpty() || paymentTransactionId == null || paymentTransactionId.isEmpty() || reason == null || reason.isEmpty()) {
            log.error(Constant.LOG_INVALID_REVERSE_PAYMENT_DATA, sagaId, orderId, paymentTransactionId);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                    null, Constant.ERROR_INVALID_REVERSE_DATA);
            return;
        }

        // NEW: Acquire distributed lock for payment reversal
        String lockKey = RedisLockService.buildPaymentLockKey(orderId);

        log.info("Attempting to acquire payment lock for reversal: orderId={}, sagaId={}", orderId, sagaId);

        if (redisLockService.acquireLock(lockKey, 1, TimeUnit.MINUTES)) {
            try {
                log.info("Payment reversal lock acquired: orderId={}, sagaId={}", orderId, sagaId);

                // PRESERVE EXISTING: Call existing reversal logic
                processPaymentReversalLogic(sagaId, messageId, orderId, reason);

            } finally {
                boolean released = redisLockService.releaseLock(lockKey);
                log.info("Payment reversal lock released: orderId={}, sagaId={}, released={}", orderId, sagaId, released);
            }
        } else {
            String lockHolder = redisLockService.getLockHolder(lockKey);
            String errorMessage = String.format(
                    "Cannot reverse payment - payment operation in progress for order %s. Lock held by: %s",
                    orderId, lockHolder);

            log.warn("Payment reversal lock acquisition failed: orderId={}, sagaId={}, lockHolder={}", orderId, sagaId, lockHolder);

            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            publishPaymentEvent(sagaId, orderId, null, Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                    null, errorMessage);
        }
    }

    /**
     * PRESERVE EXISTING: Extracted payment reversal logic (all existing logic preserved)
     */
    private void processPaymentReversalLogic(String sagaId, String messageId, String orderId, String reason) {

        log.info(Constant.LOG_REVERSING_PAYMENT_ORDER, orderId, sagaId);

        try {
            // PRESERVE EXISTING: Simple mock - randomly succeed or fail
            boolean success = Math.random() > 0.2; // 80% success rate

            if (success) {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_REVERSED, true,
                        Constant.PAYMENT_REVERSED_SUCCESS, null);

            } else {
                idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
                publishPaymentEvent(sagaId, orderId, null,
                        Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                        null, Constant.REASON_PAYMENT_FAILED);
            }

        } catch (Exception e) {
            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_COMMAND, e.getMessage(), e);
            publishPaymentEvent(sagaId, orderId, null,
                    Constant.EVENT_PAYMENT_REVERSE_FAILED, false,
                    null, Constant.ERROR_TECHNICAL_PREFIX + e.getMessage());
        }
    }

    /**
     * PRESERVE EXISTING: Publish payment event back to saga orchestrator
     * (Keep all existing logic exactly as is)
     */
    private void publishPaymentEvent(String sagaId, String orderId, Long paymentTransactionId,
                                     String eventType, boolean success, String successMessage, String errorMessage) {
        try {
            // TODO: Inject KafkaTemplate when available
            // For now, just log the event that would be published

            log.info(Constant.LOG_PUBLISHING_PAYMENT_EVENT,
                    sagaId, orderId, eventType, success);

            if (success) {
                log.info(Constant.EVENT_PAYMENT_PROCESSED, successMessage);
            } else {
                log.error(Constant.EVENT_PAYMENT_FAILED, errorMessage);
            }

            Map<String, Object> event = new HashMap<>();
            event.put(Constant.FIELD_MESSAGE_ID, Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, Constant.UUID_SUBSTRING_LENGTH));
            event.put(Constant.FIELD_SAGA_ID, sagaId);
            event.put(Constant.FIELD_TYPE, eventType);
            event.put(Constant.FIELD_SUCCESS, success);
            event.put(Constant.FIELD_ORDER_ID, orderId);
            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());

            if (paymentTransactionId != null) {
                event.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, paymentTransactionId);
            }

            if (success) {
                event.put(Constant.RESPONSE_MESSAGE, successMessage);
            } else {
                event.put(Constant.FIELD_ERROR_MESSAGE, errorMessage);
            }

            kafkaTemplate.send(Constant.TOPIC_PAYMENT_EVENTS, sagaId, event);

        } catch (Exception e) {
            log.error(Constant.LOG_ERROR_PUBLISHING_EVENT, e.getMessage(), e);
        }
    }

    /**
     * PRESERVE EXISTING: Generate unique message ID
     */
    private String generateMessageId() {
        return Constant.PREFIX_PAYMENT_MESSAGE + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }
}
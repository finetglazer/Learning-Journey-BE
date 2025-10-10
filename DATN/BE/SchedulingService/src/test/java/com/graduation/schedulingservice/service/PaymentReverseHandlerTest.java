//package com.graduation.schedulingservice.service;
//
//import com.graduation.schedulingservice.constant.Constant;
//import com.graduation.schedulingservice.model.ProcessedMessage;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.kafka.core.KafkaTemplate;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.TimeUnit;
//
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Unit tests for PaymentCommandHandlerService.handleReversePayment - Top 3 Most Important Test Cases
// */
//@ExtendWith(MockitoExtension.class)
//class PaymentReverseHandlerTest {
//
//    @Mock
//    private IdempotencyService idempotencyService;
//
//    @Mock
//    private RedisLockService redisLockService;
//
//    @Mock
//    private PaymentTransactionRepository paymentRepository;
//
//    @Mock
//    private KafkaTemplate<String, Object> kafkaTemplate;
//
//    @InjectMocks
//    private PaymentCommandHandlerService paymentCommandHandlerService;
//
//    private Map<String, Object> command;
//    private Map<String, Object> payload;
//    private String sagaId;
//    private String messageId;
//    private String orderId;
//    private String reason;
//    private String paymentTransactionId;
//    private String lockKey;
//
//    @BeforeEach
//    void setUp() {
//        sagaId = UUID.randomUUID().toString();
//        messageId = "msg-" + UUID.randomUUID().toString();
//        orderId = "order-123";
//        reason = "Order cancelled by customer";
//        paymentTransactionId = "txn-456";
//        lockKey = "payment:lock:" + orderId;
//
//        // Set up payload
//        payload = new HashMap<>();
//        payload.put(Constant.FIELD_ORDER_ID, orderId);
//        payload.put(Constant.FIELD_REASON, reason);
//        payload.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, paymentTransactionId);
//
//        // Set up command
//        command = new HashMap<>();
//        command.put(Constant.FIELD_SAGA_ID, sagaId);
//        command.put(Constant.FIELD_MESSAGE_ID, messageId);
//        command.put(Constant.FIELD_PAYLOAD, payload);
//    }
//
//    /**
//     * TEST CASE 1: IDEMPOTENCY - Most Critical for Preventing Duplicate Reversal Processing
//     * This test ensures the same reversal command is not processed twice, which is crucial
//     * for payment systems to prevent double reversals.
//     */
//    @Test
//    @DisplayName("Should skip processing when reversal message is already processed (idempotency)")
//    void handleReversePayment_whenMessageAlreadyProcessed_shouldSkipProcessing() {
//        // Arrange
//        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(true);
//
//        // Act
//        paymentCommandHandlerService.handleReversePayment(command);
//
//        // Assert
//        verify(idempotencyService, times(1)).isProcessed(messageId, sagaId);
//
//        // Ensure no further processing occurs
//        verify(redisLockService, never()).acquireLock(anyString(), anyLong(), any(TimeUnit.class));
//        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
//        verify(idempotencyService, never()).recordProcessing(anyString(), anyString(), any());
//    }
//
//    /**
//     * TEST CASE 2: SUCCESSFUL REVERSAL WITH LOCK - Happy Path
//     * This test covers the main business flow where everything works correctly:
//     * lock acquired, reversal processing executed, event published.
//     * Note: We don't mock Math.random() to avoid StackOverflowError - we test that processing occurs.
//     */
//    @Test
//    @DisplayName("Should process payment reversal when lock is acquired")
//    void handleReversePayment_whenLockAcquiredSuccessfully_shouldProcessReversal() {
//        // Arrange
//        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
//        when(redisLockService.acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn(true);
//        when(redisLockService.releaseLock(anyString())).thenReturn(true);
//
//        // Act
//        paymentCommandHandlerService.handleReversePayment(command);
//
//        // Assert - Verify lock acquisition and release
//        verify(redisLockService, times(1)).acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES));
//        verify(redisLockService, times(1)).releaseLock(anyString());
//
//        // Verify that processing occurs (either success or failure)
//        // Since the method uses Math.random(), we verify that some processing happened
//        verify(idempotencyService, times(1)).recordProcessing(
//                eq(messageId), eq(sagaId), any(ProcessedMessage.ProcessStatus.class));
//        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
//    }
//
//    /**
//     * TEST CASE 3: LOCK ACQUISITION FAILURE - Critical for Preventing Race Conditions
//     * This test ensures proper handling when another process is already processing
//     * a payment operation for the same order, preventing concurrent operations.
//     */
//    @Test
//    @DisplayName("Should handle lock acquisition failure gracefully during reversal")
//    void handleReversePayment_whenLockAcquisitionFails_shouldPublishFailureEvent() {
//        // Arrange
//        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
//        when(redisLockService.acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn(false);
//        when(redisLockService.getLockHolder(anyString())).thenReturn("payment-process-789");
//
//        // Act
//        paymentCommandHandlerService.handleReversePayment(command);
//
//        // Assert - Verify lock acquisition attempt
//        verify(redisLockService, times(1)).acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES));
//        verify(redisLockService, times(1)).getLockHolder(anyString());
//
//        // Verify failure is recorded and failure event is published
//        verify(idempotencyService, times(1)).recordProcessing(
//                messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
//        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
//
//        // Verify no lock release attempt (since lock was never acquired)
//        verify(redisLockService, never()).releaseLock(anyString());
//    }
//
//    /**
//     * BONUS TEST CASE: INVALID PAYLOAD - Data Validation
//     * This test ensures proper handling when payload data is invalid
//     */
//    @Test
//    @DisplayName("Should handle invalid payload gracefully")
//    void handleReversePayment_whenPayloadInvalid_shouldPublishFailureEvent() {
//        // Arrange
//        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
//
//        // Create command with invalid payload (missing orderId)
//        Map<String, Object> invalidPayload = new HashMap<>();
//        invalidPayload.put(Constant.FIELD_REASON, reason);
//        invalidPayload.put(Constant.FIELD_PAYMENT_TRANSACTION_ID, paymentTransactionId);
//        // Missing FIELD_ORDER_ID
//
//        Map<String, Object> invalidCommand = new HashMap<>();
//        invalidCommand.put(Constant.FIELD_SAGA_ID, sagaId);
//        invalidCommand.put(Constant.FIELD_MESSAGE_ID, messageId);
//        invalidCommand.put(Constant.FIELD_PAYLOAD, invalidPayload);
//
//        // Act
//        paymentCommandHandlerService.handleReversePayment(invalidCommand);
//
//        // Assert - Verify no lock operations occur
//        verify(redisLockService, never()).acquireLock(anyString(), anyLong(), any(TimeUnit.class));
//        verify(redisLockService, never()).releaseLock(anyString());
//
//        // Verify failure event is published for invalid data
//        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
//
//        // Verify no processing is recorded (because validation fails before processing)
//        verify(idempotencyService, never()).recordProcessing(anyString(), anyString(), any());
//    }
//}
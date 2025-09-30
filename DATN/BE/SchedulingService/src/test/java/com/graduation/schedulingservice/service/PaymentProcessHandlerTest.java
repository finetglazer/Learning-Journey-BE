package com.graduation.schedulingservice.service;

import com.graduation.schedulingservice.constant.Constant;
import com.graduation.schedulingservice.model.PaymentTransaction;
import com.graduation.schedulingservice.model.PaymentStatus;
import com.graduation.schedulingservice.model.ProcessedMessage;
import com.graduation.schedulingservice.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentCommandHandlerService - Top 3 Most Important Test Cases
 */
@ExtendWith(MockitoExtension.class)
class PaymentProcessHandlerTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentCommandHandlerService paymentCommandHandlerService;

    private Map<String, Object> command;
    private Map<String, Object> payload;
    private String sagaId;
    private String messageId;
    private String orderId;
    private String userId;
    private BigDecimal amount;
    private String lockKey;

    @BeforeEach
    void setUp() {
        sagaId = UUID.randomUUID().toString();
        messageId = "msg-" + UUID.randomUUID().toString();
        orderId = "order-123";
        userId = "user-456";
        amount = new BigDecimal("100.00");
        lockKey = "payment:lock:" + orderId;

        // Set up payload
        payload = new HashMap<>();
        payload.put(Constant.FIELD_ORDER_ID, orderId);
        payload.put(Constant.FIELD_USER_ID, userId);
        payload.put(Constant.FIELD_TOTAL_AMOUNT, amount);
        payload.put(Constant.FIELD_PAYMENT_METHOD, "CREDIT_CARD");

        // Set up command
        command = new HashMap<>();
        command.put(Constant.FIELD_SAGA_ID, sagaId);
        command.put(Constant.FIELD_MESSAGE_ID, messageId);
        command.put(Constant.FIELD_PAYLOAD, payload);
    }

    /**
     * TEST CASE 1: IDEMPOTENCY - Most Critical for Preventing Duplicate Processing
     * This test ensures the same command is not processed twice, which is crucial
     * for payment systems to prevent double charges.
     */
    @Test
    @DisplayName("Should skip processing when message is already processed (idempotency)")
    void handleProcessPayment_whenMessageAlreadyProcessed_shouldSkipProcessing() {
        // Arrange
        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(true);

        // Act
        paymentCommandHandlerService.handleProcessPayment(command);

        // Assert
        verify(idempotencyService, times(1)).isProcessed(messageId, sagaId);

        // Ensure no further processing occurs
        verify(redisLockService, never()).acquireLock(anyString(), anyLong(), any(TimeUnit.class));
        verify(paymentRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        verify(idempotencyService, never()).recordProcessing(anyString(), anyString(), any());
    }

    /**
     * TEST CASE 2: SUCCESSFUL PAYMENT WITH LOCK - Happy Path
     * This test covers the main business flow where everything works correctly:
     * lock acquired, payment processed, transaction saved, event published.
     */
    @Test
    @DisplayName("Should process payment successfully when lock is acquired")
    void handleProcessPayment_whenLockAcquiredSuccessfully_shouldProcessPayment() {
        // Arrange
        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
        when(redisLockService.acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(redisLockService.releaseLock(anyString())).thenReturn(true);

        // Mock PaymentTransaction creation and processing
        PaymentTransaction mockTransaction = mock(PaymentTransaction.class);
        // Use actual PaymentStatus enum instead of mocking it
        when(mockTransaction.getStatus()).thenReturn(PaymentStatus.CONFIRMED);
        when(mockTransaction.getId()).thenReturn(123L);

        try (MockedStatic<PaymentTransaction> paymentTransactionMock = mockStatic(PaymentTransaction.class)) {
            paymentTransactionMock.when(() -> PaymentTransaction.createForSaga(
                            orderId, userId, amount, sagaId, "CREDIT_CARD"))
                    .thenReturn(mockTransaction);

            when(paymentRepository.save(mockTransaction)).thenReturn(mockTransaction);

            // Act
            paymentCommandHandlerService.handleProcessPayment(command);

            // Assert - Verify lock acquisition and release
            verify(redisLockService, times(1)).acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES));
            verify(redisLockService, times(1)).releaseLock(anyString());

            // Verify payment processing
            verify(paymentRepository, times(1)).save(mockTransaction);
            verify(mockTransaction, times(1)).processPayment();

            // Verify success recording and event publishing
            verify(idempotencyService, times(1)).recordProcessing(
                    messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        }
    }

    /**
     * TEST CASE 3: LOCK ACQUISITION FAILURE - Critical for Preventing Race Conditions
     * This test ensures proper handling when another process is already processing
     * the same payment, which is essential for preventing concurrent payment processing.
     */
    @Test
    @DisplayName("Should handle lock acquisition failure gracefully")
    void handleProcessPayment_whenLockAcquisitionFails_shouldPublishFailureEvent() {
        // Arrange
        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
        when(redisLockService.acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn(false);
        when(redisLockService.getLockHolder(anyString())).thenReturn("another-process-123");

        // Act
        paymentCommandHandlerService.handleProcessPayment(command);

        // Assert - Verify lock acquisition attempt
        verify(redisLockService, times(1)).acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES));
        verify(redisLockService, times(1)).getLockHolder(anyString());

        // Verify no payment processing occurs
        verify(paymentRepository, never()).save(any());

        // Verify failure is recorded and failure event is published
        verify(idempotencyService, times(1)).recordProcessing(
                messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());

        // Verify no lock release attempt (since lock was never acquired)
        verify(redisLockService, never()).releaseLock(anyString());
    }

    /**
     * BONUS TEST CASE: PAYMENT PROCESSING FAILURE - Exception Handling
     * This test ensures proper handling when payment processing fails
     */
    @Test
    @DisplayName("Should handle payment processing failure gracefully")
    void handleProcessPayment_whenPaymentProcessingFails_shouldPublishFailureEvent() {
        // Arrange
        when(idempotencyService.isProcessed(messageId, sagaId)).thenReturn(false);
        when(redisLockService.acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES))).thenReturn(true);
        when(redisLockService.releaseLock(anyString())).thenReturn(true);

        // Mock PaymentTransaction creation but make processing fail
        PaymentTransaction mockTransaction = mock(PaymentTransaction.class);
        when(mockTransaction.getStatus()).thenReturn(PaymentStatus.FAILED);
        when(mockTransaction.getId()).thenReturn(123L);
        when(mockTransaction.getMockDecisionReason()).thenReturn("Insufficient funds");

        try (MockedStatic<PaymentTransaction> paymentTransactionMock = mockStatic(PaymentTransaction.class)) {
            paymentTransactionMock.when(() -> PaymentTransaction.createForSaga(
                            orderId, userId, amount, sagaId, "CREDIT_CARD"))
                    .thenReturn(mockTransaction);

            when(paymentRepository.save(mockTransaction)).thenReturn(mockTransaction);

            // Act
            paymentCommandHandlerService.handleProcessPayment(command);

            // Assert - Verify lock acquisition and release
            verify(redisLockService, times(1)).acquireLock(anyString(), eq(1L), eq(TimeUnit.MINUTES));
            verify(redisLockService, times(1)).releaseLock(anyString());

            // Verify payment processing occurred
            verify(paymentRepository, times(1)).save(mockTransaction);
            verify(mockTransaction, times(1)).processPayment();

            // Verify failure is recorded and failure event is published
            verify(idempotencyService, times(1)).recordProcessing(
                    messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
        }
    }
}
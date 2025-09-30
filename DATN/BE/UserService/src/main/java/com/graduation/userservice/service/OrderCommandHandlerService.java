//package com.graduation.userservice.service;
//
//import com.graduation.userservice.constant.Constant;
//import com.graduation.userservice.model.Order;
//import com.graduation.userservice.model.OrderStatus;
//import com.graduation.userservice.model.ProcessedMessage;
//import com.graduation.userservice.repository.OrderRepository;
//import com.graduation.userservice.utils.SagaIdGenerator;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * Service for Order business logic
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderCommandHandlerService {
//
//    private final OrderRepository orderRepository;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    private final IdempotencyService idempotencyService;
//    private final RedisLockService redisLockService;
//
//    /**
//     * Create a new order and trigger saga
//     */
//    @Transactional
//    public Order createOrder(String userId, String userEmail, String userName,
//                             String orderDescription, BigDecimal totalAmount, String shippingAddress) {
//
//        log.info(Constant.LOG_CREATING_ORDER, userId, totalAmount);
//
//        try {
//            // Generate sagaId immediately when creating the order
//            String sagaId = SagaIdGenerator.generateForType("ORDER_PURCHASE");
//
//            // Create the order with sagaId
//            Order order = Order.createOrder(userId, userEmail, userName,
//                    orderDescription, totalAmount,
//                    shippingAddress, sagaId);
//
//            // Save to database
//            Order savedOrder = orderRepository.save(order);
//            log.info(Constant.LOG_ORDER_CREATED_SUCCESS, savedOrder.getId());
//
//            // Publish event to trigger saga (saga will use existing sagaId)
//            publishOrderCreatedEvent(savedOrder);
//
//            return savedOrder;
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_CREATING_ORDER, userId, e);
//            throw new RuntimeException(String.format(Constant.ERROR_FAILED_TO_CREATE_ORDER_RUNTIME, e.getMessage()), e);
//        }
//    }
//
//    /**
//     * Publish OrderCreated event to trigger saga
//     */
//    private void publishOrderCreatedEvent(Order order) {
//        try {
//            // Create event payload
//            Map<String, Object> event = new HashMap<>();
//            event.put(Constant.FIELD_MESSAGE_ID, generateMessageId());
//            event.put(Constant.FIELD_TYPE, Constant.EVENT_ORDER_CREATED);
//            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
//
//            // Order data for saga (including sagaId)
//            event.put(Constant.FIELD_ORDER_ID, order.getId());
//            event.put(Constant.FIELD_USER_ID, order.getUserId());
//            event.put(Constant.FIELD_USER_EMAIL, order.getUserEmail());
//            event.put(Constant.FIELD_USER_NAME, order.getUserName());
//            event.put(Constant.FIELD_ORDER_DESCRIPTION, order.getOrderDescription());
//            event.put(Constant.FIELD_TOTAL_AMOUNT, order.getTotalAmount());
//            event.put(Constant.FIELD_ORDER_STATUS, order.getStatus().name());
//            event.put(Constant.FIELD_CREATED_AT, order.getCreatedAt().toString());
//            event.put(Constant.FIELD_SAGA_ID, order.getSagaId()); // Include sagaId
//
//            // Publish to saga events topic
//            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, order.getId().toString(), event);
//
//            log.info(Constant.LOG_PUBLISHED_ORDER_CREATED, order.getId());
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_FAILED_PUBLISH_ORDER_CREATED, order.getId(), e);
//        }
//    }
//
//    /**
//     * Update order status (called by saga)
//     * Steps: check processed message -> validate data -> logic solving -> record data
//     */
//    @Transactional
//    public void updateOrderStatus(Long orderId, OrderStatus newStatus, String reason, String sagaId) {
//        log.info(Constant.LOG_UPDATING_ORDER_STATUS, orderId, newStatus, sagaId);
//
//        Optional<Order> optionalOrder = orderRepository.findByIdWithHistories(orderId);
//        if (optionalOrder.isEmpty()) {
//            throw new RuntimeException(String.format(Constant.ERROR_ORDER_NOT_FOUND_ID, orderId));
//        }
//
//        Order order = optionalOrder.get();
//
//        // Update saga ID if provided
//        if (sagaId != null && order.getSagaId() == null) {
//            order.setSagaId(sagaId);
//        }
//        // Update status with history
//        order.updateStatus(newStatus, reason, Constant.ACTOR_SAGA_ORCHESTRATOR);
//
//        orderRepository.save(order);
//        log.info(Constant.LOG_ORDER_STATUS_UPDATED, orderId, newStatus);
//    }
//
//    /**
//     * Handle order update confirmed command
//     */
//    public void handleUpdateOrderConfirmed(Map<String, Object> command) {
//
//        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
//        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
//
//        String sagaId = command.get(Constant.FIELD_SAGA_ID).toString();
//        //idempotency check
//        if (idempotencyService.isProcessed(messageId, sagaId)) {
//            log.info(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
//            return; // Skip processing if already handled
//        }
//        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_CONFIRMED_SUCCESS);
//        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());
//
//        // validate reason and orderId
//        if (validatePayload(sagaId, messageId, orderId, reason)) return;
//
//        try {
//            log.info(Constant.LOG_UPDATING_ORDER_CONFIRMED, orderId, sagaId);
//            updateOrderStatus(orderId, OrderStatus.CONFIRMED, reason, sagaId);
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
//
//            // set up wait for 10 seconds before publishing event
//            try {
//                Thread.sleep(10000); // 10 seconds delay
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                log.warn(Constant.LOG_THREAD_INTERRUPTED, sagaId);
//            }
//
//            // Publish success event
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATED_CONFIRMED, true,
//                    Constant.STATUS_DESC_CONFIRMED, null);
//            //record idempotency
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_UPDATING_CONFIRMED, e.getMessage(), e);
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
//                    null, e.getMessage());
//        }
//    }
//
//    /**
//     * Handle order update delivered command
//     */
//    public void handleUpdateOrderDelivered(Map<String, Object> command) {
//        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
//        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
//        // Idempotency check
//        if (idempotencyService.isProcessed(messageId, sagaId)) {
//            log.info(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
//            return; // Skip processing if already handled
//        }
//
//        // Extract payload first (consistent with other handlers)
//        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
//
//        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());
//        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_DELIVERED_SUCCESS);
//        // Validate orderId and reason
//        if (validatePayload(sagaId, messageId, orderId, reason)) return;
//
//        log.info(Constant.LOG_UPDATING_ORDER_DELIVERED, orderId, sagaId);
//
//        try {
//
//            // Update order status to DELIVERED
//            updateOrderStatus(orderId, OrderStatus.DELIVERED, reason, sagaId);
//
//            // Add 10-second delay before sending event to saga
//            log.info(Constant.LOG_PROCESSING_DELIVERY_WAIT, sagaId);
//            try {
//                Thread.sleep(10000); // 10 seconds delay
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                log.warn(Constant.LOG_THREAD_INTERRUPTED, sagaId);
//            }
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
//
//            // Publish success event
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATED_DELIVERED, true,
//                    Constant.STATUS_DESC_DELIVERED, null);
//
//            // Log saga completion
//            log.info(Constant.LOG_SAGA_COMPLETED, sagaId, orderId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_UPDATING_DELIVERED, e.getMessage(), e);
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
//                    null, e.getMessage());
//
//            // Log saga failure
//            log.error(Constant.LOG_SAGA_FAILED, sagaId, orderId, e.getMessage());
//        }
//    }
//
//    private boolean validatePayload(String sagaId, String messageId, Long orderId, String reason) {
//        if (orderId <= 0 || reason == null || reason.isEmpty()) {
//            log.error(Constant.LOG_INVALID_ORDER_ID, orderId, sagaId);
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
//            publishOrderEvent(sagaId, null, Constant.EVENT_ORDER_STATUS_UPDATE_FAILED, false,
//                    null, Constant.ERROR_INVALID_ORDER_ID);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Check if payment is currently being processed for the given order
//     */
//    public boolean isPaymentInProgress(String paymentLockKey) {
//        try {
//            return redisLockService.isLocked(paymentLockKey);
//        } catch (Exception e) {
//            log.error("Error checking payment lock status: key={}", paymentLockKey, e);
//            // In case of error, assume payment might be in progress to be safe
//            return true;
//        }
//    }
//
//    /**
//     * Get the current payment lock holder information
//     */
//    public String getPaymentLockHolder(String paymentLockKey) {
//        try {
//            return redisLockService.getLockHolder(paymentLockKey);
//        } catch (Exception e) {
//            log.error("Error getting payment lock holder: key={}", paymentLockKey, e);
//            return "unknown";
//        }
//    }
//
//
//    /**
//     * Initiate order cancellation by publishing cancel request event to Saga
//     */
//    @Transactional
//    public void initiateCancellation(Order order, String reason) {
//        try {
//            log.info("Initiating cancellation for order: {}", order.getId());
//
//            // Publish "CancelRequestReceived" event to Saga
//            publishCancelRequestEvent(order, reason);
//
//            log.info("Cancel request event published for order: {}", order.getId());
//
//        } catch (Exception e) {
//            log.error("Failed to initiate cancellation for order: {}", order.getId(), e);
//            throw new RuntimeException("Failed to initiate order cancellation: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Handle order update cancelled command
//     */
//    public void handleUpdateOrderCancelled(Map<String, Object> command) {
//        String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
//        String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
//
//        // Idempotency check
//        if (idempotencyService.isProcessed(messageId, sagaId)) {
//            log.info(Constant.LOG_MESSAGE_ALREADY_PROCESSED, messageId, sagaId);
//            return; // Skip processing if already handled
//        }
//
//        // Extract payload first (consistent with other handlers)
//        Map<String, Object> payload = (Map<String, Object>) command.get(Constant.FIELD_PAYLOAD);
//
//        Long orderId = Long.valueOf(payload.get(Constant.FIELD_ORDER_ID).toString());
//        String reason = (String) payload.getOrDefault(Constant.FIELD_REASON, Constant.REASON_ORDER_CANCELLED_SUCCESS);
//
//        // Validate orderId and reason
//        if (validatePayload(sagaId, messageId, orderId, reason)) return;
//
//        log.info(Constant.LOG_UPDATING_ORDER_CANCELLED, orderId, sagaId);
//
//        try {
//            // Update order status to CANCELLED
//            updateOrderStatus(orderId, OrderStatus.CANCELLED, reason, sagaId);
//
//            // Add 10-second delay before sending event to saga
//            log.info(Constant.LOG_PROCESSING_CANCELLATION_WAIT, sagaId);
//            try {
//                Thread.sleep(10000); // 10 seconds delay
//            } catch (InterruptedException ie) {
//                Thread.currentThread().interrupt();
//                log.warn(Constant.LOG_THREAD_INTERRUPTED, sagaId);
//            }
//
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.SUCCESS);
//
//            // Publish success event
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_CANCELLED, true,
//                    Constant.STATUS_DESC_CANCELLED, null);
//
//            // Log saga completion for cancellation
//            log.info(Constant.LOG_SAGA_CANCELLATION_COMPLETED, sagaId, orderId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_UPDATING_CANCELLED, e.getMessage(), e);
//            idempotencyService.recordProcessing(messageId, sagaId, ProcessedMessage.ProcessStatus.FAILED);
//            publishOrderEvent(sagaId, orderId, Constant.EVENT_ORDER_CANCELLATION_FAILED, false,
//                    null, e.getMessage());
//
//            // Log saga failure for cancellation
//            log.error(Constant.LOG_SAGA_CANCELLATION_FAILED, sagaId, orderId, e.getMessage());
//        }
//    }
//
//    /**
//     * Atomically update order status with database-level locking
//     * Prevents race conditions during status transitions
//     */
//    @Transactional
//    public boolean updateOrderStatusAtomically(Long orderId, OrderStatus expectedCurrentStatus,
//                                               OrderStatus newStatus, String reason, String changedBy) {
//        log.info("Attempting atomic status update: orderId={}, from={}, to={}",
//                orderId, expectedCurrentStatus, newStatus);
//
//        try {
//            // Use pessimistic write lock to ensure atomicity
//            Optional<Order> optionalOrder = orderRepository.findByIdForAtomicUpdate(orderId);
//
//            if (optionalOrder.isEmpty()) {
//                log.warn("Order not found for atomic update: orderId={}", orderId);
//                return false;
//            }
//
//            Order order = optionalOrder.get();
//
//            // Compare-and-swap: only update if current status matches expected
//            if (order.getStatus() != expectedCurrentStatus) {
//                log.warn("Atomic update failed - status mismatch: orderId={}, expected={}, actual={}",
//                        orderId, expectedCurrentStatus, order.getStatus());
//                return false;
//            }
//
//            // Perform the atomic update
//            order.updateStatus(newStatus, reason, changedBy);
//            orderRepository.save(order);
//
//            log.info("Atomic status update successful: orderId={}, from={}, to={}",
//                    orderId, expectedCurrentStatus, newStatus);
//            return true;
//
//        } catch (Exception e) {
//            log.error("Error during atomic status update: orderId={}, from={}, to={}",
//                    orderId, expectedCurrentStatus, newStatus, e);
//            return false;
//        }
//    }
//
//    /**
//     * Publish cancel request event to trigger saga cancellation flow
//     */
//    private void publishCancelRequestEvent(Order order, String reason) {
//        try {
//            // Create cancel request event payload
//            Map<String, Object> event = new HashMap<>();
//            event.put(Constant.FIELD_MESSAGE_ID, generateMessageId());
//            event.put(Constant.FIELD_TYPE, Constant.EVENT_CANCEL_REQUEST_RECEIVED);
//            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
//
//            // Order data for saga
//            event.put(Constant.FIELD_ORDER_ID, order.getId());
//            event.put(Constant.FIELD_USER_ID, order.getUserId());
//            event.put(Constant.FIELD_SAGA_ID, order.getSagaId());
//            event.put(Constant.FIELD_ORDER_STATUS, order.getStatus().name());
//            event.put(Constant.FIELD_REASON, reason);
//            event.put(Constant.FIELD_USER_EMAIL, order.getUserEmail());
//            event.put(Constant.FIELD_USER_NAME, order.getUserName());
//            event.put(Constant.FIELD_TOTAL_AMOUNT, order.getTotalAmount());
//
//            // Publish to saga events topic
//            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, order.getId().toString(), event);
//
//            log.info("Published cancel request event for order: {} with saga: {}",
//                    order.getId(), order.getSagaId());
//
//        } catch (Exception e) {
//            log.error("Failed to publish cancel request event for order: {}", order.getId(), e);
//            throw new RuntimeException("Failed to publish cancel request event", e);
//        }
//    }
//
//    /**
//     * Publish order event back to saga orchestrator
//     */
//    private void publishOrderEvent(String sagaId, Long orderId, String eventType,
//                                   boolean success, String successMessage, String errorMessage) {
//        try {
//            // TODO: Inject KafkaTemplate when available
//            // For now, just log the event that would be published
//
//            log.info(Constant.LOG_PUBLISHING_ORDER_EVENT,
//                    sagaId, orderId, eventType, success);
//
//            if (success) {
//                log.info(Constant.LOG_ORDER_EVENT_SUCCESS, successMessage);
//            } else {
//                log.error(Constant.LOG_ORDER_EVENT_FAILURE, errorMessage);
//            }
//
//            Map<String, Object> event = new HashMap<>();
//            event.put(Constant.FIELD_MESSAGE_ID, Constant.PREFIX_ORDER_EVENT + System.currentTimeMillis());
//            event.put(Constant.FIELD_SAGA_ID, sagaId);
//            event.put(Constant.FIELD_TYPE, eventType);
//            event.put(Constant.FIELD_SUCCESS, success);
//            event.put(Constant.FIELD_ORDER_ID, orderId);
//            event.put(Constant.FIELD_TIMESTAMP, System.currentTimeMillis());
//
//            if (success) {
//                event.put(Constant.RESPONSE_MESSAGE, successMessage);
//            } else {
//                event.put(Constant.FIELD_ERROR_MESSAGE, errorMessage);
//            }
//
//            kafkaTemplate.send(Constant.TOPIC_ORDER_EVENTS, sagaId, event);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PUBLISHING_EVENT, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Generate unique message ID
//     */
//    private String generateMessageId() {
//        return Constant.PREFIX_ORDER_MESSAGE + System.currentTimeMillis() + "_" +
//                java.util.UUID.randomUUID().toString().substring(0, 8);
//    }
//}
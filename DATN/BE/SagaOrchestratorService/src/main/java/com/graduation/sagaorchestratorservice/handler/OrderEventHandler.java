//// ===================== OrderEventHandler.java =====================
//
//package com.graduation.sagaorchestratorservice.handler;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.Map;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OrderEventHandler {
//
//    private final OrderPurchaseSagaService orderPurchaseSagaService;
//
//    /**
//     * Handle order events from Order Service
//     */
//    public void handleOrderEvent(Map<String, Object> event) {
//        String eventType = (String) event.get(Constant.FIELD_TYPE);
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//
//        log.debug(Constant.LOG_PROCESSING_ORDER_EVENT, eventType, sagaId);
//
//        if (eventType.equals(Constant.EVENT_ORDER_CREATED)) {
//            handleOrderCreatedEvent(event);
//        } else if (eventType.equals(Constant.EVENT_CANCEL_REQUEST_RECEIVED)) {
//            handleCancelRequestReceived(event);
//        }
//        else {
//            orderPurchaseSagaService.handleEventMessage(event);
//        }
//    }
//
//    /**
//     * Handle ORDER_CREATED event to start new saga
//     */
//    private void handleOrderCreatedEvent(Map<String, Object> event) {
//        // Extract order data
//        String userId = (String) event.get(Constant.FIELD_USER_ID);
//        Long orderId = Long.valueOf(event.get(Constant.FIELD_ORDER_ID).toString());
//        String userEmail = (String) event.get(Constant.FIELD_USER_EMAIL);
//        String userName = (String) event.get(Constant.FIELD_USER_NAME);
//        String orderDescription = (String) event.get(Constant.FIELD_ORDER_DESCRIPTION);
//        BigDecimal totalAmount = new BigDecimal(event.get(Constant.FIELD_TOTAL_AMOUNT).toString());
//        String existingSagaId = (String) event.get(Constant.FIELD_SAGA_ID); // Get existing sagaId
//
//        try {
//            // Start saga with existing sagaId
//            orderPurchaseSagaService.startSaga(userId, orderId, userEmail, userName,
//                    orderDescription, totalAmount, existingSagaId);
//        } catch (Exception e) {
//            log.error(Constant.ERROR_HANDLING_ORDER_CREATED, e);
//            throw new RuntimeException(Constant.ERROR_FAILED_TO_START_SAGA, e);
//        }
//    }
//
//    /**
//     * Handle cancel request received from Order Service
//     */
//    private void handleCancelRequestReceived(Map<String, Object> event) {
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//        String orderId = String.valueOf(event.get(Constant.FIELD_ORDER_ID));
//        String reason = (String) event.get(Constant.FIELD_REASON);
//
//        log.info(Constant.LOG_CANCEL_REQUEST_RECEIVED, sagaId, orderId, reason);
//
//        try {
//            // Delegate to saga service for cancellation with lock checking
//            boolean cancelled = orderPurchaseSagaService.cancelSagaByUser(sagaId, orderId, reason);
//
//            if (!cancelled) {
//                log.warn("Saga cancellation was blocked or failed: sagaId={}, orderId={}", sagaId, orderId);
//            }
//
//        } catch (Exception e) {
//            log.error("Error handling cancel request for saga: {}, orderId: {}", sagaId, orderId, e);
//        }
//    }
//}
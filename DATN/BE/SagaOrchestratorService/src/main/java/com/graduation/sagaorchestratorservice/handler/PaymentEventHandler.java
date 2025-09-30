//// ===================== PaymentEventHandler.java =====================
//
//package com.graduation.sagaorchestratorservice.handler;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentEventHandler {
//
//    private final OrderPurchaseSagaService orderPurchaseSagaService;
//
//    /**
//     * Handle payment events from Payment Service
//     */
//    public void handlePaymentEvent(Map<String, Object> event) {
//        String eventType = (String) event.get(Constant.FIELD_TYPE);
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//
//        log.debug(Constant.LOG_PROCESSING_PAYMENT_EVENT, eventType, sagaId);
//
//        // Validate required fields
//        if (sagaId == null || sagaId.trim().isEmpty()) {
//            log.warn("Received payment event without sagaId, ignoring: {}", event);
//            return;
//        }
//
//        // Route event to saga service
//        orderPurchaseSagaService.handleEventMessage(event);
//    }
//}
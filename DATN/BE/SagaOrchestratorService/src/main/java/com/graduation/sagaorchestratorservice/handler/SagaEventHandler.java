//// ===================== SagaEventHandler.java =====================
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
//public class SagaEventHandler {
//
//    private final OrderPurchaseSagaService orderPurchaseSagaService;
//
//    /**
//     * Handle saga-specific events
//     */
//    public void handleSagaEvent(Map<String, Object> event) {
//        String eventType = (String) event.get(Constant.FIELD_TYPE);
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//
//        log.debug(Constant.LOG_PROCESSING_SAGA_EVENT, eventType, sagaId);
//
//        switch (eventType) {
//            case Constant.EVENT_SAGA_TIMEOUT_CHECK:
//                handleSagaTimeoutCheck(event);
//                break;
//            case Constant.EVENT_SAGA_MONITORING_UPDATE:
//                handleSagaMonitoringUpdate(event);
//                break;
////            case Constant.EVENT_SAGA_EXTERNAL_CANCEL_REQUEST:
////                handleExternalCancelRequest(event);
////                break;
//            default:
//                log.debug(Constant.LOG_UNHANDLED_SAGA_EVENT, eventType, sagaId);
//                break;
//        }
//    }
//
//    /**
//     * Handle saga timeout check events
//     */
//    private void handleSagaTimeoutCheck(Map<String, Object> event) {
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//        log.debug("Handling timeout check for saga: {}", sagaId);
//
//        try {
//            // Delegate to saga service for timeout handling
//            orderPurchaseSagaService.checkForTimeouts();
//
//        } catch (Exception e) {
//            log.error("Error handling timeout check for saga: {}", sagaId, e);
//        }
//    }
//
//    /**
//     * Handle saga monitoring update events
//     */
//    private void handleSagaMonitoringUpdate(Map<String, Object> event) {
//        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//        String updateType = (String) event.get(Constant.FIELD_UPDATE_TYPE);
//
//        log.debug("Handling monitoring update for saga: {} type: {}", sagaId, updateType);
//
//        // TODO: Implement monitoring update logic
//        // This could update dashboard metrics, send alerts, etc.
//    }
//
//    /**
//     * Handle external cancel request
//     */
////    private void handleExternalCancelRequest(Map<String, Object> event) {
////        String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
////        String requestedBy = (String) event.get(Constant.FIELD_REQUESTED_BY);
////        String reason = (String) event.get(Constant.FIELD_REASON);
////
////        log.info("Handling external cancel request for saga: {} by: {} reason: {}",
////                sagaId, requestedBy, reason);
////
////        try {
////            // Delegate to saga service for cancellation
////            orderPurchaseSagaService.cancelSagaByUser(sagaId);
////
////        } catch (Exception e) {
////            log.error("Error handling external cancel request for saga: {}", sagaId, e);
////        }
////    }
//
//    /**
//     * Handle DLQ messages for debugging and recovery
//     */
//    public void handleDlqMessage(Object messagePayload) {
//        try {
//            log.error(Constant.LOG_RECEIVED_DLQ_MESSAGE, messagePayload);
//
//            // TODO: Implement DLQ handling logic
//            // 1. Parse message to understand what failed
//            // 2. Log for debugging purposes
//            // 3. Optionally attempt manual recovery
//            // 4. Alert monitoring systems
//
//        } catch (Exception e) {
//            log.error(Constant.ERROR_PROCESSING_DLQ_MESSAGE, e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Handle health check messages
//     */
//    public void handleHealthCheckMessage(Map<String, Object> message) {
//        try {
//            log.debug(Constant.LOG_RECEIVED_HEALTH_CHECK, message.get(Constant.FIELD_TIMESTAMP));
//
//            // Simply acknowledge to confirm listener is working
//
//        } catch (Exception e) {
//            log.error(Constant.ERROR_PROCESSING_HEALTH_CHECK, e.getMessage(), e);
//        }
//    }
//}
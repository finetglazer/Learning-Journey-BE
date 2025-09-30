//package com.graduation.sagaorchestratorservice.controller;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
//import com.graduation.sagaorchestratorservice.service.OrderPurchaseSagaService;
//import com.graduation.sagaorchestratorservice.service.SagaMonitoringService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * REST Controller for Saga Orchestrator Service
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/sagas")
//@RequiredArgsConstructor
//public class SagaController {
//
//    private final OrderPurchaseSagaService orderPurchaseSagaService;
//    private final SagaMonitoringService monitoringService;
//
//    /**
//     * Get saga by ID
//     */
//    @GetMapping("/{sagaId}")
//    public ResponseEntity<Map<String, Object>> getSaga(@PathVariable String sagaId) {
//        try {
//            log.debug(Constant.LOG_GETTING_SAGA, sagaId);
//            Optional<OrderPurchaseSagaState> saga = orderPurchaseSagaService.findById(sagaId);
//
//            if (saga.isPresent()) {
//                Map<String, Object> response = Map.of(
//                        Constant.RESPONSE_SUCCESS, true,
//                        Constant.RESPONSE_SAGA, createSagaResponse(saga.get())
//                );
//                return ResponseEntity.ok(response);
//            } else {
//                Map<String, Object> response = Map.of(
//                        Constant.RESPONSE_SUCCESS, false,
//                        Constant.RESPONSE_MESSAGE, Constant.SAGA_NOT_FOUND
//                );
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//            }
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_GETTING_SAGA, sagaId, e);
//            Map<String, Object> errorResponse = Map.of(
//                    Constant.RESPONSE_SUCCESS, false,
//                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_SAGA + e.getMessage()
//            );
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
//
//    /**
//     * Get saga by order ID
//     */
//    @GetMapping("/order/{orderId}")
//    public ResponseEntity<Map<String, Object>> getSagaByOrderId(@PathVariable Long orderId) {
//        try {
//            log.debug(Constant.LOG_GETTING_SAGA_BY_ORDER, orderId);
//            Optional<OrderPurchaseSagaState> saga = orderPurchaseSagaService.findByOrderId(orderId);
//
//            if (saga.isPresent()) {
//                Map<String, Object> response = Map.of(
//                        Constant.RESPONSE_SUCCESS, true,
//                        Constant.RESPONSE_SAGA, createSagaResponse(saga.get())
//                );
//                return ResponseEntity.ok(response);
//            } else {
//                Map<String, Object> response = Map.of(
//                        Constant.RESPONSE_SUCCESS, false,
//                        Constant.RESPONSE_MESSAGE, Constant.SAGA_NOT_FOUND_FOR_ORDER + orderId
//                );
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//            }
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_GETTING_SAGA_BY_ORDER, orderId, e);
//            Map<String, Object> errorResponse = Map.of(
//                    Constant.RESPONSE_SUCCESS, false,
//                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_SAGA + e.getMessage()
//            );
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
//
//    /**
//     * Get sagas by user ID
//     */
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<Map<String, Object>> getSagasByUserId(@PathVariable String userId) {
//        try {
//            log.debug(Constant.LOG_GETTING_SAGAS_BY_USER, userId);
//            List<OrderPurchaseSagaState> sagas = orderPurchaseSagaService.findByUserId(userId);
//
//            List<Map<String, Object>> sagaResponses = sagas.stream()
//                    .map(this::createSagaResponse)
//                    .toList();
//
//            Map<String, Object> response = Map.of(
//                    Constant.RESPONSE_SUCCESS, true,
//                    Constant.RESPONSE_SAGAS, sagaResponses,
//                    Constant.RESPONSE_TOTAL_COUNT, sagas.size()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_GETTING_SAGAS_BY_USER, userId, e);
//            Map<String, Object> errorResponse = Map.of(
//                    Constant.RESPONSE_SUCCESS, false,
//                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_USER_SAGAS + e.getMessage()
//            );
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
//
//    /**
//     * Get all active sagas
//     */
//    @GetMapping("/active")
//    public ResponseEntity<Map<String, Object>> getActiveSagas() {
//        try {
//            log.debug(Constant.LOG_GETTING_ACTIVE_SAGAS);
//            List<OrderPurchaseSagaState> activeSagas = orderPurchaseSagaService.findActiveSagas();
//
//            List<Map<String, Object>> sagaResponses = activeSagas.stream()
//                    .map(this::createSagaResponse)
//                    .toList();
//
//            Map<String, Object> response = Map.of(
//                    Constant.RESPONSE_SUCCESS, true,
//                    Constant.RESPONSE_ACTIVE_SAGAS, sagaResponses,
//                    Constant.RESPONSE_TOTAL_COUNT, activeSagas.size()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_GETTING_ACTIVE_SAGAS, e);
//            Map<String, Object> errorResponse = Map.of(
//                    Constant.RESPONSE_SUCCESS, false,
//                    Constant.RESPONSE_MESSAGE, Constant.ERROR_RETRIEVING_ACTIVE_SAGAS + e.getMessage()
//            );
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
//
//    /**
//     * Cancel saga by user request
//     */
////    @PostMapping("/{sagaId}/cancel")
////    public ResponseEntity<Map<String, Object>> cancelSaga(@PathVariable String sagaId) {
////        try {
////            log.info(Constant.LOG_CANCELLING_SAGA, sagaId);
////            OrderPurchaseSagaState cancelledSaga = orderPurchaseSagaService.cancelSagaByUser(sagaId);
////
////            Map<String, Object> response = Map.of(
////                    Constant.RESPONSE_SUCCESS, true,
////                    Constant.RESPONSE_MESSAGE, Constant.SAGA_CANCELLATION_INITIATED,
////                    Constant.RESPONSE_SAGA, createSagaResponse(cancelledSaga)
////            );
////
////            return ResponseEntity.ok(response);
////
////        } catch (Exception e) {
////            log.error(Constant.LOG_ERROR_CANCELLING_SAGA, sagaId, e);
////            Map<String, Object> errorResponse = Map.of(
////                    Constant.RESPONSE_SUCCESS, false,
////                    Constant.RESPONSE_MESSAGE, Constant.ERROR_CANCELLING_SAGA + e.getMessage()
////            );
////            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
////        }
////    }
//
//    /**
//     * Get saga monitoring health status
//     */
//    @GetMapping("/health")
//    public ResponseEntity<Map<String, Object>> getHealthStatus() {
//        try {
//            SagaMonitoringService.HealthStatus health = monitoringService.getHealthStatus();
//
//            Map<String, Object> response = Map.of(
//                    Constant.RESPONSE_SERVICE, Constant.SERVICE_NAME,
//                    Constant.RESPONSE_STATUS, health.healthy ? Constant.STATUS_UP : Constant.STATUS_DOWN,
//                    Constant.RESPONSE_ACTIVE_COUNT, health.activeCount,
//                    Constant.RESPONSE_TOTAL_PROCESSED, health.totalProcessed,
//                    Constant.RESPONSE_TOTAL_FAILURES, health.totalFailures,
//                    Constant.RESPONSE_FAILURE_RATE, String.format(Constant.FORMAT_FAILURE_RATE, health.failureRate * 100),
//                    Constant.RESPONSE_TIMESTAMP, java.time.LocalDateTime.now()
//            );
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_GETTING_HEALTH_STATUS, e);
//            Map<String, Object> errorResponse = Map.of(
//                    Constant.RESPONSE_SERVICE, Constant.SERVICE_NAME,
//                    Constant.RESPONSE_STATUS, Constant.STATUS_DOWN,
//                    Constant.RESPONSE_ERROR, e.getMessage(),
//                    Constant.RESPONSE_TIMESTAMP, java.time.LocalDateTime.now()
//            );
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
//        }
//    }
//
//    /**
//     * Test endpoint
//     */
//    @GetMapping("/test")
//    public ResponseEntity<Map<String, Object>> test() {
//        Map<String, Object> response = Map.of(
//                Constant.RESPONSE_SERVICE, Constant.SERVICE_NAME,
//                Constant.RESPONSE_STATUS, Constant.STATUS_RUNNING,
//                Constant.RESPONSE_MESSAGE, Constant.TEST_ENDPOINT_MESSAGE,
//                Constant.RESPONSE_TIMESTAMP, java.time.LocalDateTime.now()
//        );
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Create response object for saga
//     */
//    private Map<String, Object> createSagaResponse(OrderPurchaseSagaState saga) {
//        Map<String, Object> response = new HashMap<>();
//        response.put(Constant.RESPONSE_SAGA_ID, saga.getSagaId());
//        response.put(Constant.RESPONSE_ORDER_ID, saga.getOrderId());
//        response.put(Constant.RESPONSE_USER_ID, saga.getUserId());
//        response.put(Constant.RESPONSE_STATUS, saga.getStatus().name());
//        response.put(Constant.RESPONSE_CURRENT_STEP, saga.getCurrentStep() != null ? saga.getCurrentStep().name() : "null");
//        response.put(Constant.RESPONSE_TOTAL_AMOUNT, saga.getTotalAmount());
//        response.put(Constant.RESPONSE_START_TIME, saga.getStartTime().toString());
//        response.put(Constant.RESPONSE_LAST_UPDATED_TIME, saga.getLastUpdatedTime().toString());
//        response.put(Constant.RESPONSE_RETRY_COUNT, saga.getRetryCount());
//        response.put(Constant.RESPONSE_FAILURE_REASON, saga.getFailureReason());
//        response.put(Constant.RESPONSE_COMPLETED_STEPS, saga.getCompletedSteps());
//        response.put(Constant.RESPONSE_SAGA_EVENTS, saga.getSagaEvents());
//        return response;
//    }
//}
//package com.graduation.sagaorchestratorservice.listener;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import com.graduation.sagaorchestratorservice.handler.OrderEventHandler;
//import com.graduation.sagaorchestratorservice.handler.PaymentEventHandler;
//import com.graduation.sagaorchestratorservice.handler.SagaEventHandler;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
///**
// * Kafka listener for Saga Orchestrator Service
// * Handles events from Order and Payment services to coordinate saga flow
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class SagaOrchestratorKafkaListener {
//
//    private final OrderEventHandler orderEventHandler;
//    private final PaymentEventHandler paymentEventHandler;
//    private final SagaEventHandler sagaEventHandler;
//
//    /**
//     * Listen to order events from Order Service
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.order-events}",
//            containerFactory = "orderEventKafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}" + Constant.GROUP_SUFFIX_ORDER_EVENTS
//    )
//    public void consumeOrderEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
//        try {
//            String eventType = (String) event.get(Constant.FIELD_TYPE);
//            String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//            String messageId = (String) event.get(Constant.FIELD_MESSAGE_ID);
//
//            log.info(Constant.LOG_RECEIVED_ORDER_EVENT,
//                    eventType, sagaId, messageId);
//
//            // Delegate to handler
//            orderEventHandler.handleOrderEvent(event);
//
//            // Acknowledge the message
//            ack.acknowledge();
//            log.debug(Constant.LOG_EVENT_ACKNOWLEDGED, eventType, sagaId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_ORDER_EVENT, e.getMessage(), e);
//            // Don't acknowledge - will be retried or sent to DLQ
//            throw new RuntimeException(Constant.ERROR_ORDER_EVENT_PROCESSING_FAILED, e);
//        }
//    }
//
//    /**
//     * Listen to payment events from Payment Service
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.payment-events}",
//            containerFactory = "paymentEventKafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}" + Constant.GROUP_SUFFIX_PAYMENT_EVENTS
//    )
//    public void consumePaymentEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
//        try {
//            String eventType = (String) event.get(Constant.FIELD_TYPE);
//            String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//            String messageId = (String) event.get(Constant.FIELD_MESSAGE_ID);
//
//            log.info(Constant.LOG_RECEIVED_PAYMENT_EVENT,
//                    eventType, sagaId, messageId);
//
//            // Delegate to handler
//            paymentEventHandler.handlePaymentEvent(event);
//
//            // Acknowledge the message
//            ack.acknowledge();
//            log.debug(Constant.LOG_EVENT_ACKNOWLEDGED, eventType, sagaId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_EVENT, e.getMessage(), e);
//            // Don't acknowledge - will be retried or sent to DLQ
//            throw new RuntimeException(Constant.ERROR_PAYMENT_EVENT_PROCESSING_FAILED, e);
//        }
//    }
//
//    /**
//     * Listen to saga-specific events (for monitoring and coordination)
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.saga-events}",
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}" + Constant.GROUP_SUFFIX_SAGA_EVENTS
//    )
//    public void consumeSagaEvents(@Payload Map<String, Object> event, Acknowledgment ack) {
//        try {
//            String eventType = (String) event.get(Constant.FIELD_TYPE);
//            String sagaId = (String) event.get(Constant.FIELD_SAGA_ID);
//            String messageId = (String) event.get(Constant.FIELD_MESSAGE_ID);
//
//            log.info(Constant.LOG_RECEIVED_SAGA_EVENT,
//                    eventType, sagaId, messageId);
//
//            // Delegate to handler
//            sagaEventHandler.handleSagaEvent(event);
//
//            // Acknowledge the message
//            ack.acknowledge();
//            log.debug(Constant.LOG_EVENT_ACKNOWLEDGED, eventType, sagaId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_SAGA_EVENT, e.getMessage(), e);
//            // Don't acknowledge - will be retried or sent to DLQ
//            throw new RuntimeException(Constant.ERROR_SAGA_EVENT_PROCESSING_FAILED, e);
//        }
//    }
//
//    /**
//     * Listen to Dead Letter Queue messages for debugging and recovery
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.dlq}",
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}" + Constant.GROUP_SUFFIX_DLQ
//    )
//    public void consumeDlqMessages(@Payload Object messagePayload, Acknowledgment ack) {
//        try {
//            log.warn(Constant.LOG_RECEIVED_DLQ_MESSAGE, messagePayload);
//
//            // Delegate to handler
//            sagaEventHandler.handleDlqMessage(messagePayload);
//
//            // Acknowledge to prevent infinite loop in DLQ processing
//            ack.acknowledge();
//
//        } catch (Exception e) {
//            log.error(Constant.ERROR_PROCESSING_DLQ_MESSAGE, e.getMessage(), e);
//            // Still acknowledge to prevent infinite loop in DLQ processing
//            ack.acknowledge();
//        }
//    }
//
//    /**
//     * Health check listener (optional)
//     */
//    @KafkaListener(
//            topics = Constant.TOPIC_SAGA_HEALTH_CHECK,
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}" + Constant.GROUP_SUFFIX_HEALTH
//    )
//    public void consumeHealthCheckMessages(@Payload Map<String, Object> message, Acknowledgment ack) {
//        try {
//            log.debug(Constant.LOG_RECEIVED_HEALTH_CHECK, message.get(Constant.FIELD_TIMESTAMP));
//
//            // Delegate to handler
//            sagaEventHandler.handleHealthCheckMessage(message);
//
//            // Acknowledge to confirm listener is working
//            ack.acknowledge();
//
//        } catch (Exception e) {
//            log.error(Constant.ERROR_PROCESSING_HEALTH_CHECK, e.getMessage(), e);
//            ack.acknowledge(); // Still acknowledge to avoid blocking
//        }
//    }
//}
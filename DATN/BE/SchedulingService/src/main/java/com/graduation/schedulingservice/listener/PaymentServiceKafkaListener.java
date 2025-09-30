//package com.graduation.schedulingservice.listener;
//
//import com.graduation.schedulingservice.constant.Constant;
//import com.graduation.schedulingservice.service.PaymentCommandHandlerService;
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
// * Kafka listener for Payment Service commands
// * Handles payment-related commands from the Saga Orchestrator
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentServiceKafkaListener {
//
//    private final PaymentCommandHandlerService commandHandlerService;
//
//    /**
//     * Listen to payment commands from Saga Orchestrator
//     * Handles commands like: PAYMENT_PROCESS, PAYMENT_REVERSE
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.payment-commands}",
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}"
//    )
//    public void consumePaymentCommands(@Payload Map<String, Object> command, Acknowledgment ack) {
//        try {
//            String commandType = (String) command.get(Constant.FIELD_TYPE);
//            String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
//            String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
//
//            log.info(Constant.LOG_PROCESSING_PAYMENT_COMMAND, commandType, sagaId, messageId);
//
//            // Route to appropriate handler based on command type
//            switch (commandType) {
//                case Constant.COMMAND_PAYMENT_PROCESS:
//                    commandHandlerService.handleProcessPayment(command);
//                    break;
//                case Constant.COMMAND_PAYMENT_REVERSE:
//                    commandHandlerService.handleReversePayment(command);
//                    break;
//                default:
//                    log.warn(Constant.LOG_UNKNOWN_PAYMENT_COMMAND, commandType, sagaId);
//                    break;
//            }
//
//            // Acknowledge the message
//            ack.acknowledge();
//            log.debug(Constant.LOG_PAYMENT_COMMAND_ACKNOWLEDGED, commandType, sagaId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_PAYMENT_COMMAND, e.getMessage(), e);
//            // Don't acknowledge - will be retried or sent to DLQ
//            throw new RuntimeException(Constant.ERROR_PAYMENT_COMMAND_PROCESSING_FAILED, e);
//        }
//    }
//}
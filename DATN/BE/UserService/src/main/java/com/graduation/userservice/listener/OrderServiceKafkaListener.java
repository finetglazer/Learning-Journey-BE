//package com.graduation.userservice.listener;
//
//import com.graduation.userservice.constant.Constant;
//import com.graduation.userservice.service.OrderCommandHandlerService;
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
// * Kafka listener for Order Service commands
// * Handles order-related commands from the Saga Orchestrator
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OrderServiceKafkaListener {
//
//    // TODO: Inject OrderCommandHandlerService when implemented
//    // private final OrderCommandHandlerService commandHandlerService;
//
//    private final OrderCommandHandlerService orderCommandHandlerService;
//
//    /**
//     * Listen to order commands from Saga Orchestrator
//     * Handles commands like: ORDER_UPDATE_CONFIRMED, ORDER_UPDATE_DELIVERED, ORDER_CANCEL
//     */
//    @KafkaListener(
//            topics = "${kafka.topics.order-commands}",
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "${spring.kafka.consumer.group-id}"
//    )
//    public void consumeOrderCommands(@Payload Map<String, Object> command, Acknowledgment ack) {
//        try {
//            String commandType = (String) command.get(Constant.FIELD_TYPE);
//            String sagaId = (String) command.get(Constant.FIELD_SAGA_ID);
//            String messageId = (String) command.get(Constant.FIELD_MESSAGE_ID);
//
//            log.info(Constant.LOG_PROCESSING_ORDER_COMMAND,
//                    commandType, sagaId, messageId);
//
//            // Route to appropriate handler based on command type
//            switch (commandType) {
//                case Constant.COMMAND_ORDER_UPDATE_CONFIRMED:
//                    orderCommandHandlerService.handleUpdateOrderConfirmed(command);
//                    break;
//                case Constant.COMMAND_ORDER_UPDATE_DELIVERED:
//                    orderCommandHandlerService.handleUpdateOrderDelivered(command);
//                    break;
//                case Constant.COMMAND_ORDER_CANCEL:
//                    orderCommandHandlerService.handleUpdateOrderCancelled(command);
//                default:
//                    log.warn(Constant.LOG_UNKNOWN_ORDER_COMMAND, commandType, sagaId);
//                    break;
//            }
//
//            // Acknowledge the message
//            ack.acknowledge();
//            log.debug(Constant.LOG_ORDER_COMMAND_ACKNOWLEDGED, commandType, sagaId);
//
//        } catch (Exception e) {
//            log.error(Constant.LOG_ERROR_PROCESSING_ORDER_COMMAND, e.getMessage(), e);
//            // Don't acknowledge - will be retried or sent to DLQ
//            throw new RuntimeException(Constant.ERROR_ORDER_COMMAND_PROCESSING_FAILED, e);
//        }
//    }
//}
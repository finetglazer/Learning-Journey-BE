//package com.graduation.sagaorchestratorservice.config;
//
//import com.graduation.sagaorchestratorservice.constants.Constant;
//import org.apache.kafka.clients.admin.AdminClientConfig;
//import org.apache.kafka.clients.admin.NewTopic;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringDeserializer;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.annotation.EnableKafka;
//import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
//import org.springframework.kafka.core.*;
//import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
//import org.springframework.kafka.listener.DefaultErrorHandler;
//import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
//import org.springframework.kafka.support.serializer.JsonDeserializer;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//import org.springframework.util.backoff.ExponentialBackOff;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Kafka configuration for Saga Orchestrator Service
// */
//@EnableKafka
//@Configuration
//public class KafkaConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.consumer.group-id}")
//    private String groupId;
//
//    // Topic names from properties
//    @Value("${kafka.topics.order-commands}")
//    private String orderCommandsTopic;
//
//    @Value("${kafka.topics.order-events}")
//    private String orderEventsTopic;
//
//    @Value("${kafka.topics.payment-commands}")
//    private String paymentCommandsTopic;
//
//    @Value("${kafka.topics.payment-events}")
//    private String paymentEventsTopic;
//
//    @Value("${kafka.topics.saga-events}")
//    private String sagaEventsTopic;
//
//    @Value("${kafka.topics.dlq}")
//    private String dlqTopic;
//
//    // Kafka Admin Configuration
//    @Bean
//    public KafkaAdmin kafkaAdmin() {
//        Map<String, Object> configs = new HashMap<>();
//        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        return new KafkaAdmin(configs);
//    }
//
//    // Topic Configurations
//    @Bean
//    public NewTopic orderCommandsTopic() {
//        return new NewTopic(orderCommandsTopic, 3, (short) 1);
//    }
//
//    @Bean
//    public NewTopic orderEventsTopic() {
//        return new NewTopic(orderEventsTopic, 3, (short) 1);
//    }
//
//    @Bean
//    public NewTopic paymentCommandsTopic() {
//        return new NewTopic(paymentCommandsTopic, 3, (short) 1);
//    }
//
//    @Bean
//    public NewTopic paymentEventsTopic() {
//        return new NewTopic(paymentEventsTopic, 3, (short) 1);
//    }
//
//    @Bean
//    public NewTopic sagaEventsTopic() {
//        return new NewTopic(sagaEventsTopic, 3, (short) 1);
//    }
//
//    @Bean
//    public NewTopic dlqTopic() {
//        return new NewTopic(dlqTopic, 1, (short) 1);
//    }
//
//    // Producer Configuration
//    @Bean
//    public ProducerFactory<String, Object> producerFactory() {
//        Map<String, Object> configProps = new HashMap<>();
//        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//
//        // Producer reliability settings
//        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
//        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
//        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
//
//        // Batching for performance
//        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
//        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
//        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
//
//        // Add type information to headers
//        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
//
//        return new DefaultKafkaProducerFactory<>(configProps);
//    }
//
//    @Bean
//    public KafkaTemplate<String, Object> kafkaTemplate() {
//        return new KafkaTemplate<>(producerFactory());
//    }
//
//    // Consumer Configuration
//    @Bean
//    public ConsumerFactory<String, Object> consumerFactory() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
//
//        // Configure JSON deserializer with error handling
//        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
//
//        // Consumer settings
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
//        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
//        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
//        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
//        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
//
//        // Type info for deserialization
//        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
//        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);
//
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    // Listener Container Factory
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(consumerFactory());
//        factory.setConcurrency(3);
//        factory.getContainerProperties().setAckMode(
//                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//
//        // Configure error handling with dead letter topic
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new DeadLetterPublishingRecoverer(kafkaTemplate(),
//                        (record, exception) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
//                new ExponentialBackOff(1000, 2)
//        );
//
//        // Set maximum delivery attempts
//        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
//            if (deliveryAttempt > 3) {
//                // Log the failure after max retries
//                org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConfig.class);
//                log.error("Message processing failed after {} attempts, sending to DLQ: {}",
//                        deliveryAttempt, record.value(), ex);
//            }
//        });
//
//        factory.setCommonErrorHandler(errorHandler);
//
//        return factory;
//    }
//
//    // Order Events specific container factory
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> orderEventKafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(createOrderEventConsumerFactory());
//        factory.setConcurrency(2);
//        factory.getContainerProperties().setAckMode(
//                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//
//        // Custom error handling for order events
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new DeadLetterPublishingRecoverer(kafkaTemplate(),
//                        (record, exception) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
//                new ExponentialBackOff(2000, 2)
//        );
//
//        factory.setCommonErrorHandler(errorHandler);
//
//        return factory;
//    }
//
//    /**
//     * Payment Events specific container factory
//     */
//    @Bean
//    public ConcurrentKafkaListenerContainerFactory<String, Object> paymentEventKafkaListenerContainerFactory() {
//        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
//                new ConcurrentKafkaListenerContainerFactory<>();
//
//        factory.setConsumerFactory(createPaymentEventConsumerFactory());
//        factory.setConcurrency(2);
//        factory.getContainerProperties().setAckMode(
//                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//
//        // Custom error handling for payment events
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new DeadLetterPublishingRecoverer(kafkaTemplate(),
//                        (record, exception) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
//                new ExponentialBackOff(1500, 2)
//        );
//
//        factory.setCommonErrorHandler(errorHandler);
//
//        return factory;
//    }
//
//    // Helper methods for specific consumer factories
//    private ConsumerFactory<String, Object> createOrderEventConsumerFactory() {
//        return createEventConsumerFactory(groupId + Constant.GROUP_SUFFIX_ORDER_EVENTS);
//    }
//
//    private ConsumerFactory<String, Object> createPaymentEventConsumerFactory() {
//        return createEventConsumerFactory(groupId + Constant.GROUP_SUFFIX_PAYMENT_EVENTS);
//    }
//
//    private ConsumerFactory<String, Object> createEventConsumerFactory(String groupId) {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
//        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
//
//        // Set trusted packages
//        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
//        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");
//        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//
//        // Type info for deserialization
//        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
//        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, false);
//
//        return new DefaultKafkaConsumerFactory<>(props);
//    }
//
//    /**
//     * Generic error handler bean
//     */
//    @Bean
//    public DefaultErrorHandler kafkaErrorHandler() {
//        DefaultErrorHandler handler = new DefaultErrorHandler(
//                (record, exception) -> {
//                    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KafkaConfig.class);
//                    log.error(Constant.ERROR_PROCESSING_RECORD, exception.getMessage());
//                    exception.printStackTrace();
//                },
//                new ExponentialBackOff(1000, 2)
//        );
//
//        // Add non-retryable exceptions
//        handler.addNotRetryableExceptions(
//                org.apache.kafka.common.errors.SerializationException.class,
//                org.springframework.kafka.support.serializer.DeserializationException.class,
//                IllegalArgumentException.class
//        );
//
//        return handler;
//    }
//}
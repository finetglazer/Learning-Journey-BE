//package com.graduation.schedulingservice.config;
//
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
// * Kafka configuration for Payment Service
// */
//@EnableKafka
//@Configuration
//public class PaymentServiceKafkaConfig {
//
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServers;
//
//    @Value("${spring.kafka.consumer.group-id}")
//    private String groupId;
//
//    @Value("${kafka.topics.dlq}")
//    private String dlqTopic;
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
//
//        // Batching for performance
//        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
//        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
//        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
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
//        factory.setConcurrency(2);
//        factory.getContainerProperties().setAckMode(
//                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);
//
//        // Configure error handling with dead letter topic
//        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
//                new DeadLetterPublishingRecoverer(kafkaTemplate(),
//                        (record, exception) -> new org.apache.kafka.common.TopicPartition(dlqTopic, 0)),
//                new ExponentialBackOff(1500, 2)
//        );
//
//        // Set maximum delivery attempts
//        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
//            if (deliveryAttempt > 3) {
//                org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentServiceKafkaConfig.class);
//                log.error("Payment command processing failed after {} attempts, sending to DLQ: {}",
//                        deliveryAttempt, record.value(), ex);
//            }
//        });
//
//        factory.setCommonErrorHandler(errorHandler);
//
//        return factory;
//    }
//}
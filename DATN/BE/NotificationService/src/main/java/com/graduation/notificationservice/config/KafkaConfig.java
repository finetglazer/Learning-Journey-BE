package com.graduation.notificationservice.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for NotificationService (Consumer).
 * Configured for Aiven Kafka with SASL_SSL authentication.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${KAFKA_USER}")
    private String kafkaUser;

    @Value("${KAFKA_PASSWORD}")
    private String kafkaPassword;

    /**
     * Topic name for user updated events.
     * Following convention: domain.service.entity.event.version
     */
    public static final String TOPIC_USER_UPDATED = "pm.user-service.user.updated.v1";

    /**
     * Dead Letter Queue topic.
     */
    public static final String TOPIC_USER_UPDATED_DLQ = "pm.user-service.user.updated.v1.dlq";

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // SSL/mTLS Configuration for Aiven
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        // Client Certificate (mTLS)
        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        // SSL Trust Store - read PEM content and use ssl.truststore.certificates
        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        // Disable hostname verification (optional, uncomment if needed)
        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        // Configure JSON deserializer with error handling
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.graduation.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.lang.Object");

        // Consumer settings
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Type info for deserialization
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, true);
        props.put(JsonDeserializer.TYPE_MAPPINGS,
                "com.graduation.userservice.event.UserUpdatedEvent:com.graduation.notificationservice.event.UserUpdatedEvent");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }
}

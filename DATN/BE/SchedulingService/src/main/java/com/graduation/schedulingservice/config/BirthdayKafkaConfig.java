package com.graduation.schedulingservice.config;

import com.graduation.schedulingservice.event.BirthdayEvent;
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
 * Kafka configuration for SchedulingService (Consumer).
 * Consumes birthday events from UserService to create memorable events.
 */
@EnableKafka
@Configuration
public class BirthdayKafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:scheduling-service}")
    private String groupId;

    /**
     * Topic name for birthday updated events.
     * Published by UserService when a user updates their date of birth.
     */
    public static final String TOPIC_BIRTHDAY_UPDATED = "pm.user-service.birthday.updated.v1";

    @Bean
    public ConsumerFactory<String, BirthdayEvent> birthdayConsumerFactory() throws IOException {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);

        // SSL/mTLS Configuration for Aiven
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        // Consumer settings
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Specific Deserializer for BirthdayEvent
        JsonDeserializer<BirthdayEvent> deserializer = new JsonDeserializer<>(BirthdayEvent.class);
        deserializer.setUseTypeHeaders(false);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");

        ErrorHandlingDeserializer<BirthdayEvent> errorDeserializer = new ErrorHandlingDeserializer<>(deserializer);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BirthdayEvent> birthdayKafkaListenerContainerFactory()
            throws IOException {
        ConcurrentKafkaListenerContainerFactory<String, BirthdayEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(birthdayConsumerFactory());
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}

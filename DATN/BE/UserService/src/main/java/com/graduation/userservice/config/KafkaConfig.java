package com.graduation.userservice.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for UserService (Producer).
 * Configured for Aiven Kafka with SASL_SSL authentication.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${KAFKA_USER}")
    private String kafkaUser;

    @Value("${KAFKA_PASSWORD}")
    private String kafkaPassword;

    /**
     * Topic name for user updated events.
     * Following convention: domain.service.entity.event.version
     */
    public static final String TOPIC_USER_UPDATED = "pm.user-service.user.updated.v1";

    @Bean
    public ProducerFactory<String, Object> producerFactory() throws IOException {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // SSL/mTLS Configuration for Aiven
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        // Client Certificate (mTLS)
        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        // SSL Trust Store - read PEM content and use ssl.truststore.certificates
        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        // Disable hostname verification (optional, uncomment if needed)
        configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        // Producer reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Add type information to headers for consumer deserialization
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() throws IOException {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public KafkaAdmin kafkaAdmin() throws IOException {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // SSL/mTLS Configuration for Aiven
        configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        // Client Certificate (mTLS)
        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        configs.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        // SSL Trust Store - read PEM content and use ssl.truststore.certificates
        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        configs.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        // Disable hostname verification (optional, uncomment if needed)
        configs.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userUpdatedTopic() {
        return new NewTopic(TOPIC_USER_UPDATED, 1, (short) 1);
    }
}

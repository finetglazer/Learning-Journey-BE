package com.graduation.projectservice.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    public static final String TOPIC_PROJECT_INVITATION = "pm.project-service.invitation.v1";

    @Bean
    public ProducerFactory<String, Object> producerFactory() throws IOException {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // SSL/mTLS Configuration for Aiven
        configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        configProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configProps.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        configProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");

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
        configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");

        ClassPathResource keyResource = new ClassPathResource("service.key");
        String serviceKey = new String(keyResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, serviceKey);

        ClassPathResource certResource = new ClassPathResource("service.cert");
        String serviceCert = new String(certResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, serviceCert);

        configs.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");

        ClassPathResource resource = new ClassPathResource("ca.pem");
        String caCertificate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        configs.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        configs.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic projectInvitationTopic() {

        return TopicBuilder.name(TOPIC_PROJECT_INVITATION)
                .partitions(1)
                .replicas(1)
                .build();
    }
}

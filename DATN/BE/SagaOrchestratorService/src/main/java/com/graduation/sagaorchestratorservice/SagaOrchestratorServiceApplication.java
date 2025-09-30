package com.graduation.sagaorchestratorservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class SagaOrchestratorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SagaOrchestratorServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("========================================");
        log.info("🚀 Saga Orchestrator Service Started Successfully!");
        log.info("========================================");
        log.info("📊 Infrastructure Features Enabled:");
        log.info("  ✅ Kafka messaging with topics configuration");
        log.info("  ✅ PostgreSQL persistence with JPA");
        log.info("  ✅ Idempotency service for message deduplication");
        log.info("  ✅ Monitoring service with Micrometer metrics");
        log.info("  ✅ Timeout scheduler for saga management");
        log.info("  ✅ Dead Letter Queue for failed messages");
        log.info("  ✅ Automatic retry with exponential backoff");
        log.info("========================================");
        log.info("🌐 Service Endpoints:");
        log.info("  📍 Server Port: 8083");
        log.info("  📊 Health Check: http://localhost:8083/actuator/health");
        log.info("  📈 Metrics: http://localhost:8083/actuator/metrics");
        log.info("  📋 Info: http://localhost:8083/actuator/info");
        log.info("========================================");
        log.info("🔧 Infrastructure Ready for Saga Implementation!");
        log.info("  📝 Next Steps: Implement saga definitions and business logic");
        log.info("========================================");
    }
}
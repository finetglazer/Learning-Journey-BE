package com.graduation.documentservice.scheduler;

import com.graduation.documentservice.constant.DocumentConstant;
import com.graduation.documentservice.model.DocSnapshot;
import com.graduation.documentservice.repository.DocSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class SnapshotCleanupScheduler {

    private final DocSnapshotRepository docSnapshotRepository;

    @Value("${app.snapshot.retention-days:90}")
    private int retentionDays;

    @Value("${app.snapshot.min-keep:10}")
    private int minKeepSnapshots;

    /**
     * Run daily at 2:00 AM to clean up old snapshots
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldSnapshots() {
        log.info(DocumentConstant.LOG_CLEANUP_STARTED);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<DocSnapshot> oldSnapshots = docSnapshotRepository.findByCreatedAtBefore(cutoffDate);

        int deletedCount = 0;

        for (DocSnapshot snapshot : oldSnapshots) {
            // Check if we should keep minimum snapshots for this document
            long remainingCount = docSnapshotRepository.countByPgNodeId(snapshot.getPgNodeId());

            if (remainingCount > minKeepSnapshots) {
                docSnapshotRepository.delete(snapshot);
                deletedCount++;
            }
        }

        log.info(DocumentConstant.LOG_CLEANUP_COMPLETED, deletedCount);
    }
}

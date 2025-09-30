package com.graduation.sagaorchestratorservice.repository;

import com.graduation.sagaorchestratorservice.model.OrderPurchaseSagaState;
import com.graduation.sagaorchestratorservice.model.enums.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for OrderPurchaseSagaState entity
 */
@Repository
public interface OrderPurchaseSagaStateRepository extends JpaRepository<OrderPurchaseSagaState, String> {

    /**
     * Find saga by order ID
     */
    Optional<OrderPurchaseSagaState> findByOrderId(Long orderId);

    /**
     * Find sagas by user ID
     */
    List<OrderPurchaseSagaState> findByUserId(String userId);

    /**
     * Find sagas by user ID and status
     */
    List<OrderPurchaseSagaState> findByUserIdAndStatus(String userId, SagaStatus status);

    /**
     * Find sagas by status
     */
    List<OrderPurchaseSagaState> findByStatus(SagaStatus status);

    /**
     * Find sagas with multiple statuses
     */
    List<OrderPurchaseSagaState> findByStatusIn(List<SagaStatus> statuses);

    /**
     * Find active sagas (not in final state)
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.status IN ('STARTED', 'IN_PROGRESS', 'COMPENSATING')")
    List<OrderPurchaseSagaState> findActiveSagas();

    /**
     * Find sagas that started within a time range
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.startTime BETWEEN :startTime AND :endTime")
    List<OrderPurchaseSagaState> findSagasStartedBetween(@Param("startTime") Instant startTime,
                                                    @Param("endTime") Instant endTime);

    /**
     * Find sagas that haven't been updated recently (for timeout detection)
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.status IN :statuses AND s.lastUpdatedTime < :cutoffTime")
    List<OrderPurchaseSagaState> findStaleSagas(@Param("statuses") List<SagaStatus> statuses,
                                           @Param("cutoffTime") Instant cutoffTime);

    /**
     * Find sagas with current step timeout
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.status IN :statuses AND s.currentStepStartTime < :cutoffTime")
    List<OrderPurchaseSagaState> findSagasWithStepTimeout(@Param("statuses") List<SagaStatus> statuses,
                                                     @Param("cutoffTime") Instant cutoffTime);

    /**
     * Find failed sagas for analysis
     */
    List<OrderPurchaseSagaState> findByStatusOrderByLastUpdatedTimeDesc(SagaStatus status);

    /**
     * Count sagas by status
     */
    long countByStatus(SagaStatus status);

    /**
     * Count sagas by user ID
     */
    long countByUserId(String userId);

    /**
     * Find latest sagas for a user
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.userId = :userId ORDER BY s.startTime DESC")
    List<OrderPurchaseSagaState> findLatestSagasByUser(@Param("userId") String userId,
                                                  org.springframework.data.domain.Pageable pageable);

    /**
     * Find sagas by payment transaction ID
     */
    Optional<OrderPurchaseSagaState> findByPaymentTransactionId(Long paymentTransactionId);

    /**
     * Check if saga exists for order
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Find completed sagas within time range
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.status = 'COMPLETED' AND s.endTime BETWEEN :startTime AND :endTime")
    List<OrderPurchaseSagaState> findCompletedSagasBetween(@Param("startTime") Instant startTime,
                                                      @Param("endTime") Instant endTime);

    /**
     * Find sagas that need cleanup (old completed/failed sagas)
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.status IN ('COMPLETED', 'COMPENSATION_COMPLETED') AND s.endTime < :cutoffTime")
    List<OrderPurchaseSagaState> findSagasForCleanup(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Get saga statistics by status
     */
    @Query("SELECT s.status, COUNT(s) FROM OrderPurchaseSagaState s GROUP BY s.status")
    List<Object[]> getSagaStatisticsByStatus();

    /**
     * Get average saga execution time for completed sagas
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (end_time - start_time))) as avg_seconds
        FROM order_purchase_sagas 
        WHERE status = 'COMPLETED' AND end_time IS NOT NULL
        """, nativeQuery = true)
    Double getAverageExecutionTimeForCompletedSagas();

    /**
     * Find sagas with high retry count
     */
    @Query("SELECT s FROM OrderPurchaseSagaState s WHERE s.retryCount >= :threshold ORDER BY s.retryCount DESC")
    List<OrderPurchaseSagaState> findSagasWithHighRetryCount(@Param("threshold") int threshold);

    /**
     * Delete old saga records (for cleanup)
     */
    void deleteByEndTimeBefore(Instant cutoffTime);

    /**
     * Find sagas by order IDs (bulk lookup)
     */
    List<OrderPurchaseSagaState> findByOrderIdIn(List<Long> orderIds);
}
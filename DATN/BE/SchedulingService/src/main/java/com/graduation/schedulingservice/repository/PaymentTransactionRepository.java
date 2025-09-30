package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.PaymentTransaction;
import com.graduation.schedulingservice.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PaymentTransaction entity
 */
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    /**
     * Find payment transaction by order ID
     */
    Optional<PaymentTransaction> findByOrderId(String orderId);

    /**
     * Find payment transactions by user ID
     */
    List<PaymentTransaction> findByUserId(String userId);

    /**
     * Find payment transaction by saga ID
     */
    Optional<PaymentTransaction> findByOrderIdAndSagaId(String orderId, String sagaId);

    /**
     * Find payment transaction by transaction reference
     */
    Optional<PaymentTransaction> findByTransactionReference(String transactionReference);

    /**
     * Find payment transaction by external transaction ID
     */
    Optional<PaymentTransaction> findByExternalTransactionId(String externalTransactionId);

    /**
     * Find payment transactions by status
     */
    List<PaymentTransaction> findByStatus(PaymentStatus status);

    /**
     * Find payment transactions by user ID and status
     */
    List<PaymentTransaction> findByUserIdAndStatus(String userId, PaymentStatus status);

    /**
     * Find payment transactions processed within a date range
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.processedAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findTransactionsProcessedBetween(@Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    /**
     * Find payment transactions by amount range
     */
    List<PaymentTransaction> findByAmountBetween(BigDecimal minAmount, BigDecimal maxAmount);

    /**
     * Find payment transactions with amount greater than specified value
     */
    List<PaymentTransaction> findByAmountGreaterThan(BigDecimal amount);

    /**
     * Find pending payment transactions
     */
    List<PaymentTransaction> findByStatusOrderByProcessedAtAsc(PaymentStatus status);

    /**
     * Find failed transactions that can be retried
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'FAILED' AND pt.retryCount < 3")
    List<PaymentTransaction> findRetryableTransactions();

    /**
     * Find transactions that need attention (pending for too long)
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'PENDING' AND pt.processedAt < :cutoffTime")
    List<PaymentTransaction> findTransactionsNeedingAttention(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count transactions by status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Count transactions by user ID
     */
    long countByUserId(String userId);

    /**
     * Count successful transactions by user ID
     */
    long countByUserIdAndStatus(String userId, PaymentStatus status);

    /**
     * Get total amount processed by user ID
     */
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.userId = :userId AND pt.status = 'CONFIRMED'")
    BigDecimal getTotalAmountByUser(@Param("userId") String userId);

    /**
     * Get total amount processed by status
     */
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = :status")
    BigDecimal getTotalAmountByStatus(@Param("status") PaymentStatus status);

    /**
     * Find transactions by payment method
     */
    List<PaymentTransaction> findByPaymentMethod(String paymentMethod);

    /**
     * Find transactions by multiple statuses
     */
    List<PaymentTransaction> findByStatusIn(List<PaymentStatus> statuses);

    /**
     * Get payment statistics by status
     */
    @Query("SELECT pt.status, COUNT(pt), SUM(pt.amount), AVG(pt.amount) FROM PaymentTransaction pt GROUP BY pt.status")
    List<Object[]> getPaymentStatisticsByStatus();

    /**
     * Get payment statistics by payment method
     */
    @Query("SELECT pt.paymentMethod, COUNT(pt), SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = 'CONFIRMED' GROUP BY pt.paymentMethod")
    List<Object[]> getPaymentStatisticsByMethod();

    /**
     * Find latest transactions for a user
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.userId = :userId ORDER BY pt.processedAt DESC")
    List<PaymentTransaction> findLatestTransactionsByUser(@Param("userId") String userId,
                                                          org.springframework.data.domain.Pageable pageable);

    /**
     * Find transactions requiring retry
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'FAILED' AND pt.retryCount < :maxRetries AND pt.lastRetryAt IS NULL OR pt.lastRetryAt < :retryAfter")
    List<PaymentTransaction> findTransactionsForRetry(@Param("maxRetries") int maxRetries,
                                                      @Param("retryAfter") LocalDateTime retryAfter);

    /**
     * Check if user has any successful payments
     */
    boolean existsByUserIdAndStatus(String userId, PaymentStatus status);

    /**
     * Find transactions by saga ID list
     */
    List<PaymentTransaction> findBySagaIdIn(List<String> sagaIds);

    /**
     * Get average processing time for successful payments
     */
    @Query(value = """
        SELECT AVG(EXTRACT(EPOCH FROM (processed_at - processed_at))) as avg_seconds
        FROM payment_transactions 
        WHERE status = 'CONFIRMED'
        """, nativeQuery = true)
    Double getAverageProcessingTime();

    /**
     * Find high-value transactions (for monitoring)
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.amount >= :threshold ORDER BY pt.amount DESC")
    List<PaymentTransaction> findHighValueTransactions(@Param("threshold") BigDecimal threshold);

    /**
     * Find suspicious transactions (multiple failures for same user)
     */
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.userId IN (SELECT pt2.userId FROM PaymentTransaction pt2 WHERE pt2.status IN ('FAILED', 'DECLINED') GROUP BY pt2.userId HAVING COUNT(pt2) >= :failureThreshold)")
    List<PaymentTransaction> findSuspiciousTransactions(@Param("failureThreshold") int failureThreshold);

    /**
     * Delete old transactions (for cleanup)
     */
    void deleteByProcessedAtBefore(LocalDateTime cutoffDate);

    /**
     * Find transactions by order IDs
     */
    List<PaymentTransaction> findByOrderIdIn(List<String> orderIds);

    /**
     * Check if order has payment transaction
     */
    boolean existsByOrderId(String orderId);
}
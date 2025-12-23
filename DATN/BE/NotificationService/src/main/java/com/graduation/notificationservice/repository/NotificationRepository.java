package com.graduation.notificationservice.repository;

import com.graduation.notificationservice.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Notification entity.
 * Provides query methods for filtering notifications by recipient and read
 * status.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find unread notifications for a specific recipient, ordered by creation date
     * (newest first).
     *
     * @param recipientId ID of the notification recipient
     * @param pageable    Pagination and sorting parameters
     * @return Page of unread notifications
     */
    Page<Notification> findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * Find all notifications for a specific recipient, ordered by creation date
     * (newest first).
     *
     * @param recipientId ID of the notification recipient
     * @param pageable    Pagination and sorting parameters
     * @return Page of all notifications
     */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /**
     * Bulk update: Mark all unread notifications as read for a specific user.
     * Uses a single UPDATE query for better performance.
     *
     * @param recipientId ID of the user
     * @return Number of notifications updated
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    int markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);

    /**
     * Bulk delete: Remove all read notifications for a specific user.
     * Uses a single DELETE query for better performance.
     *
     * @param recipientId ID of the user
     * @return Number of notifications deleted
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.recipientId = :recipientId AND n.isRead = true")
    int deleteReadByRecipientId(@Param("recipientId") Long recipientId);

    Notification getNotificationByToken(String token);
}

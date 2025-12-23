package com.graduation.notificationservice.service.impl;

import com.graduation.notificationservice.client.UserServiceClient;
import com.graduation.notificationservice.constant.Constant;
import com.graduation.notificationservice.exception.ForbiddenException;
import com.graduation.notificationservice.exception.NotFoundException;
import com.graduation.notificationservice.model.Notification;
import com.graduation.notificationservice.model.UserInfoCache;
import com.graduation.notificationservice.model.enums.NotificationType;
import com.graduation.notificationservice.payload.response.*;
import com.graduation.notificationservice.repository.NotificationRepository;
import com.graduation.notificationservice.repository.UserInfoCacheRepository;
import com.graduation.notificationservice.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService.
 * Handles notification retrieval with filtering, pagination, and sender info
 * enrichment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserInfoCacheRepository userInfoCacheRepository;
    private final UserServiceClient userServiceClient;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public BaseResponse<?> getNotifications(Long userId, String filter, int page, int limit) {
        log.info("Fetching notifications for userId={}, filter={}, page={}, limit={}", userId, filter, page, limit);

        // Validate filter parameter
        if (!Constant.FILTER_UNREAD.equals(filter) && !Constant.FILTER_ALL.equals(filter)) {
            log.warn("Invalid filter parameter: {}", filter);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    Constant.INVALID_FILTER_PARAM,
                    null);
        }

        // Create Pageable with limit+1 to detect if more pages exist
        // Spring uses 0-based page indexing, so subtract 1 from user's page number
        Pageable pageable = PageRequest.of(page - 1, limit + 1);

        // Query notifications based on filter
        Page<Notification> notificationPage;
        if (Constant.FILTER_UNREAD.equals(filter)) {
            notificationPage = notificationRepository.findByRecipientIdAndIsReadFalseOrderByCreatedAtDesc(userId,
                    pageable);
        } else {
            notificationPage = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<Notification> notifications = notificationPage.getContent();
        log.debug("Retrieved {} notifications from database", notifications.size());

        // Calculate hasMore using limit+1 strategy
        boolean hasMore = notifications.size() > limit;
        if (hasMore) {
            // Remove the extra (limit+1)th item
            notifications = notifications.subList(0, limit);
        }

        // Batch fetch sender information from cache
        Set<Long> senderIds = notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, UserInfoCache> senderMap = new HashMap<>();
        if (!senderIds.isEmpty()) {
            List<UserInfoCache> cachedUsers = userInfoCacheRepository.findAllById(senderIds);
            senderMap = cachedUsers.stream()
                    .collect(Collectors.toMap(UserInfoCache::getUserId, user -> user));
            log.debug("Loaded {} sender info records from cache", senderMap.size());
        }

        // Convert entities to DTOs
        List<NotificationDTO> notificationDTOs = new ArrayList<>();
        for (Notification notification : notifications) {
            notificationDTOs.add(toNotificationDTO(notification, senderMap));
        }

        // Build pagination metadata
        PaginationDTO pagination = new PaginationDTO(page, hasMore);

        // Build response
        NotificationListResponse data = new NotificationListResponse(notificationDTOs, pagination);

        log.info("Successfully retrieved {} notifications for userId={}", notificationDTOs.size(), userId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.NOTIFICATIONS_RETRIEVED,
                data);
    }

    /**
     * Convert Notification entity to NotificationDTO.
     *
     * @param notification Notification entity
     * @param senderMap    Map of sender IDs to cached user info
     * @return NotificationDTO
     */
    private NotificationDTO toNotificationDTO(Notification notification, Map<Long, UserInfoCache> senderMap) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getNotificationId());

        // Map sender information
        SenderDTO sender = mapSender(notification.getSenderId(), senderMap);
        dto.setSender(sender);

        dto.setContentMessage(notification.getContentMessage());
        dto.setType(notification.getType().name());
        dto.setTargetUrl(notification.getTargetUrl());

        // Only include invitationStatus for ACTION_INVITATION type
        if (notification.getType() == NotificationType.ACTION_INVITATION) {
            dto.setInvitationStatus(notification.getInvitationStatus().name());
        } else {
            dto.setInvitationStatus(null);
        }

        dto.setIsRead(notification.getIsRead());
        dto.setToken(notification.getToken());
        dto.setReferenceId(notification.getReferenceId());

        // Format createdAt to ISO 8601 string
        dto.setCreatedAt(notification.getCreatedAt().format(ISO_FORMATTER));

        return dto;
    }

    /**
     * Map sender ID to SenderDTO using cached user info.
     * If sender not found in cache, call UserService to fetch and cache user info.
     *
     * @param senderId  Sender's user ID
     * @param senderMap Map of cached user info
     * @return SenderDTO
     */
    private SenderDTO mapSender(Long senderId, Map<Long, UserInfoCache> senderMap) {
        if (senderId == null) {
            return new SenderDTO(null, "System", null);
        }

        UserInfoCache cachedUser = senderMap.get(senderId);
        if (cachedUser != null) {
            return new SenderDTO(
                    cachedUser.getUserId(),
                    cachedUser.getDisplayName(),
                    cachedUser.getAvatarUrl());
        } else {
            // Sender not found in cache - call UserService to fetch and cache user info
            log.info("Sender userId={} not found in cache, fetching from UserService", senderId);

            // Use UserServiceClient to fetch user info
            Optional<UserBatchDTO> userOptional = userServiceClient.findById(senderId);

            if (userOptional.isPresent()) {
                UserBatchDTO userBatchDTO = userOptional.get();
                log.info("Successfully fetched user info for userId={} from UserService", senderId);

                // Cache the user info
                UserInfoCache newCache = new UserInfoCache();
                newCache.setUserId(userBatchDTO.getUserId());
                newCache.setDisplayName(userBatchDTO.getName());
                newCache.setAvatarUrl(userBatchDTO.getAvatarUrl());
                userInfoCacheRepository.save(newCache);
                log.info("Cached user info for userId={}", senderId);

                // Update the senderMap to avoid future lookups in the same request
                senderMap.put(senderId, newCache);

                return new SenderDTO(
                        userBatchDTO.getUserId(),
                        userBatchDTO.getName(),
                        userBatchDTO.getAvatarUrl());
            } else {
                log.warn("User not found in UserService for userId={}", senderId);
                return new SenderDTO(senderId, "Unknown User", null);
            }
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateReadStatus(Long userId, Long notificationId, Boolean isRead) {
        log.info("Updating read status for notificationId={}, userId={}, isRead={}", notificationId, userId, isRead);

        // Find notification
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(Constant.NOTIFICATION_NOT_FOUND));

        // Verify ownership
        if (!notification.getRecipientId().equals(userId)) {
            log.warn("User {} attempted to modify notification {} owned by user {}",
                    userId, notificationId, notification.getRecipientId());
            throw new ForbiddenException(Constant.NOTIFICATION_ACCESS_DENIED);
        }

        // Verify status
        if (notification.getIsRead().equals(isRead)) {
            log.info("Notification {} already has isRead={} status, no update needed", notificationId, isRead);
            // create msg as the above log
            String msg = String.format("Notification %d already has isRead=%b status, no update needed", notificationId,
                    isRead);

            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    msg, null);
        }

        // Update status
        notification.setIsRead(isRead);
        notificationRepository.save(notification);

        log.info("Successfully updated read status for notificationId={}", notificationId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.NOTIFICATION_STATUS_UPDATED,
                null);
    }

    @Override
    @Transactional
    public BaseResponse<?> markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for userId={}", userId);

        // Use optimized bulk update query
        int updatedCount = notificationRepository.markAllAsReadByRecipientId(userId);

        log.info("Successfully marked {} notifications as read for userId={}", updatedCount, userId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.ALL_NOTIFICATIONS_MARKED_READ,
                null);
    }

    @Override
    @Transactional
    public BaseResponse<?> clearReadNotifications(Long userId) {
        log.info("Clearing read notifications for userId={}", userId);

        // Use optimized bulk delete query
        int deletedCount = notificationRepository.deleteReadByRecipientId(userId);

        log.info("Successfully deleted {} read notifications for userId={}", deletedCount, userId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.READ_NOTIFICATIONS_CLEARED,
                null);
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteNotification(Long userId, Long notificationId) {
        log.info("Deleting notification {} for userId={}", notificationId, userId);

        // Find notification
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException(Constant.NOTIFICATION_NOT_FOUND));

        // Verify ownership
        if (!notification.getRecipientId().equals(userId)) {
            log.warn("User {} attempted to delete notification {} owned by user {}",
                    userId, notificationId, notification.getRecipientId());
            throw new ForbiddenException(Constant.NOTIFICATION_ACCESS_DENIED);
        }

        // Delete notification
        notificationRepository.delete(notification);

        log.info("Successfully deleted notification {} for userId={}", notificationId, userId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.NOTIFICATION_DELETED,
                null);
    }
}

package com.graduation.notificationservice.constant;

public class Constant {

    // ============================================
    // Response Status Codes
    // ============================================
    public static final int SUCCESS_STATUS = 1;
    public static final int ERROR_STATUS = 0;

    // ============================================
    // General Response Messages
    // ============================================
    public static final String SERVICE_RUNNING = "Notification Service is running!";
    public static final String SERVICE_INFO_RETRIEVED = "Service information retrieved successfully";

    // ============================================
    // Error Messages
    // ============================================
    public static final String ERROR_UNAUTHORIZED_ACCESS = "You don't have permission to access this resource";
    public static final String ERROR_INVALID_REQUEST = "Invalid request";
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";

    // ============================================
    // Log Messages
    // ============================================
    public static final String LOG_TEST_HEALTH_REQUEST = "GET /api/notification/test/health - Health check requested";
    public static final String LOG_TEST_INFO_REQUEST = "GET /api/notification/test/info - Service info requested";

    // ============================================
    // Notification Messages
    // ============================================
    public static final String NOTIFICATIONS_RETRIEVED = "Notifications retrieved";
    public static final String INVALID_FILTER_PARAM = "Filter must be UNREAD or ALL";

    // ============================================
    // Filter Types
    // ============================================
    public static final String FILTER_UNREAD = "UNREAD";
    public static final String FILTER_ALL = "ALL";

    // ============================================
    // Notification Success Messages
    // ============================================
    public static final String NOTIFICATION_STATUS_UPDATED = "Status updated";
    public static final String ALL_NOTIFICATIONS_MARKED_READ = "All notifications marked as read";
    public static final String READ_NOTIFICATIONS_CLEARED = "Read notifications cleared";
    public static final String NOTIFICATION_DELETED = "Notification deleted";

    // ============================================
    // Notification Error Messages
    // ============================================
    public static final String NOTIFICATION_NOT_FOUND = "Notification not found";
    public static final String NOTIFICATION_ACCESS_DENIED = "You don't have permission to access this notification";
    public static final String NOTIFICATION_STATUS_UNCHANGED = "Notification status is already as requested";
}

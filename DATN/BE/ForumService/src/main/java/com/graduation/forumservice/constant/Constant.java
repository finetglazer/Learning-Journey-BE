package com.graduation.forumservice.constant;

public class Constant {

    // ============================================
    // Response Status Codes
    // ============================================
    public static final int SUCCESS_STATUS = 1;
    public static final int ERROR_STATUS = 0;

    // ============================================
    // General Response Messages
    // ============================================
    public static final String SERVICE_RUNNING = "Forum Service is running!";
    public static final String SERVICE_INFO_RETRIEVED = "Service information retrieved successfully";

    // ============================================
    // Error Messages
    // ============================================
    public static final String ERROR_UNAUTHORIZED_ACCESS = "You don't have permission to access this resource";
    public static final String ERROR_INVALID_REQUEST = "Invalid request";
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String INVALID_PARAM = "Invalid parameter";
    public static final String FEED_RETRIEVED = "Post feed retrieved successfully";

    // ============================================
    // Log Messages
    // ============================================
    public static final String LOG_TEST_HEALTH_REQUEST = "GET /api/forum/test/health - Health check requested";
    public static final String LOG_TEST_INFO_REQUEST = "GET /api/forum/test/info - Service info requested";
}

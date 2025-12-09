package com.graduation.userservice.constant;

public class Constant {

    // ===================== API & Security Constants =====================
    public static final String API_AUTH_BASE_PATH = "/api/users/auth";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String PREFIX_BEARER = "Bearer ";

    // ===================== Table & Column Names =====================
    // Shared
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_SAGA_ID = "saga_id";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_MESSAGE_ID = "message_id";

    // User Service Tables
    public static final String TABLE_USERS = "users";
    public static final String TABLE_EMAIL_VERIFICATIONS = "email_verifications";
    public static final String TABLE_PASSWORD_RESET_TOKENS = "password_reset_tokens";
    public static final String TABLE_USER_SESSIONS = "user_sessions";
    public static final String TABLE_OAUTH_PROVIDERS = "oauth_providers";
    public static final String TABLE_PROCESSED_MESSAGES = "processed_messages";

    // Column Names for ProcessedMessage
    public static final String COLUMN_PROCESSED_AT = "processed_at";
    // ===================== Validation Messages =====================
    // Auth validation messages
    public static final String VALIDATION_EMAIL_REQUIRED = "Email is required";
    public static final String VALIDATION_EMAIL_FORMAT = "Invalid email format";

    // ===================== General & Auth Response Messages =====================
    public static final String MSG_VALIDATION_FAILED = "Validation failed";
    public static final String MSG_TOKEN_REQUIRED = "Token is required";
    public static final String MSG_REGISTRATION_SUCCESS = "Registration successful! Please check your email to verify your account.";
    public static final String MSG_EMAIL_ALREADY_EXISTS = "Email already registered";
    public static final String MSG_DISPLAY_NAME_EXISTS = "Display name already registered";
    public static final String MSG_REGISTRATION_FAILED = "Registration failed for email: ";
    public static final String MSG_VERIFICATION_SUCCESS = "User account activated successfully";
    public static final String MSG_VERIFICATION_INVALID = "Invalid verification code";
    public static final String MSG_VERIFICATION_NOT_FOUND = "Verification code not found";
    public static final String MSG_VERIFICATION_EXPIRED = "Verification code has expired";
    public static final String MSG_VERIFICATION_EXPIRED_REGISTER_AGAIN = "Verification code has expired. Please register again.";
    public static final String MSG_VERIFICATION_FAILED = "Email verification failed for code: ";
    public static final String MSG_USER_NOT_FOUND = "User not found";
    public static final String MSG_LOGIN_SUCCESS = "Login successful";
    public static final String MSG_LOGIN_FAILED = "Login failed for email: ";
    public static final String MSG_INVALID_CREDENTIALS = "Invalid credentials";
    public static final String MSG_ACCOUNT_NOT_VERIFIED = "Please verify your email address";
    public static final String MSG_ACCOUNT_NOT_VERIFIED_LOGIN = "Account not verified. Please check your email.";
    public static final String MSG_ACCOUNT_SUSPENDED = "Account has been suspended";
    public static final String MSG_LOGOUT_SUCCESS = "Logout successful";
    public static final String MSG_LOGOUT_SERVER_FAIL = "Logout failed on the server, but you should clear client-side data.";
    public static final String MSG_PASSWORD_RESET_SENT = "Password reset link has been sent to your email";
    public static final String MSG_PASSWORD_RESET_SUCCESS = "Password has been reset successfully";
    public static final String MSG_PASSWORD_RESET_INVALID = "Invalid or expired reset token";
    public static final String MSG_PASSWORD_CHANGE_SUCCESS = "Password changed successfully";
    public static final String MSG_CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
    public static final String MSG_PASSWORD_MISMATCH = "New password and confirmation password do not match.";
    public static final String MSG_EMAIL_NOT_FOUND = "Email address not found";
    public static final String MSG_NO_PASSWORD_SET = "This account does not have a password and cannot be reset. Please log in using your social provider.";

    // ===================== IDEMPOTENCY SERVICE MESSAGES =====================
    public static final String ERROR_MESSAGE_ID_REQUIRED = "messageId is required for idempotency check";
    public static final String ERROR_MESSAGE_ID_REQUIRED_RECORD = "messageId is required";
    public static final String ERROR_SAGA_ID_REQUIRED = "sagaId is required";
    public static final String ERROR_FAILED_TO_RECORD = "Failed to record processing";

    // ===================== AUTH SERVICE LOG MESSAGES =====================
    public static final String LOG_AUTH_REGISTRATION_SUCCESS = "Registration successful for {}. Publishing verification event.";
    public static final String LOG_AUTH_REGISTRATION_FAILED = "Registration failed for email: {}";
    public static final String LOG_AUTH_VERIFICATION_FAILED = "Email verification failed for code: {}";
    public static final String LOG_AUTH_WELCOME_EMAIL_WARN = "Failed to send welcome email to {} after verification.";
    public static final String LOG_AUTH_ACCOUNT_ACTIVATED = "User account activated successfully: {}";
    public static final String LOG_AUTH_LOGIN_SUCCESS = "User logged in successfully: {}";
    public static final String LOG_AUTH_LOGIN_FAILED = "Login failed for email: {}";
    public static final String LOG_AUTH_SESSION_INVALIDATED = "User session invalidated successfully for token ending with: ...{}";
    public static final String LOG_AUTH_LOGOUT_NO_SESSION = "Attempted to log out with a token that has no active session: ...{}";
    public static final String LOG_AUTH_LOGOUT_ERROR = "An error occurred during logout";
    public static final String LOG_AUTH_PASSWORD_CHANGED = "Password changed successfully for user: {}";
    public static final String LOG_AUTH_PASSWORD_CHANGE_FAILED = "Password change failed for user: {}";

    // ===================== ORDER SERVICE LOG MESSAGES =====================
    public static final String LOG_MESSAGE_ALREADY_PROCESSED = "Message already processed: messageId={}, sagaId={}";
    public static final String LOG_MESSAGE_NOT_PROCESSED = "Message not processed: messageId={}, sagaId={}";
    public static final String LOG_RECORDED_PROCESSING = "Recorded processing for messageId: {}, sagaId: {}, status: {}";
    public static final String LOG_FAILED_TO_RECORD_PROCESSING = "Failed to record processing for messageId: {}, sagaId: {}";
    public static final String LOG_CLEANING_OLD_MESSAGES = "Cleaning up old processed messages";
    public static final String LOG_DELETED_OLD_MESSAGES = "Deleted {} old processed messages";
    public static final String LOG_OLD_MESSAGES_CLEANUP_SUCCESS = "Old processed messages cleanup completed successfully";
    public static final String LOG_NO_OLD_MESSAGES = "No old processed messages to clean up";

    // ===================== User Settings Messages =====================
    public static final String MSG_GET_SLEEP_HOURS_SUCCESS = "Sleep hours retrieved successfully";
    public static final String MSG_GET_SLEEP_HOURS_FAILED = "Failed to retrieve sleep hours";
    public static final String MSG_UPDATE_SLEEP_HOURS_SUCCESS = "Sleep hours updated successfully";
    public static final String MSG_UPDATE_SLEEP_HOURS_FAILED = "Failed to update sleep hours";
    public static final String MSG_INVALID_TIME_FORMAT = "Invalid time format. Use HH:mm format (e.g., 22:00)";
    public static final String MSG_INVALID_TIME_RANGE_SAME = "Start time and end time cannot be the same";
    public static final String MSG_GET_DAILY_LIMITS_SUCCESS = "Daily limits retrieved successfully";
    public static final String MSG_GET_DAILY_LIMITS_FAILED = "Failed to retrieve daily limits";
    public static final String MSG_UPDATE_DAILY_LIMITS_SUCCESS = "Daily limits updated successfully";
    public static final String MSG_UPDATE_DAILY_LIMITS_FAILED = "Failed to update daily limits";
    public static final String MSG_INVALID_ITEM_TYPE = "Invalid item type: ";
    public static final String MSG_INVALID_HOURS_RANGE = "Hours must be between 0 and 24";

    // ===================== User Profile Messages =====================
    public static final String MSG_GET_PROFILE_SUCCESS = "Profile retrieved successfully";
    public static final String MSG_GET_PROFILE_FAILED = "Failed to retrieve profile";
    public static final String MSG_UPDATE_PROFILE_SUCCESS = "Profile updated successfully";
    public static final String MSG_UPDATE_PROFILE_FAILED = "Failed to update profile";
    public static final String MSG_NAME_REQUIRED = "Name is required";
    public static final String MSG_NAME_TOO_LONG = "Name must not exceed 100 characters";
    public static final String MSG_INVALID_DATE_FORMAT = "Invalid date format. Use dd/MM/yyyy format (e.g., 15/08/1998)";
    public static final String MSG_FILE_REQUIRED = "Avatar file is required";
    public static final String MSG_FILE_TOO_LARGE = "Avatar file size must not exceed 5MB";
    public static final String MSG_INVALID_FILE_TYPE = "Avatar must be jpg, jpeg, or png format";
    public static final String MSG_INVALID_IMAGE_DATA = "Invalid image data";
    public static final String MSG_AVATAR_UPLOAD_FAILED = "Failed to upload avatar image";

    // ===================== User Settings Log Messages =====================
    public static final String LOG_USER_NOT_FOUND = "User not found: {}";
    public static final String LOG_GET_SLEEP_HOURS_SUCCESS = "Sleep hours retrieved for user: {}";
    public static final String LOG_GET_SLEEP_HOURS_FAILED = "Failed to get sleep hours for user: {}";
    public static final String LOG_UPDATE_SLEEP_HOURS_SUCCESS = "Sleep hours updated for user: {} with {} ranges";
    public static final String LOG_UPDATE_SLEEP_HOURS_FAILED = "Failed to update sleep hours for user: {}";
    public static final String LOG_INVALID_TIME_FORMAT = "Invalid time format: start={}, end={}";
    public static final String LOG_GET_DAILY_LIMITS_SUCCESS = "Daily limits retrieved for user: {}";
    public static final String LOG_GET_DAILY_LIMITS_FAILED = "Failed to get daily limits for user: {}";
    public static final String LOG_UPDATE_DAILY_LIMITS_SUCCESS = "Daily limits updated for user: {} with {} item types";
    public static final String LOG_UPDATE_DAILY_LIMITS_FAILED = "Failed to update daily limits for user: {}";
    public static final String LOG_INVALID_ITEM_TYPE = "Invalid item type: {}";
    public static final String LOG_INVALID_HOURS_VALUE = "Invalid hours value for {}: {}";

    // ===================== User Profile Log Messages =====================
    public static final String LOG_GET_PROFILE_SUCCESS = "Profile retrieved for user: {}";
    public static final String LOG_GET_PROFILE_FAILED = "Failed to get profile for user: {}";
    public static final String LOG_UPDATE_PROFILE_SUCCESS = "Profile updated for user: {}";
    public static final String LOG_UPDATE_PROFILE_FAILED = "Failed to update profile for user: {}";
    public static final String LOG_INVALID_NAME = "Invalid name provided for user: {}";
    public static final String LOG_NAME_TOO_LONG = "Name too long for user: {}";
    public static final String LOG_INVALID_DATE_FORMAT = "Invalid date format: {}";
    public static final String LOG_AVATAR_VALIDATION_FAILED = "Avatar validation failed for user {}: {}";
    public static final String LOG_AVATAR_UPLOAD_FAILED = "Avatar upload failed for user: {}";
    public static final String LOG_AVATAR_UPLOADED_SUCCESS = "Avatar uploaded successfully for user {}: {}";
    public static final String LOG_AVATAR_DELETED_SUCCESS = "Avatar deleted successfully for user {}: {}";
    public static final String LOG_AVATAR_DELETE_FAILED = "Failed to delete avatar for user: {}";
    public static final String LOG_AVATAR_NOT_FOUND = "Avatar not found for user: {}";
    public static final String MSG_INVALID_DOB_FORMAT = "Invalid date of birth format. Please use YYYY-MM-DD.";
    public static final String LOG_INVALID_DOB_FORMAT = "Invalid DOB format for user {}: {}";
    public static final String LOG_AVATAR_FILE_TOO_LARGE = "Avatar file too large for user {}: {} bytes";
}
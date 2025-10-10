package com.graduation.schedulingservice.constant;

public class Constant {


    // ===================== SERVICE LOG MESSAGES =====================
    public static final String LOG_MESSAGE_ALREADY_PROCESSED = "Message already processed: messageId={}";
    public static final String LOG_MESSAGE_NOT_PROCESSED = "Message not processed: messageId={}, sagaId={}";
    public static final String LOG_RECORDED_PROCESSING = "Recorded processing for messageId: {}, sagaId: {}, status: {}";
    public static final String LOG_FAILED_TO_RECORD_PROCESSING = "Failed to record processing for messageId: {}, sagaId: {}";
    public static final String LOG_CLEANING_OLD_MESSAGES = "Cleaning up old processed messages";
    public static final String LOG_DELETED_OLD_MESSAGES = "Deleted {} old processed messages";
    public static final String LOG_OLD_MESSAGES_CLEANUP_SUCCESS = "Old processed messages cleanup completed successfully";
    public static final String LOG_NO_OLD_MESSAGES = "No old processed messages to clean up";

    // ===================== FORMAT STRINGS =====================
    public static final String FORMAT_PROCESSED_MESSAGE_TOSTRING = "ProcessedMessage{messageId='%s', sagaId='%s', status=%s, processedAt=%s}";

    // ===================== TABLE AND COLUMN NAMES =====================
    public static final String TABLE_PROCESSED_MESSAGES = "processed_messages";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_PROCESSED_AT = "processed_at";
    public static final String COLUMN_SAGA_ID = "saga_id";
    public static final String COLUMN_MESSAGE_ID = "message_id";


    // ===================== CLEANUP CONFIGURATION =====================
    public static final Integer CLEANUP_HOURS_THRESHOLD = 30;

    // ===================== CALENDAR ITEM MESSAGES =====================
    public static final String MSG_ITEM_CREATED_SUCCESS = "Calendar item created successfully";
    public static final String MSG_ITEM_NOT_FOUND = "Calendar item not found";
    public static final String MSG_UNAUTHORIZED_ACCESS = "Unauthorized access to calendar item";
    public static final String MSG_INVALID_ITEM_TYPE = "Invalid item type. Must be TASK, ROUTINE, or EVENT";
    public static final String MSG_INVALID_TIME_SLOT = "Invalid time slot: end time must be after start time";
    public static final String MSG_CALENDAR_NOT_FOUND = "Calendar not found";

    // ===================== CALENDAR ITEM LOGS =====================
    public static final String LOG_ITEM_CREATED_SUCCESS = "Calendar item created successfully: userId={}, itemId={}, type={}";
    public static final String LOG_ITEM_CREATION_FAILED = "Failed to create calendar item: userId={}";
    public static final String LOG_INVALID_TIME_SLOT = "Invalid time slot provided: start={}, end={}";

    // ===================== ERROR MESSAGES =====================
    public static final String ERROR_MESSAGE_ID_REQUIRED = "messageId is required";
    public static final String ERROR_MESSAGE_ID_REQUIRED_RECORD = "messageId is required to record processing";
    public static final String ERROR_SAGA_ID_REQUIRED = "sagaId is required to record processing";
    public static final String ERROR_FAILED_TO_RECORD = "Failed to record message processing";
}
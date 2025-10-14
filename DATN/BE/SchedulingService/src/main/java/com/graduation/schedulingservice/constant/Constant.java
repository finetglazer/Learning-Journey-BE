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
    public static final String MSG_UNAUTHORIZED_ACCESS = "Unauthorized access";
    public static final String MSG_INVALID_ITEM_TYPE = "Invalid item type. Must be TASK, ROUTINE, or EVENT";
    public static final String MSG_INVALID_TIME_SLOT = "Invalid time slot: end time must be after start time";
    public static final String MSG_CALENDAR_NOT_FOUND = "Calendar not found";
    public static final String MSG_CONSTRAINT_VIOLATIONS = "Constraint violations detected";
    public static final String MSG_ITEM_CREATION_FAILED = "Failed to create calendar item";
    public static final String MSG_INVALID_TIMEZONE_FORMAT = "Invalid timezone format";
    public static final String MSG_NO_ITEMS_TO_CONVERT = "No calendar items to convert";
    public static final String MSG_CONVERSION_SUCCESS = "Successfully converted %d calendar items";
    public static final String MSG_CONVERSION_FAILED = "Failed to convert calendar items timezone";
    public static final String MSG_ITEM_FETCH_SUCCESS = "Calendar item retrieved successfully";
    public static final String MSG_ITEM_FETCH_FAILED = "Failed to fetch calendar item";
    public static final String MSG_ITEM_NAME_EMPTY = "Item name cannot be empty";
    public static final String MSG_INVALID_STATUS_VALUE = "Invalid status value";
    public static final String MSG_ITEM_UPDATE_SUCCESS = "Item updated successfully";
    public static final String MSG_ITEM_UPDATE_FAILED = "Failed to update calendar item";
    public static final String MSG_ITEM_DELETE_SUCCESS = "Item deleted successfully";
    public static final String MSG_ITEM_DELETE_FAILED = "Failed to delete calendar item";


    // ===================== CALENDAR ITEM LOGS =====================
    public static final String LOG_ITEM_CREATED_SUCCESS = "Calendar item created successfully: userId={}, itemId={}, type={}";
    public static final String LOG_ITEM_CREATION_FAILED = "Failed to create calendar item: userId={}";
    public static final String LOG_INVALID_TIME_SLOT = "Invalid time slot provided: start={}, end={}";
    public static final String LOG_INVALID_ITEM_TYPE = "Invalid item type provided: {}";
    public static final String LOG_CALENDAR_UNAUTHORIZED = "Calendar not found or unauthorized: calendarId={}, userId={}";
    public static final String LOG_CONSTRAINT_VIOLATIONS = "Constraint violations for user {}: {}";
    public static final String LOG_INVALID_DAY_OF_WEEK = "Invalid day of week: {}";
    public static final String LOG_TIMEZONE_CONVERT_START = "Converting timezone for user {} from {} to {}";
    public static final String LOG_INVALID_TIMEZONE = "Invalid timezone format: old={}, new={}";
    public static final String LOG_NO_ITEMS_FOR_USER = "No calendar items found for user {}";
    public static final String LOG_CONVERSION_SUCCESS = "Successfully converted {} calendar items for user {}";
    public static final String LOG_CONVERSION_FAILED = "Failed to convert timezone for user {}";
    public static final String LOG_FETCHING_ITEM = "Fetching calendar item: userId={}, itemId={}";
    public static final String LOG_ITEM_NOT_FOUND = "Calendar item not found: itemId={}";
    public static final String LOG_UNAUTHORIZED_ACCESS = "Unauthorized access attempt: userId={}, itemId={}, ownerId={}";
    public static final String LOG_ITEM_FETCHED_SUCCESS = "Calendar item fetched successfully: itemId={}";
    public static final String LOG_FETCHING_ITEM_FAILED = "Failed to fetch calendar item: itemId={}";
    public static final String LOG_UPDATING_ITEM = "Updating calendar item: userId={}, itemId={}";
    public static final String LOG_UNAUTHORIZED_UPDATE = "Unauthorized update attempt: userId={}, itemId={}, ownerId={}";
    public static final String LOG_INVALID_NAME_UPDATE = "Invalid name provided for itemId={}";
    public static final String LOG_INVALID_STATUS_UPDATE = "Invalid status value: {}";
    public static final String LOG_ITEM_UPDATED_SUCCESS = "Calendar item updated successfully: itemId={}";
    public static final String LOG_UPDATE_ITEM_FAILED = "Failed to update calendar item: itemId={}";
    public static final String LOG_DELETING_ITEM = "Deleting calendar item: userId={}, itemId={}";
    public static final String LOG_UNAUTHORIZED_DELETE = "Unauthorized delete attempt: userId={}, itemId={}, ownerId={}";
    public static final String LOG_ITEM_DELETED_SUCCESS = "Calendar item deleted successfully: itemId={}";
    public static final String LOG_DELETE_ITEM_FAILED = "Failed to delete calendar item: itemId={}";

    // ===================== VALIDATION =====================
    public static final String VALIDATION_OVERLAP_MESSAGE = "overlaps with existing calendar items";

    // ===================== ERROR MESSAGES =====================
    public static final String ERROR_MESSAGE_ID_REQUIRED = "messageId is required";
    public static final String ERROR_MESSAGE_ID_REQUIRED_RECORD = "messageId is required to record processing";
    public static final String ERROR_SAGA_ID_REQUIRED = "sagaId is required to record processing";
    public static final String ERROR_FAILED_TO_RECORD = "Failed to record message processing";

    // ===================== ITEM RETRIEVAL MESSAGES =====================
    public static final String MSG_ITEMS_RETRIEVED_SUCCESS = "Items retrieved successfully";
    public static final String MSG_ITEMS_RETRIEVAL_FAILED = "Failed to retrieve items";
    public static final String MSG_UNSCHEDULED_ITEMS_RETRIEVED_SUCCESS = "Unscheduled items retrieved successfully";
    public static final String MSG_UNSCHEDULED_ITEMS_RETRIEVAL_FAILED = "Failed to retrieve unscheduled items";
    public static final String MSG_INVALID_DATE_FORMAT = "Invalid date format. Please use YYYY-MM-DD.";
    public static final String MSG_INVALID_VIEW_TYPE = "Invalid view type. Use DAY, WEEK, MONTH, or YEAR.";
    public static final String MSG_DATE_OUTSIDE_WINDOW = "Date must be within 5 years from today.";


    // ===================== ITEM RETRIEVAL LOGS =====================
    public static final String LOG_GET_ITEMS_BY_DATE_RANGE = "Getting calendar items for userId={}, view={}, date={}, calendarIds={}";
    public static final String LOG_GET_ITEMS_BY_DATE_RANGE_FAILED = "Failed to get items by date range for userId={}";
    public static final String LOG_INVALID_DATE_FORMAT = "Invalid date format for getItemsByDateRange: {}";
    public static final String LOG_INVALID_VIEW_TYPE = "Invalid view type for getItemsByDateRange: {}";
    public static final String LOG_DATE_OUTSIDE_WINDOW = "Date is outside the allowed 5-year window: {}";
    public static final String LOG_GET_UNSCHEDULED_ITEMS = "Getting unscheduled items for userId={}, weekPlanId={}";
    public static final String LOG_GET_UNSCHEDULED_ITEMS_FAILED = "Failed to get unscheduled items for userId={}";

    // ===================== CALENDAR MESSAGES =====================
    public static final String MSG_CALENDARS_RETRIEVED_SUCCESS = "Calendars retrieved successfully";
    public static final String MSG_CALENDARS_RETRIEVAL_FAILED = "Failed to retrieve calendars";

    // ===================== CALENDAR LOGS =====================
    public static final String LOG_GET_USER_CALENDARS = "Getting calendars for userId={}";
    public static final String LOG_GET_USER_CALENDARS_FAILED = "Failed to get calendars for userId={}";

}
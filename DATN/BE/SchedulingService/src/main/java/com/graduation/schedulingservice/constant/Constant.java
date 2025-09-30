package com.graduation.schedulingservice.constant;

public class Constant {

    // ===================== RESPONSE FIELD NAMES =====================
    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_MESSAGE = "message";
    public static final String RESPONSE_ERROR_MESSAGE = "errorMessage";
    public static final String RESPONSE_PAYMENT_TRANSACTION_ID = "paymentTransactionId";
    public static final String RESPONSE_TRANSACTION_ID = "transactionId";
    public static final String RESPONSE_TRANSACTION_REFERENCE = "transactionReference";
    public static final String RESPONSE_TIMESTAMP = "timestamp";
    public static final String RESPONSE_SERVICE = "service";
    public static final String RESPONSE_STATUS = "status";

    // ===================== SUCCESS MESSAGES =====================
    public static final String PAYMENT_PROCESSED_SUCCESS = "Payment processed successfully";
    public static final String PAYMENT_CONFIRMED_SUCCESS = "Payment confirmed successfully";
    public static final String PAYMENT_REVERSED_SUCCESS = "Payment reversed successfully";
    public static final String PAYMENT_RETRY_SUCCESS = "Payment retry initiated successfully";

    // ===================== ERROR MESSAGES =====================
    public static final String PAYMENT_NOT_FOUND = "Payment transaction not found";
    public static final String FAILED_TO_PROCESS_PAYMENT = "Failed to process payment: ";
    public static final String ERROR_RETRIEVING_PAYMENT = "Error retrieving payment: ";
    public static final String ERROR_REVERSING_PAYMENT = "Error reversing payment: ";

    // ===================== DEFAULT VALUES =====================
    public static final String DEFAULT_PAYMENT_METHOD = "CREDIT_CARD";
    public static final Integer DEFAULT_RETRY_COUNT = 0;
    public static final Integer MAX_RETRY_COUNT = 3;

    // ===================== SERVICE INFORMATION =====================
    public static final String SERVICE_NAME = "PaymentService";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_UP = "UP";
    public static final String TEST_ENDPOINT_MESSAGE = "Payment Service Test Endpoint";

    // ===================== KAFKA COMMAND TYPES =====================
    public static final String COMMAND_PAYMENT_PROCESS = "PAYMENT_PROCESS";
    public static final String COMMAND_PAYMENT_REVERSE = "PAYMENT_REVERSE";

    // ===================== KAFKA EVENT TYPES =====================
    public static final String EVENT_PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    public static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String EVENT_PAYMENT_REVERSED = "PAYMENT_REVERSED";
    public static final String EVENT_PAYMENT_REVERSE_FAILED = "PAYMENT_REVERSE_FAILED";

    // ===================== MAP FIELD NAMES =====================
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_SAGA_ID = "sagaId";
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_PAYLOAD = "payload";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USER_EMAIL = "userEmail";
    public static final String FIELD_USER_NAME = "userName";
    public static final String FIELD_TOTAL_AMOUNT = "totalAmount";
    public static final String FIELD_PAYMENT_METHOD = "paymentMethod";
    public static final String FIELD_PAYMENT_TRANSACTION_ID = "paymentTransactionId";
    public static final String FIELD_TRANSACTION_REFERENCE = "transactionReference";
    public static final String FIELD_EXTERNAL_TRANSACTION_ID = "externalTransactionId";
    public static final String FIELD_AUTH_TOKEN = "authToken";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_SUCCESS = "success";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_AMOUNT = "amount";
    public static final String FIELD_STATUS = "status";

    // ===================== VALIDATION MESSAGES =====================
    // PaymentTransaction validation messages
    public static final String VALIDATION_ORDER_ID_BLANK = "Order ID cannot be blank";
    public static final String VALIDATION_USER_ID_BLANK = "User ID cannot be blank";
    public static final String VALIDATION_AMOUNT_NULL = "Amount cannot be null";
    public static final String VALIDATION_AMOUNT_MIN = "Amount must be greater than 0";
    public static final String VALIDATION_AMOUNT_DIGITS = "Amount must have at most 10 integer digits and 2 decimal places";
    public static final String VALIDATION_PAYMENT_STATUS_NULL = "Payment status cannot be null";

    // ProcessedMessage validation messages
    public static final String VALIDATION_MESSAGE_ID_NULL = "Message ID cannot be null";
    public static final String VALIDATION_SAGA_ID_NULL = "Saga ID cannot be null";
    public static final String VALIDATION_PROCESSED_AT_NULL = "Processed at cannot be null";
    public static final String VALIDATION_PROCESS_STATUS_NULL = "Process status cannot be null";

    // ===================== PAYMENT BUSINESS LOGIC MESSAGES =====================
    public static final String ERROR_INVALID_STATUS_TRANSITION = "Invalid status transition from %s to %s";
    public static final String ERROR_PAYMENT_ONLY_PROCESS_PENDING = "Payment can only be processed from PENDING status, current: %s";
    public static final String ERROR_PAYMENT_ONLY_CONFIRM_PENDING = "Payment can only be confirmed from PENDING status, current: %s";
    public static final String ERROR_PAYMENT_ONLY_DECLINE_PENDING = "Payment can only be declined from PENDING status, current: %s";
    public static final String ERROR_CANNOT_MARK_FAILED_FINAL = "Cannot mark as failed from final status: %s";
    public static final String ERROR_RETRY_NOT_ALLOWED = "Payment retry not allowed for status: %s";
    public static final String ERROR_MAX_RETRY_EXCEEDED = "Maximum retry count exceeded for payment";

    // ===================== DEFAULT REASONS =====================
    public static final String REASON_PAYMENT_PROCESSED = "Payment processed successfully";
    public static final String REASON_PAYMENT_CONFIRMED = "Payment confirmed successfully";
    public static final String REASON_PAYMENT_DECLINED = "Payment declined by processor";
    public static final String REASON_PAYMENT_FAILED = "Payment processing failed";
    public static final String REASON_PAYMENT_REVERSED = "Payment reversed successfully";
    public static final String REASON_PAYMENT_RETRY_FORMAT = "Payment retry attempt #%d";
    public static final String REASON_HIGH_AMOUNT_DECLINED = "High amount transaction declined";
    public static final String REASON_TECHNICAL_ERROR = "Technical error during processing";

    // ===================== IDEMPOTENCY SERVICE MESSAGES =====================
    public static final String ERROR_MESSAGE_ID_REQUIRED = "messageId is required for idempotency check";
    public static final String ERROR_MESSAGE_ID_REQUIRED_RECORD = "messageId is required";
    public static final String ERROR_SAGA_ID_REQUIRED = "sagaId is required";
    public static final String ERROR_FAILED_TO_RECORD = "Failed to record processing";

    // ===================== CONTROLLER LOG MESSAGES =====================
    public static final String LOG_PROCESSING_PAYMENT = "Processing payment for order: {}, amount: {}, sagaId: {}";
    public static final String LOG_ERROR_PROCESSING_PAYMENT = "Error processing payment for order: {}";
    public static final String LOG_ERROR_GETTING_PAYMENT = "Error getting payment: {}";
    public static final String LOG_ERROR_REVERSING_PAYMENT = "Error reversing payment: {}";

    // ===================== KAFKA LISTENER LOG MESSAGES =====================
    public static final String LOG_PROCESSING_PAYMENT_COMMAND = "Processing payment command type: {} for saga: {} messageId: {}";
    public static final String LOG_UNKNOWN_PAYMENT_COMMAND = "Unknown payment command type: {} for saga: {}";
    public static final String LOG_PAYMENT_COMMAND_ACKNOWLEDGED = "Payment command acknowledged: {} for saga: {}";
    public static final String LOG_ERROR_PROCESSING_PAYMENT_COMMAND = "Error processing payment command: {}";
    public static final String ERROR_PAYMENT_COMMAND_PROCESSING_FAILED = "Payment command processing failed";

    // ===================== SERVICE LOG MESSAGES =====================
    public static final String LOG_MESSAGE_ALREADY_PROCESSED = "Message already processed: messageId={}";
    public static final String LOG_MESSAGE_NOT_PROCESSED = "Message not processed: messageId={}, sagaId={}";
    public static final String LOG_RECORDED_PROCESSING = "Recorded processing for messageId: {}, sagaId: {}, status: {}";
    public static final String LOG_FAILED_TO_RECORD_PROCESSING = "Failed to record processing for messageId: {}, sagaId: {}";
    public static final String LOG_CLEANING_OLD_MESSAGES = "Cleaning up old processed messages";
    public static final String LOG_DELETED_OLD_MESSAGES = "Deleted {} old processed messages";
    public static final String LOG_OLD_MESSAGES_CLEANUP_SUCCESS = "Old processed messages cleanup completed successfully";
    public static final String LOG_NO_OLD_MESSAGES = "No old processed messages to clean up";
    public static final String LOG_COMMAND_ALREADY_PROCESSED = "Command already processed: sagaId={}, messageId={}";
    public static final String LOG_COMMAND_PAYLOAD_NULL = "Command payload is null for sagaId: {}";
    public static final String LOG_INVALID_PAYMENT_COMMAND_DATA = "Invalid payment command data for sagaId: {}, orderId: {}, userId: {}, amount: {}";
    public static final String LOG_INVALID_REVERSE_PAYMENT_DATA = "Invalid reverse payment command data for sagaId: {}, orderId: {}, paymentTransactionId: {}";
    public static final String LOG_PROCESSING_PAYMENT_ORDER = "Processing payment for order: {}, amount: {}, sagaId: {}";
    public static final String LOG_REVERSING_PAYMENT_ORDER = "Reversing payment for order: {}, sagaId: {}";
    public static final String LOG_PUBLISHING_PAYMENT_EVENT = "Publishing payment event: type={}, success={}, sagaId={}";
    public static final String LOG_ERROR_PUBLISHING_EVENT = "Error publishing payment event: {}";

    // ===================== BUSINESS ERROR MESSAGES =====================
    public static final String ERROR_PAYMENT_NOT_FOUND_ID = "Payment transaction not found: %s";
    public static final String ERROR_FAILED_TO_PROCESS_PAYMENT_RUNTIME = "Failed to process payment: %s";
    public static final String ERROR_INVALID_PAYMENT_ID = "Invalid payment transaction ID";
    public static final String ERROR_INVALID_COMMAND_FORMAT = "Invalid command format: missing payload";
    public static final String ERROR_INVALID_PAYMENT_DATA = "Invalid payment command data";
    public static final String ERROR_INVALID_REVERSE_DATA = "Invalid reverse payment command data";
    public static final String ERROR_TECHNICAL_PREFIX = "Technical error: ";

    // ===================== KAFKA TOPICS AND PREFIXES =====================
    public static final String TOPIC_PAYMENT_EVENTS = "payment.events";
    public static final String PREFIX_PAYMENT_MESSAGE = "PAY_";
    public static final String PREFIX_EXTERNAL_TRANSACTION = "EXT_";
    public static final String PREFIX_AUTH_TOKEN = "AUTH_";

    // ===================== FORMAT STRINGS =====================
    public static final String FORMAT_PAYMENT_TRANSACTION_TOSTRING = "PaymentTransaction{id=%d, orderId='%s', userId='%s', amount=%s, status=%s, sagaId='%s'}";
    public static final String FORMAT_PROCESSED_MESSAGE_TOSTRING = "ProcessedMessage{messageId='%s', sagaId='%s', status=%s, processedAt=%s}";

    // ===================== TABLE AND COLUMN NAMES =====================
    public static final String TABLE_PAYMENT_TRANSACTIONS = "payment_transactions";
    public static final String TABLE_PROCESSED_MESSAGES = "processed_messages";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_AUTH_TOKEN = "auth_token";
    public static final String COLUMN_MOCK_DECISION_REASON = "mock_decision_reason";
    public static final String COLUMN_PROCESSED_AT = "processed_at";
    public static final String COLUMN_SAGA_ID = "saga_id";
    public static final String COLUMN_PAYMENT_METHOD = "payment_method";
    public static final String COLUMN_TRANSACTION_REFERENCE = "transaction_reference";
    public static final String COLUMN_EXTERNAL_TRANSACTION_ID = "external_transaction_id";
    public static final String COLUMN_FAILURE_REASON = "failure_reason";
    public static final String COLUMN_RETRY_COUNT = "retry_count";
    public static final String COLUMN_LAST_RETRY_AT = "last_retry_at";
    public static final String COLUMN_MESSAGE_ID = "message_id";

    // ===================== INDEX NAMES =====================
    public static final String INDEX_PAYMENT_ORDER_ID = "idx_payment_order_id";
    public static final String INDEX_PAYMENT_USER_ID = "idx_payment_user_id";
    public static final String INDEX_PAYMENT_STATUS = "idx_payment_status";
    public static final String INDEX_PAYMENT_SAGA_ID = "idx_payment_saga_id";
    public static final String INDEX_PAYMENT_PROCESSED_AT = "idx_payment_processed_at";

    // ===================== MOCK PAYMENT PROCESSING =====================
    public static final Double HIGH_AMOUNT_THRESHOLD = 1000.0;
    public static final Double DECLINE_PROBABILITY = 0.3;
    public static final Double FAILURE_PROBABILITY = 0.1;
    public static final Integer UUID_SUBSTRING_LENGTH = 8;
    public static final Integer MOCK_PROCESSING_DELAY= 15000;

    // ===================== CLEANUP CONFIGURATION =====================
    public static final Integer CLEANUP_HOURS_THRESHOLD = 30;
}
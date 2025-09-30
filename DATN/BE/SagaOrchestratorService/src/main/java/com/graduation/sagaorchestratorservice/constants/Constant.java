package com.graduation.sagaorchestratorservice.constants;

public class Constant {

    // ===================== RESPONSE FIELD NAMES =====================
    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_MESSAGE = "message";
    public static final String RESPONSE_SAGA = "saga";
    public static final String RESPONSE_SAGAS = "sagas";
    public static final String RESPONSE_ACTIVE_SAGAS = "activeSagas";
    public static final String RESPONSE_TOTAL_COUNT = "totalCount";
    public static final String RESPONSE_SERVICE = "service";
    public static final String RESPONSE_STATUS = "status";
    public static final String RESPONSE_ERROR = "error";
    public static final String RESPONSE_TIMESTAMP = "timestamp";
    public static final String RESPONSE_SAGA_ID = "sagaId";
    public static final String RESPONSE_ORDER_ID = "orderId";
    public static final String RESPONSE_USER_ID = "userId";
    public static final String RESPONSE_CURRENT_STEP = "currentStep";
    public static final String RESPONSE_TOTAL_AMOUNT = "totalAmount";
    public static final String RESPONSE_START_TIME = "startTime";
    public static final String RESPONSE_LAST_UPDATED_TIME = "lastUpdatedTime";
    public static final String RESPONSE_RETRY_COUNT = "retryCount";
    public static final String RESPONSE_FAILURE_REASON = "failureReason";
    public static final String RESPONSE_COMPLETED_STEPS = "completedSteps";
    public static final String RESPONSE_SAGA_EVENTS = "sagaEvents";
    public static final String RESPONSE_ACTIVE_COUNT = "activeCount";
    public static final String RESPONSE_TOTAL_PROCESSED = "totalProcessed";
    public static final String RESPONSE_TOTAL_FAILURES = "totalFailures";
    public static final String RESPONSE_FAILURE_RATE = "failureRate";

    // ===================== SUCCESS MESSAGES =====================
    public static final String SAGA_CANCELLATION_INITIATED = "Saga cancellation initiated";
    public static final String SAGA_COMPLETED_SUCCESS = "Saga completed successfully";

    // ===================== ERROR MESSAGES =====================
    public static final String SAGA_NOT_FOUND = "Saga not found";
    public static final String SAGA_NOT_FOUND_FOR_ORDER = "Saga not found for order: ";
    public static final String ERROR_RETRIEVING_SAGA = "Error retrieving saga: ";
    public static final String ERROR_RETRIEVING_USER_SAGAS = "Error retrieving user sagas: ";
    public static final String ERROR_RETRIEVING_ACTIVE_SAGAS = "Error retrieving active sagas: ";
    public static final String ERROR_CANCELLING_SAGA = "Error cancelling saga: ";
    public static final String ERROR_GETTING_HEALTH_STATUS = "Error getting health status";
    public static final String ERROR_PROCESSING_EVENT = "Error processing event: ";
    public static final String ERROR_PROCESSING_RECORD = "Error processing record: ";
    public static final String ERROR_HANDLING_ORDER_CREATED = "Error handling ORDER_CREATED event";
    public static final String ERROR_HANDLING_PAYMENT_EVENT = "Error handling payment event";
    public static final String ERROR_HANDLING_ORDER_EVENT = "Error handling order event";
    public static final String ERROR_HANDLING_SAGA_EVENT = "Error handling saga event";
    public static final String ERROR_PROCESSING_DLQ_MESSAGE = "Error processing DLQ message";
    public static final String ERROR_PROCESSING_HEALTH_CHECK = "Error processing health check message";

    // ===================== SERVICE INFORMATION =====================
    public static final String SERVICE_NAME = "SagaOrchestratorService";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    public static final String TEST_ENDPOINT_MESSAGE = "Saga Orchestrator Service Test Endpoint";

    // ===================== KAFKA EVENT TYPES =====================
    // Order Events
    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_STATUS_UPDATED_CONFIRMED = "ORDER_STATUS_UPDATED_CONFIRMED";
    public static final String EVENT_ORDER_STATUS_UPDATED_DELIVERED = "ORDER_STATUS_UPDATED_DELIVERED";
    public static final String EVENT_ORDER_STATUS_UPDATE_FAILED = "ORDER_STATUS_UPDATE_FAILED";
    public static final String EVENT_ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String EVENT_ORDER_CANCELLATION_FAILED = "ORDER_CANCELLATION_FAILED";
    public static final String EVENT_CANCEL_REQUEST_RECEIVED = "CANCEL_REQUEST_RECEIVED";
    public static final String EVENT_CANCELLATION_BLOCKED = "CANCELLATION_BLOCKED";
    public static final String EVENT_CANCELLATION_INITIATED = "CANCELLATION_INITIATED";

    // Payment Events
    public static final String EVENT_PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    public static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String EVENT_PAYMENT_REVERSED = "PAYMENT_REVERSED";
    public static final String EVENT_PAYMENT_CANCELLATION_FAILED = "PAYMENT_CANCELLATION_FAILED";

    // Saga Events
    public static final String EVENT_SAGA_TIMEOUT_CHECK = "SAGA_TIMEOUT_CHECK";
    public static final String EVENT_SAGA_MONITORING_UPDATE = "SAGA_MONITORING_UPDATE";
    public static final String EVENT_SAGA_EXTERNAL_CANCEL_REQUEST = "SAGA_EXTERNAL_CANCEL_REQUEST";



    // ===================== SAGA EVENT TYPES =====================
    public static final String SAGA_EVENT_INITIATED = "SAGA_INITIATED";
    public static final String SAGA_EVENT_STEP_STARTED = "STEP_STARTED";
    public static final String SAGA_EVENT_STEP_COMPLETED = "STEP_COMPLETED";
    public static final String SAGA_EVENT_STEP_FAILED = "STEP_FAILED";
    public static final String SAGA_EVENT_COMPENSATION_STARTED = "COMPENSATION_STARTED";
    public static final String SAGA_EVENT_COMPENSATION_COMPLETED = "COMPENSATION_COMPLETED";
    public static final String SAGA_EVENT_COMPENSATION_STEP = "COMPENSATION_STEP";
    public static final String SAGA_EVENT_COMPENSATION_RETRY = "COMPENSATION_RETRY";
    public static final String SAGA_EVENT_COMPENSATION_FAILED = "COMPENSATION_FAILED";
    public static final String SAGA_EVENT_SAGA_COMPLETED = "SAGA_COMPLETED";
    public static final String SAGA_EVENT_SAGA_FAILED = "SAGA_FAILED";
    public static final String SAGA_EVENT_RETRY = "RETRY";

    // ===================== KAFKA COMMAND TYPES =====================
    public static final String COMMAND_PAYMENT_PROCESS = "PAYMENT_PROCESS";
    public static final String COMMAND_PAYMENT_REVERSE = "PAYMENT_REVERSE";
    public static final String COMMAND_ORDER_UPDATE_CONFIRMED = "ORDER_UPDATE_CONFIRMED";
    public static final String COMMAND_ORDER_UPDATE_DELIVERED = "ORDER_UPDATE_DELIVERED";
    public static final String COMMAND_ORDER_CANCEL = "ORDER_CANCEL";

    // ===================== MAP FIELD NAMES =====================
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_SAGA_ID = "sagaId";
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_PAYLOAD = "payload";
    public static final String FIELD_STEP_ID = "stepId";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USER_EMAIL = "userEmail";
    public static final String FIELD_USER_NAME = "userName";
    public static final String FIELD_ORDER_DESCRIPTION = "orderDescription";
    public static final String FIELD_TOTAL_AMOUNT = "totalAmount";
    public static final String FIELD_SUCCESS = "success";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_PAYMENT_TRANSACTION_ID = "paymentTransactionId";
    public static final String FIELD_NEW_STATUS = "newStatus";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_CANCELLED_BY = "cancelledBy";
    public static final String FIELD_REQUESTED_BY = "requestedBy";
    public static final String FIELD_UPDATE_TYPE = "updateType";
    public static final String FIELD_CANCELLATION_REASON = "cancellationReason";
    public static final String FIELD_CURRENT_STEP = "currentStep";
    public static final String FIELD_COMPENSATION_STRATEGY = "compensationStrategy";

    // ===================== MESSAGE TYPES =====================
    public static final String MESSAGE_TYPE_COMMAND = "COMMAND";
    public static final String MESSAGE_TYPE_EVENT = "EVENT";

    // ===================== STATUS VALUES =====================
    public static final String ORDER_STATUS_CONFIRMED = "CONFIRMED";
    public static final String ORDER_STATUS_DELIVERED = "DELIVERED";

    // ===================== REASON MESSAGES =====================
    public static final String REASON_PAYMENT_PROCESSED_SUCCESS = "Payment processed successfully";
    public static final String REASON_ORDER_CONFIRMED_SUCCESS = "Order confirmed - ready for delivery";
    public static final String REASON_ORDER_CANCELLED_SAGA = "Order cancelled by saga";
    public static final String REASON_CANCELLED_BY_USER = "Cancelled by user request";

    // ===================== ACTORS/PERFORMERS =====================
    public static final String ACTOR_SAGA_ORCHESTRATOR = "SAGA_ORCHESTRATOR";
    public static final String ACTOR_SAGA_COMPENSATION = "SAGA_COMPENSATION";

    // ===================== KAFKA CONSUMER GROUP SUFFIXES =====================
    public static final String GROUP_SUFFIX_ORDER_EVENTS = "-order-events";
    public static final String GROUP_SUFFIX_PAYMENT_EVENTS = "-payment-events";
    public static final String GROUP_SUFFIX_SAGA_EVENTS = "-saga-events";
    public static final String GROUP_SUFFIX_DLQ = "-dlq";
    public static final String GROUP_SUFFIX_HEALTH = "-health";

    // ===================== DLQ TOPIC SUFFIX =====================
    public static final String DLQ_TOPIC_SUFFIX = ".dlq";

    // ===================== VALIDATION MESSAGES =====================
    public static final String VALIDATION_MESSAGE_ID_REQUIRED = "messageId is required";
    public static final String VALIDATION_SAGA_ID_REQUIRED = "sagaId is required";
    public static final String VALIDATION_STEP_ID_REQUIRED = "stepId is required";
    public static final String VALIDATION_MESSAGE_NULL = "Message cannot be null";
    public static final String VALIDATION_TOPIC_NULL = "Topic cannot be null or empty";

    // ===================== TABLE AND COLUMN NAMES =====================
    // Table names
    public static final String TABLE_PROCESSED_MESSAGES = "processed_messages";
    public static final String TABLE_ORDER_PURCHASE_SAGAS = "order_purchase_sagas";

    // ProcessedMessage columns
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_SAGA_ID = "saga_id";
    public static final String COLUMN_STEP_ID = "step_id";
    public static final String COLUMN_MESSAGE_TYPE = "message_type";
    public static final String COLUMN_RESULT_JSON = "result_json";
    public static final String COLUMN_PROCESSED_AT = "processed_at";
    public static final String COLUMN_ACTION_TYPE = "action_type";

    // OrderPurchaseSagaState columns
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_USER_NAME = "user_name";
    public static final String COLUMN_ORDER_DESCRIPTION = "order_description";
    public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_PAYMENT_TRANSACTION_ID = "payment_transaction_id";
    public static final String COLUMN_CURRENT_STEP = "current_step";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_COMPLETED_STEPS = "completed_steps";
    public static final String COLUMN_SAGA_EVENTS = "saga_events";
    public static final String COLUMN_FAILURE_REASON = "failure_reason";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_END_TIME = "end_time";
    public static final String COLUMN_LAST_UPDATED_TIME = "last_updated_time";
    public static final String COLUMN_CURRENT_STEP_START_TIME = "current_step_start_time";
    public static final String COLUMN_RETRY_COUNT = "retry_count";
    public static final String COLUMN_MAX_RETRIES = "max_retries";
    public static final String COLUMN_COMPENSATION_RETRY_COUNT = "compensation_retry_count";
    public static final String COLUMN_MAX_COMPENSATION_RETRIES = "max_compensation_retries";

    // ===================== INDEX NAMES =====================
    // ProcessedMessage indexes
    public static final String INDEX_PROCESSED_MESSAGE_SAGA_STEP = "idx_processed_message_saga_step";
    public static final String INDEX_PROCESSED_MESSAGE_PROCESSED_AT = "idx_processed_message_processed_at";

    // OrderPurchaseSagaState indexes
    public static final String INDEX_SAGA_ORDER_ID = "idx_saga_order_id";
    public static final String INDEX_SAGA_USER_ID = "idx_saga_user_id";
    public static final String INDEX_SAGA_STATUS = "idx_saga_status";
    public static final String INDEX_SAGA_START_TIME = "idx_saga_start_time";

    // ===================== METRICS NAMES =====================
    public static final String METRIC_SAGA_ACTIVE_COUNT = "saga.active.count";
    public static final String METRIC_SAGA_TOTAL_PROCESSED = "saga.total.processed";
    public static final String METRIC_SAGA_MESSAGES_TOTAL_PROCESSED = "saga.messages.total.processed";
    public static final String METRIC_SAGA_TOTAL_FAILURES = "saga.total.failures";
    public static final String METRIC_SAGA_STARTED = "saga.started";
    public static final String METRIC_SAGA_COMPLETED = "saga.completed";
    public static final String METRIC_SAGA_FAILED = "saga.failed";
    public static final String METRIC_SAGA_MESSAGE_PROCESSED = "saga.message.processed";
    public static final String METRIC_SAGA_MESSAGE_FAILED = "saga.message.failed";
    public static final String METRIC_SAGA_EXECUTION_TIME = "saga.execution.time";
    public static final String METRIC_SAGA_MESSAGE_PROCESSING_TIME = "saga.message.processing.time";

    // ===================== METRIC DESCRIPTIONS =====================
    public static final String METRIC_DESC_SAGA_STARTED = "Number of sagas started";
    public static final String METRIC_DESC_SAGA_COMPLETED = "Number of sagas completed successfully";
    public static final String METRIC_DESC_SAGA_FAILED = "Number of sagas that failed";
    public static final String METRIC_DESC_MESSAGE_PROCESSED = "Number of messages processed";
    public static final String METRIC_DESC_MESSAGE_FAILED = "Number of message processing failures";
    public static final String METRIC_DESC_EXECUTION_TIME = "Time taken to complete sagas";
    public static final String METRIC_DESC_MESSAGE_PROCESSING_TIME = "Time taken to process individual messages";

    // ===================== LOG MESSAGES =====================
    // Controller log messages
    public static final String LOG_GETTING_SAGA = "Getting saga: {}";
    public static final String LOG_ERROR_GETTING_SAGA = "Error getting saga: {}";
    public static final String LOG_GETTING_SAGA_BY_ORDER = "Getting saga for order: {}";
    public static final String LOG_ERROR_GETTING_SAGA_BY_ORDER = "Error getting saga for order: {}";
    public static final String LOG_GETTING_SAGAS_BY_USER = "Getting sagas for user: {}";
    public static final String LOG_ERROR_GETTING_SAGAS_BY_USER = "Error getting sagas for user: {}";
    public static final String LOG_GETTING_ACTIVE_SAGAS = "Getting active sagas";
    public static final String LOG_ERROR_GETTING_ACTIVE_SAGAS = "Error getting active sagas";
    public static final String LOG_CANCELLING_SAGA = "Processing user cancellation request for saga: {}";
    public static final String LOG_ERROR_CANCELLING_SAGA = "Error cancelling saga: {}";
    public static final String LOG_ERROR_GETTING_HEALTH_STATUS = "Error getting health status";

    // Event handler log messages
    public static final String LOG_PROCESSING_ORDER_EVENT = "Processing order event type: {} for saga: {}";
    public static final String LOG_PROCESSING_PAYMENT_EVENT = "Processing payment event type: {} for saga: {}";
    public static final String LOG_PROCESSING_SAGA_EVENT = "Processing saga event type: {} for saga: {}";
    public static final String LOG_UNHANDLED_ORDER_EVENT = "Unhandled order event type: {}";
    public static final String LOG_UNHANDLED_PAYMENT_EVENT = "Unhandled payment event type: {}";
    public static final String LOG_UNHANDLED_SAGA_EVENT = "Unhandled saga event type: {} for saga: {}";
    public static final String LOG_STARTING_SAGA_FOR_ORDER = "Starting saga for order created event: orderId={}, userId={}, amount={}";
    public static final String LOG_SAGA_STARTED_SUCCESS = "Successfully started saga for order: {}";

    // Kafka listener log messages
    public static final String LOG_RECEIVED_ORDER_EVENT = "Received order event type: {} for saga: {} messageId: {}";
    public static final String LOG_RECEIVED_PAYMENT_EVENT = "Received payment event type: {} for saga: {} messageId: {}";
    public static final String LOG_RECEIVED_SAGA_EVENT = "Received saga event type: {} for saga: {} messageId: {}";
    public static final String LOG_RECEIVED_DLQ_MESSAGE = "Received message in DLQ: {}";
    public static final String LOG_RECEIVED_HEALTH_CHECK = "Received health check message: {}";
    public static final String LOG_EVENT_ACKNOWLEDGED = "Event acknowledged: {} for saga: {}";
    public static final String LOG_ERROR_PROCESSING_ORDER_EVENT = "Error processing order event: {}";
    public static final String LOG_ERROR_PROCESSING_PAYMENT_EVENT = "Error processing payment event: {}";
    public static final String LOG_ERROR_PROCESSING_SAGA_EVENT = "Error processing saga event: {}";

    // Saga service log messages
    public static final String LOG_STARTING_ORDER_PURCHASE_SAGA = "Starting order purchase saga for order: {} user: {}";
    public static final String LOG_SAGA_ALREADY_EXISTS = "Saga already exists for order: {}";
    public static final String LOG_ORDER_PURCHASE_SAGA_STARTED = "Order purchase saga started successfully: {}";
    public static final String LOG_PROCESSING_STEP = "Processing step [{}] for saga: {}";
    public static final String LOG_PUBLISHED_COMMAND = "Published command [{}] for saga [{}] to topic: {}";
    public static final String LOG_ERROR_PROCESSING_STEP = "Error processing step {} for saga {}";
    public static final String LOG_HANDLING_EVENT = "Handling event [{}] for saga: {}";
    public static final String LOG_EVENT_ALREADY_PROCESSED = "Event [{}] for saga [{}] has already been processed";
    public static final String LOG_EVENT_IGNORED_WRONG_STEP = "Event [{}] doesn't match current step [{}] for saga [{}]";
    public static final String LOG_PROCESSING_SUCCESS_EVENT = "Processing success event [{}] for saga [{}]";
    public static final String LOG_PROCESSING_FAILURE_EVENT = "Processing failure event [{}] for saga [{}]: {}";
    public static final String LOG_HANDLING_STEP_FAILURE = "Handling step failure for saga [{}]: {}";
    public static final String LOG_STARTING_COMPENSATION = "Starting compensation for saga: {}";
    public static final String LOG_COMPENSATION_STEP_COMPLETED = "Compensation step [{}] completed for saga: {}";
    public static final String LOG_COMPENSATION_STEP_FAILED = "🚨 Compensation step FAILED for saga [{}]: {}";
    public static final String LOG_RETRYING_COMPENSATION = "🔄 Retrying compensation step for saga [{}], attempt {}/{}";
    public static final String LOG_COMPENSATION_FAILED_FINAL = "❌ COMPENSATION FAILED for saga [{}] after {} retries. Manual intervention required!";
    public static final String LOG_COMPLETING_SAGA = "Completing saga successfully: {}";
    public static final String LOG_SAGA_STEP_TIMED_OUT = "Saga step timed out: {}";
    public static final String LOG_HANDLING_MANUAL_TIMEOUT = "Handling manual timeout for saga: {} - {}";
    public static final String LOG_SAGA_NO_LONGER_ACTIVE = "Saga {} is no longer active, skipping timeout handling";
    public static final String LOG_SAGA_NOT_FOUND_TIMEOUT = "Saga {} not found for manual timeout handling";
    public static final String LOG_SCHEDULING_RETRY = "Scheduling retry for saga {} with delay of {}ms (attempt {})";
    public static final String LOG_EXECUTING_DELAYED_RETRY = "Executing delayed retry for saga {} (attempt {})";
    public static final String LOG_SAGA_STATE_CHANGED_RETRY = "Saga {} state changed during retry delay, skipping retry";
    public static final String LOG_SAGA_NOT_FOUND_RETRY = "Saga {} not found during delayed retry";
    public static final String LOG_ERROR_DELAYED_RETRY = "Error during delayed retry for saga {}";

    // Monitoring service log messages
    public static final String LOG_INITIALIZING_METRICS = "Initializing saga monitoring metrics";
    public static final String LOG_METRICS_INITIALIZED = "Saga monitoring metrics initialized successfully";
    public static final String LOG_RECORDED_SAGA_STARTED = "Recorded saga started: {} of type {}";
    public static final String LOG_RECORDED_SAGA_COMPLETED = "Recorded saga completed: {} in {}ms";
    public static final String LOG_RECORDED_SAGA_FAILED = "Recorded saga failed: {} after {}ms, reason: {}";
    public static final String LOG_UNKNOWN_SAGA_COMPLETION = "Attempted to record completion for unknown saga: {}";
    public static final String LOG_UNKNOWN_SAGA_FAILURE = "Attempted to record failure for unknown saga: {}";
    public static final String LOG_RECORDED_MESSAGE_PROCESSED = "Recorded message processed: type={}, saga={}, time={}ms";
    public static final String LOG_RECORDED_MESSAGE_FAILED = "Recorded message failed: type={}, saga={}, reason={}";
    public static final String LOG_UPDATING_METRICS = "Updating saga metrics";
    public static final String LOG_CURRENT_METRICS = "Current saga metrics: active={}, processed={}, failures={}, failure_rate={:.2f}%";
    public static final String LOG_ERROR_UPDATING_METRICS = "Error updating saga metrics";
    public static final String LOG_METRICS_RESET = "Saga metrics reset";

    // Scheduler log messages
    public static final String LOG_TIMEOUT_CHECK_RUNNING = "Running scheduled timeout check #{}";
    public static final String LOG_TIMEOUT_CHECK_COMPLETED = "Timeout check #{} completed in {}ms - found {} timed-out sagas";
    public static final String LOG_TIMEOUT_CHECK_NO_TIMEOUTS = "Timeout check #{} completed in {}ms - no timed-out sagas found";
    public static final String LOG_ERROR_TIMEOUT_CHECK = "Error during scheduled timeout check #{}";
    public static final String LOG_CHECKING_ACTIVE_SAGAS = "Checking {} active sagas for timeouts";
    public static final String LOG_CHECKING_TIMED_OUT_SAGAS = "Checking for timed-out saga steps with timeout: {}";
    public static final String LOG_FOUND_TIMED_OUT_SAGAS = "Found {} timed-out sagas with timeout {}";
    public static final String LOG_TRIGGERING_COMPENSATION = "Triggering compensation for saga {} due to: {}";
    public static final String LOG_TIMEOUT_COMPENSATION_TRIGGERED = "Timeout compensation triggered for saga: {}";
    public static final String LOG_ERROR_TRIGGERING_COMPENSATION = "Error triggering compensation for saga {}: {}";
    public static final String LOG_STARTING_DAILY_CLEANUP = "Starting daily saga cleanup task";
    public static final String LOG_DAILY_SUMMARY = "Daily summary: {} timeout checks performed, {} sagas timed out";
    public static final String LOG_ERROR_DAILY_CLEANUP = "Error during daily cleanup";

    // Idempotency service log messages
    public static final String LOG_MESSAGE_ALREADY_PROCESSED = "Message already processed: messageId={}";
    public static final String LOG_MESSAGE_ALREADY_PROCESSED_SAGA_STEP = "Message already processed: sagaId={}, stepId={}, actionType={}";
    public static final String LOG_MESSAGE_NOT_PROCESSED = "Message not processed before: messageId={}, sagaId={}, stepId={}, actionType={}";
    public static final String LOG_GENERATED_MESSAGE_ID = "Generated messageId {} for sagaId {} and stepId {}";
    public static final String LOG_GENERATED_RANDOM_MESSAGE_ID = "Generated random messageId {} for message with insufficient context";
    public static final String LOG_RECORDED_MESSAGE_PROCESSING = "Successfully recorded message processing: messageId={}, sagaId={}, stepId={}";
    public static final String LOG_FAILED_RECORD_MESSAGE_PROCESSING = "Failed to record message processing: messageId={}, sagaId={}, stepId={}";
    public static final String LOG_FOUND_PROCESSED_RESULT = "Found processed result for messageId={}, sagaId={}, stepId={}";
    public static final String LOG_NO_PROCESSED_RESULT = "No processed result found for messageId={}, sagaId={}, stepId={}";
    public static final String LOG_CLEANED_SAGA_MESSAGES = "Cleaned up {} processed messages for saga {}";
    public static final String LOG_FAILED_CLEANUP_SAGA_MESSAGES = "Failed to cleanup processed messages for saga {}";
    public static final String LOG_STARTING_CLEANUP_OLD_MESSAGES = "Starting cleanup of old processed messages";
    public static final String LOG_COMPLETED_CLEANUP = "Completed cleanup: deleted {} old processed messages";
    public static final String LOG_NO_OLD_MESSAGES_CLEANUP = "No old processed messages to cleanup";
    public static final String LOG_FAILED_CLEANUP_OLD_MESSAGES = "Failed to cleanup old processed messages";
    public static final String LOG_MANUAL_CLEANUP = "Manual cleanup: deleted {} processed messages older than {} days";
    public static final String LOG_FAILED_MANUAL_CLEANUP = "Failed to manually cleanup processed messages";

    // Publisher log messages
    public static final String LOG_PUBLISHING_MESSAGE = "Publishing message to topic: {}, key: {}, messageType: {}";
    public static final String LOG_FAILED_SEND_MESSAGE = "Failed to send message to topic: {}, key: {}, error: {}";
    public static final String LOG_SUCCESS_SEND_MESSAGE = "Successfully sent message to topic: {}, key: {}, offset: {}, partition: {}";
    public static final String LOG_NO_MESSAGES_BATCH = "No messages to publish in batch";
    public static final String LOG_PUBLISHING_BATCH = "Publishing batch of {} messages to topic: {}";
    public static final String LOG_FAILED_PUBLISH_BATCH = "Failed to publish message in batch: {}";
    public static final String LOG_PUBLISHING_TO_DLQ = "Publishing message to DLQ: topic={}, reason={}";
    public static final String LOG_KAFKA_HEALTH_CHECK_FAILED = "Kafka health check failed";

    // Log cancel messages
    public static final String LOG_CANCEL_REQUEST_RECEIVED = "Cancel request received for saga: {}, orderId: {}, reason: {}";
    public static final String LOG_CANCELLATION_BLOCKED_PAYMENT = "Cancellation blocked - payment in progress: sagaId={}, orderId={}, lockHolder={}";
    public static final String LOG_CANCELLATION_PROCEEDING = "Payment not in progress, proceeding with cancellation: sagaId={}, orderId={}";
    public static final String LOG_COMPENSATION_STRATEGY_DETERMINED = "Compensation strategy determined for saga: {}, currentStep: {}, strategy: {}";
    public static final String LOG_EXECUTING_COMPENSATION_STEP = "Executing compensation step: {} for saga: {}";
    public static final String LOG_COMPENSATION_COMPLETED = "Compensation completed for saga: {}";
    public static final String LOG_COMPENSATION_FAILED = "Compensation failed for saga: {}, error: {}";

    // ===================== BUSINESS ERROR MESSAGES =====================
    public static final String ERROR_SAGA_NOT_FOUND = "Saga not found: %s";
    public static final String ERROR_CANNOT_CANCEL_SAGA = "Cannot cancel saga in state: %s";
    public static final String ERROR_STEP_FAILED = "Step failed without specific reason";
    public static final String ERROR_STEP_TIMED_OUT = "Step timed out after %d retries";
    public static final String ERROR_COMPENSATION_FAILED = "Compensation failed after retries: %s";
    public static final String ERROR_FAILED_TO_START_SAGA = "Failed to start saga for ORDER_CREATED event";
    public static final String ERROR_ORDER_EVENT_PROCESSING_FAILED = "Order event processing failed";
    public static final String ERROR_PAYMENT_EVENT_PROCESSING_FAILED = "Payment event processing failed";
    public static final String ERROR_SAGA_EVENT_PROCESSING_FAILED = "Saga event processing failed";

    // ===================== FORMAT STRINGS =====================
    public static final String FORMAT_SAGA_TOSTRING = "OrderPurchaseSaga{sagaId='%s', orderId=%d, userId='%s', status=%s, currentStep=%s}";
    public static final String FORMAT_PROCESSED_MESSAGE_TOSTRING = "ProcessedMessage{messageId='%s', sagaId='%s', stepId=%d, messageType='%s', processedAt=%s}";
    public static final String FORMAT_SAGA_EVENT_TOSTRING = "[%s] %s: %s";
    public static final String FORMAT_FAILURE_RATE = "%.2f%%";

    // ===================== DEFAULT VALUES =====================
    public static final String DEFAULT_EMPTY_JSON = "{}";
    public static final String DEFAULT_EMPTY_ARRAY = "[]";
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int DEFAULT_MAX_COMPENSATION_RETRIES = 3;
    public static final int DEFAULT_TIMEOUT_MINUTES = 10;
    public static final int DEFAULT_CONCURRENCY = 3;
    public static final int DEFAULT_PERCENTILES_COUNT = 4;

    // ===================== SAGA EVENT DESCRIPTIONS =====================
    public static final String DESC_SAGA_INITIATED = "Order purchase saga initiated for order: ";
    public static final String DESC_STEP_STARTED = "Started executing step: ";
    public static final String DESC_STEP_COMPLETED = "Successfully completed step: ";
    public static final String DESC_STEP_FAILED = "Step %s failed: %s";
    public static final String DESC_COMPENSATION_STARTED = "Started compensation: ";
    public static final String DESC_COMPENSATION_COMPLETED = "Compensation process completed successfully";
    public static final String DESC_SAGA_COMPLETED = "Order purchase saga completed successfully";
    public static final String DESC_SAGA_FAILED = "Saga execution failed: ";
    public static final String DESC_COMPENSATION_STEP = "Starting compensation with step: ";
    public static final String DESC_COMPENSATION_RETRY = "Retrying compensation step %s (attempt %d)";
    public static final String DESC_COMPENSATION_FAILED = "Compensation failed after %d retries: %s";
    public static final String DESC_RETRY_STEP = "Retrying step %s after timeout (attempt %d)";

    // ===================== HEALTH CHECK TOPIC =====================
    public static final String TOPIC_SAGA_HEALTH_CHECK = "saga.health.check";

    // ===================== JSON PARSER ERROR MESSAGES =====================
    public static final String ERROR_PARSE_RESULT_JSON = "Failed to parse result JSON for message {}: {}";
    public static final String ERROR_SERIALIZE_RESULT_MAP = "Failed to serialize result map to JSON: {}";
    public static final String ERROR_PARSE_COMPLETED_STEPS = "Failed to parse completed steps JSON for saga {}: {}";
    public static final String ERROR_PARSE_SAGA_EVENTS = "Failed to parse saga events JSON for saga {}: {}";
    public static final String ERROR_SERIALIZE_COMPLETED_STEPS = "Failed to serialize completed steps for saga {}";
    public static final String ERROR_SERIALIZE_SAGA_EVENTS = "Failed to serialize saga events for saga {}";
}
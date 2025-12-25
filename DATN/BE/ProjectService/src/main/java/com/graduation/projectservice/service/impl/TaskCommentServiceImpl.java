package com.graduation.projectservice.service.impl;

import com.graduation.projectservice.client.UserServiceClient;
import com.graduation.projectservice.constant.Constant;
import com.graduation.projectservice.exception.ForbiddenException;
import com.graduation.projectservice.exception.NotFoundException;
import com.graduation.projectservice.helper.ProjectAuthorizationHelper;
import com.graduation.projectservice.model.PM_TaskComment;
import com.graduation.projectservice.payload.request.CreateCommentRequest;
import com.graduation.projectservice.payload.request.UpdateCommentRequest;
import com.graduation.projectservice.payload.response.*;
import com.graduation.projectservice.repository.TaskCommentRepository;
import com.graduation.projectservice.repository.TaskRepository;
import com.graduation.projectservice.service.TaskCommentService;
import org.springframework.kafka.core.KafkaTemplate;
import com.graduation.projectservice.config.KafkaConfig;
import com.graduation.projectservice.event.TaskUpdateEvent;
import com.graduation.projectservice.model.PM_Task;
import com.graduation.projectservice.model.PM_TaskAssignee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskCommentServiceImpl implements TaskCommentService {

    private final TaskCommentRepository taskCommentRepository;
    private final TaskRepository taskRepository;
    private final ProjectAuthorizationHelper authHelper;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int REPLY_PREVIEW_MAX_LENGTH = 50;

    @Override
    @Transactional(readOnly = true)
    public BaseResponse<?> getComments(Long userId, Long taskId) {
        log.info(Constant.LOG_GETTING_COMMENTS, taskId, userId);

        // 1. Validate task exists and get project ID
        Long projectId = taskCommentRepository.findProjectIdByTaskId(taskId)
                .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

        // 2. Auth: Owner or Member
        authHelper.requireActiveMember(projectId, userId);

        // 3. Fetch all comments for the task
        List<PM_TaskComment> comments = taskCommentRepository.findByTaskIdOrderByCreatedAtAsc(taskId);

        if (comments.isEmpty()) {
            log.info(Constant.LOG_COMMENTS_RETRIEVED, taskId, 0);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.COMMENTS_RETRIEVED_SUCCESS,
                    Collections.emptyList());
        }

        // 4. Collect user IDs for batch fetching
        Set<Long> userIdsToFetch = new HashSet<>();
        comments.forEach(comment -> {
            userIdsToFetch.add(comment.getUserId());
            if (comment.getReplyToUserId() != null) {
                userIdsToFetch.add(comment.getReplyToUserId());
            }
        });

        // 5. Batch fetch user info from User Service
        List<UserBatchDTO> userDetails = userServiceClient.findUsersByIds(new ArrayList<>(userIdsToFetch));
        Map<Long, UserBatchDTO> userMap = userDetails.stream()
                .collect(Collectors.toMap(UserBatchDTO::getUserId, u -> u, (a, b) -> a));

        // 6. Map to DTOs
        List<TaskCommentDTO> commentDTOs = comments.stream().map(comment -> {
            UserBatchDTO user = userMap.get(comment.getUserId());

            ReplyInfoDTO replyInfo = null;
            if (comment.getParentCommentId() != null || comment.getReplyPreview() != null) {
                replyInfo = ReplyInfoDTO.builder()
                        .replyToUserId(comment.getReplyToUserId())
                        .replyPreview(comment.getReplyPreview())
                        .build();
            }

            return TaskCommentDTO.builder()
                    .commentId(comment.getCommentId())
                    .userId(comment.getUserId())
                    .userName(user != null ? user.getName() : "Unknown User")
                    .userAvatar(user != null ? user.getAvatarUrl() : null)
                    .content(comment.getContent())
                    .isEdited(comment.getUpdatedAt().isAfter(comment.getCreatedAt().plusSeconds(1)))
                    .createdAt(comment.getCreatedAt())
                    .replyInfo(replyInfo)
                    .build();
        }).collect(Collectors.toList());

        log.info(Constant.LOG_COMMENTS_RETRIEVED, taskId, commentDTOs.size());
        return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.COMMENTS_RETRIEVED_SUCCESS, commentDTOs);
    }

    @Override
    @Transactional
    public BaseResponse<?> createComment(Long userId, Long taskId, CreateCommentRequest request) {
        log.info(Constant.LOG_CREATING_COMMENT, taskId, userId);

        // 1. Validate task exists and get project ID
        Long projectId = taskCommentRepository.findProjectIdByTaskId(taskId)
                .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

        // 2. Auth: Owner or Member
        authHelper.requireActiveMember(projectId, userId);

        // 3. Validate content
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return new BaseResponse<>(Constant.ERROR_STATUS, Constant.ERROR_COMMENT_CONTENT_REQUIRED, null);
        }

        // 4. Handle reply logic
        String replyPreview = null;
        Long replyToUserId = null;

        if (request.getParentCommentId() != null) {
            Optional<PM_TaskComment> parentOpt = taskCommentRepository.findById(request.getParentCommentId());
            if (parentOpt.isPresent()) {
                PM_TaskComment parent = parentOpt.get();

                // Validate parent belongs to same task
                if (!parent.getTaskId().equals(taskId)) {
                    return new BaseResponse<>(Constant.ERROR_STATUS, Constant.ERROR_PARENT_COMMENT_INVALID, null);
                }

                replyToUserId = parent.getUserId();
                replyPreview = truncateForPreview(parent.getContent());
            }
            // If parent not found, we still create comment but without reply info (parent
            // was deleted)
        }

        // 5. Create comment
        PM_TaskComment comment = new PM_TaskComment();
        comment.setTaskId(taskId);
        comment.setUserId(userId);
        comment.setContent(request.getContent().trim());
        comment.setParentCommentId(request.getParentCommentId());
        comment.setReplyPreview(replyPreview);
        comment.setReplyToUserId(replyToUserId);

        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);

        PM_TaskComment savedComment = taskCommentRepository.save(comment);

        log.info(Constant.LOG_COMMENT_CREATED, savedComment.getCommentId(), taskId);

        // Publish Event
        publishTaskUpdateEvent(taskId, projectId, userId, TaskUpdateEvent.ACTION_COMMENT_ADD);

        // 6. Build response
        Map<String, Object> data = new HashMap<>();
        data.put("comment_id", savedComment.getCommentId());
        data.put("content", savedComment.getContent());
        data.put("created_at", savedComment.getCreatedAt());

        if (replyPreview != null) {
            Map<String, Object> replyInfo = new HashMap<>();
            replyInfo.put("reply_preview", replyPreview);
            data.put("reply_info", replyInfo);
        }

        return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.COMMENT_CREATED_SUCCESS, data);
    }

    @Override
    @Transactional
    public BaseResponse<?> updateComment(Long userId, Long commentId, UpdateCommentRequest request) {
        log.info(Constant.LOG_UPDATING_COMMENT, commentId, userId);

        // 1. Fetch comment
        PM_TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(Constant.ERROR_COMMENT_NOT_FOUND));

        // 2. Auth: Only creator can edit
        if (!comment.getUserId().equals(userId)) {
            throw new ForbiddenException(Constant.ERROR_COMMENT_EDIT_FORBIDDEN);
        }

        // 3. Validate content
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return new BaseResponse<>(Constant.ERROR_STATUS, Constant.ERROR_COMMENT_CONTENT_REQUIRED, null);
        }

        // 4. Update comment
        comment.setContent(request.getContent().trim());
        comment.setUpdatedAt(LocalDateTime.now());
        taskCommentRepository.save(comment);

        log.info(Constant.LOG_COMMENT_UPDATED, commentId);

        // Publish Event
        // Need to fetch projectId since updateComment doesn't have it explicitly yet
        Long projectId = taskCommentRepository.findProjectIdByTaskId(comment.getTaskId()).orElse(null);
        if (projectId != null) {
            publishTaskUpdateEvent(comment.getTaskId(), projectId, userId, TaskUpdateEvent.ACTION_COMMENT_UPDATE);
        }

        return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.COMMENT_UPDATED_SUCCESS, null);
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteComment(Long userId, Long commentId) {
        log.info(Constant.LOG_DELETING_COMMENT, commentId, userId);

        // 1. Fetch comment
        PM_TaskComment comment = taskCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(Constant.ERROR_COMMENT_NOT_FOUND));

        // 2. Get project ID for owner check
        Long projectId = taskCommentRepository.findProjectIdByTaskId(comment.getTaskId())
                .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

        // 3. Auth: Creator OR Project Owner can delete
        boolean isCreator = comment.getUserId().equals(userId);
        boolean isOwner = authHelper.isOwner(projectId, userId);

        if (!isCreator && !isOwner) {
            throw new ForbiddenException(Constant.ERROR_COMMENT_DELETE_FORBIDDEN);
        }

        // 4. Update reply_preview for all child comments
        taskCommentRepository.updateReplyPreviewByParentId(commentId, Constant.DELETED_COMMENT_PREVIEW);

        // 5. Delete comment
        taskCommentRepository.delete(comment);
        log.info(Constant.LOG_COMMENT_DELETED, commentId);

        // Publish Event
        publishTaskUpdateEvent(comment.getTaskId(), projectId, userId, TaskUpdateEvent.ACTION_COMMENT_DELETE);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.COMMENT_DELETED_SUCCESS, null);
    }

    /**
     * Truncate content to create a preview for replies
     * Max 50 characters + "..." suffix
     */
    private String truncateForPreview(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= REPLY_PREVIEW_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, REPLY_PREVIEW_MAX_LENGTH) + "...";
    }

    private void publishTaskUpdateEvent(Long taskId, Long projectId, Long userId, String action) {
        try {
            PM_Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new NotFoundException(Constant.ERROR_TASK_NOT_FOUND));

            Set<Long> assigneeIds = task.getAssignees().stream()
                    .map(PM_TaskAssignee::getUserId)
                    .collect(Collectors.toSet());

            TaskUpdateEvent event = new TaskUpdateEvent(
                    taskId,
                    projectId,
                    userId,
                    assigneeIds,
                    action);

            kafkaTemplate.send(KafkaConfig.TOPIC_PROJECT_TASK_UPDATE, event);
            log.info("Published TaskUpdateEvent for task {} with action {}", taskId, action);
        } catch (Exception ex) {
            log.error("Failed to publish TaskUpdateEvent for task {}", taskId, ex);
        }
    }
}

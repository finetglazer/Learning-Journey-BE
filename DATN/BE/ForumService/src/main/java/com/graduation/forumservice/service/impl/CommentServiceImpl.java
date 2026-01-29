package com.graduation.forumservice.service.impl;

import com.graduation.forumservice.config.KafkaConfig;
import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.event.ForumActivityEvent;
import com.graduation.forumservice.exception.NotFoundException;
import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.request.CreateCommentRequest;
import com.graduation.forumservice.payload.request.UpdateCommentRequest;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.*;
import com.graduation.forumservice.service.CommentService;
import com.graduation.forumservice.service.helper.ForumDTOMapper;
import com.graduation.forumservice.service.helper.UserInfoResolverService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final ForumCommentRepository forumCommentRepository;
    private final ForumPostRepository forumPostRepository;
    private final ForumAnswerRepository forumAnswerRepository;
    private final UserInfoResolverService userInfoResolverService;
    private final ForumDTOMapper forumDTOMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public BaseResponse<?> getComments(String targetType, Long targetId, int page, int limit) {
        log.info("Fetching SQL-based comments: type={}, id={}, page={}, limit={}", targetType, targetId, page, limit);

        Pageable pageable = PageRequest.of(page - 1, limit);
        List<ForumComment> rawComments;

        if ("POST".equalsIgnoreCase(targetType)) {
            rawComments = forumCommentRepository.findAllByPostIdOrderByCreatedAtAsc(targetId, pageable);
        } else if ("ANSWER".equalsIgnoreCase(targetType)) {
            rawComments = forumCommentRepository.findAllByAnswerIdOrderByCreatedAtAsc(targetId, pageable);
        } else {
            return new BaseResponse<>(Constant.ERROR_STATUS, "Invalid target type", null);
        }

        boolean hasMore = rawComments.size() > limit;
        if (hasMore) {
            rawComments = rawComments.subList(0, limit);
        }

        List<CommentDTO> commentDTOs = rawComments.stream().map(forumDTOMapper::toCommentDTO).toList();

        PaginationDTO pagination = new PaginationDTO(page, hasMore);
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comments retrieved successfully",
                Map.of("comments", commentDTOs, "pagination", pagination));
    }

    @Override
    @Transactional
    public BaseResponse<?> addComment(Long userId, CreateCommentRequest request) {
        log.info("Adding new comment: userId={}, targetType={}, targetId={}",
                userId, request.getTargetType(), request.getTargetId());

        try {
            Long postId = null;
            Long answerId = null;
            Long parentCommentId = null;

            Long recipientId = null;
            String postTitle = "Unknown Post";
            ForumActivityEvent.ForumEventType eventType = null;

            if ("POST".equalsIgnoreCase(request.getTargetType())) {
                ForumPost post = forumPostRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Target post not found!"));

                postId = request.getTargetId();
                recipientId = post.getUserId();
                postTitle = post.getTitle();
                eventType = ForumActivityEvent.ForumEventType.COMMENT_ON_POST;

            } else if ("ANSWER".equalsIgnoreCase(request.getTargetType())) {
                ForumAnswer answer = forumAnswerRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Target answer not found!"));

                answerId = request.getTargetId();
                postId = answer.getPostId();
                recipientId = answer.getUserId();
                eventType = ForumActivityEvent.ForumEventType.COMMENT_ON_ANSWER;

                postTitle = forumPostRepository.findById(postId)
                        .map(ForumPost::getTitle)
                        .orElse("Unknown Post");

            } else if ("COMMENT".equalsIgnoreCase(request.getTargetType())) {
                ForumComment targetComment = forumCommentRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Target comment not found!"));

                parentCommentId = targetComment.getCommentId();
                postId = targetComment.getPostId();
                answerId = targetComment.getAnswerId();
                recipientId = targetComment.getUserId();
                eventType = ForumActivityEvent.ForumEventType.REPLY_ON_COMMENT;

                if (postId != null) {
                    postTitle = forumPostRepository.findById(postId)
                            .map(ForumPost::getTitle)
                            .orElse("Unknown Post");
                }
            }

            String previewSnapshot = null;
            if (parentCommentId != null) {
                previewSnapshot = forumCommentRepository.findById(parentCommentId).map(parent -> {
                    String text = parent.getContentText();
                    return (text != null && text.length() > 50) ? text.substring(0, 47) + "..." : text;
                }).orElse(null);
            }

            ForumComment comment = ForumComment.builder()
                    .userId(userId)
                    .postId(postId)
                    .answerId(answerId)
                    .parentCommentId(parentCommentId)
                    .contentText(request.getContent())
                    .replyPreviewSnapshot(previewSnapshot)
                    .build();

            comment = forumCommentRepository.save(comment);

            if (recipientId != null && !recipientId.equals(userId)) {
                PostAuthorDTO actor = userInfoResolverService.fetchAuthorInfo(userId);

                ForumActivityEvent event = new ForumActivityEvent(
                        userId,
                        actor.getName(),
                        actor.getAvatar(),
                        recipientId,
                        postId,
                        postTitle,
                        answerId,
                        comment.getCommentId(),
                        eventType);
                sendKafkaNotification(event);
            }

            log.info("Successfully saved comment with ID: {}", comment.getCommentId());
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment added successfully",
                    forumDTOMapper.toCommentDTO(comment));

        } catch (Exception e) {
            log.error("Failed to add comment: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> editComment(Long userId, Long commentId, UpdateCommentRequest request) {
        log.info("Editing comment: commentId={}, userId={}", commentId, userId);

        try {
            ForumComment comment = forumCommentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found!"));

            if (!comment.getUserId().equals(userId)) {
                return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to edit this comment",
                        null);
            }

            comment.setContentText(request.getContent());
            comment.setUpdatedAt(LocalDateTime.now());

            ForumComment updatedComment = forumCommentRepository.save(comment);

            log.info("Successfully updated comment ID: {}", commentId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment updated successfully",
                    forumDTOMapper.toCommentDTO(updatedComment));

        } catch (NotFoundException e) {
            log.error("Edit failed: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Unexpected error during comment edit: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteComment(Long userId, Long commentId) {
        log.info("Attempting to delete comment: commentId={}, requestedBy={}", commentId, userId);

        ForumComment comment = forumCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found!"));

        if (!comment.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt: userId {} tried to delete comment {}", userId, commentId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to delete this comment", null);
        }

        try {
            Long deletedReplies = forumCommentRepository.deleteAllByParentCommentId(commentId);
            log.debug("Deleted {} child replies for comment {}", deletedReplies, commentId);

            forumCommentRepository.delete(comment);

            log.info("Successfully deleted comment {}", commentId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment and its replies deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete comment {}: {}", commentId, e.getMessage());
            throw e;
        }
    }

    // =========== Private Helper Methods ===========

    private void sendKafkaNotification(ForumActivityEvent event) {
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_FORUM_ACTIVITY, event);
            log.info("Sent forum activity Kafka event: {} by user {}", event.getType(), event.getActorId());
        } catch (Exception e) {
            log.error("Failed to send Kafka notification: {}", e.getMessage());
        }
    }
}

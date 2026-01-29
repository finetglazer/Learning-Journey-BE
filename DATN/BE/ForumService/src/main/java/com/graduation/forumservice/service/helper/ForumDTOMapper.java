package com.graduation.forumservice.service.helper;

import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.MongoContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Centralized DTO mapper for Forum entities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForumDTOMapper {

    private final UserInfoResolverService userInfoResolverService;
    private final MongoContentRepository mongoContentRepository;

    /**
     * Maps raw SQL result array to PostFeedDTO.
     */
    public PostFeedDTO toPostFeedDTO(Object[] row) {
        // Safe handling for Tags (Index 7)
        Object tagsRaw = row[7];
        List<String> tags = Collections.emptyList();
        if (tagsRaw instanceof String tagsStr && !tagsStr.isEmpty()) {
            tags = Arrays.asList(tagsStr.split(","));
        }

        // Safe handling for Status (Index 6)
        PostStatus status = PostStatus.ACTIVE;
        Object statusRaw = row[6];
        if (statusRaw instanceof Number num) {
            status = PostStatus.values()[num.intValue()];
        } else if (statusRaw instanceof String str) {
            try {
                status = PostStatus.valueOf(str);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown status string: {}", str);
            }
        } else if (statusRaw instanceof java.sql.Timestamp) {
            log.error("CRITICAL: Index 6 is a Timestamp. Your SQL query columns are out of order!");
        }

        // Safe handling for CreatedAt (Index 8)
        LocalDateTime createdAt = LocalDateTime.now();
        if (row[8] instanceof java.sql.Timestamp ts) {
            createdAt = ts.toLocalDateTime();
        }

        // Resolve Author (cached)
        Long userId = ((Number) row[1]).longValue();
        PostAuthorDTO author = userInfoResolverService.fetchAuthorInfo(userId);

        return PostFeedDTO.builder()
                .postId(((Number) row[0]).longValue())
                .userId(userId)
                .title(String.valueOf(row[2]))
                .preview(String.valueOf(row[3]))
                .isSolved((Boolean) row[5])
                .status(status)
                .tags(tags)
                .authorId(author.getUserId())
                .authorName(author.getName())
                .authorAvatar(author.getAvatar())
                .createdAt(createdAt)
                .score(((Number) row[9]).intValue())
                .viewCount(((Number) row[10]).longValue())
                .answerCount(((Number) row[11]).intValue())
                .build();
    }

    /**
     * Maps ForumPost entity to PostFeedDTO.
     */
    public PostFeedDTO toPostFeedDTO(ForumPost post, PostAuthorDTO author, Integer score, Long views, Integer answers) {
        return PostFeedDTO.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .authorId(author.getUserId())
                .authorName(author.getName())
                .authorAvatar(author.getAvatar())
                .title(post.getTitle())
                .preview(post.getPlainTextPreview())
                .isSolved(post.getIsSolved())
                .status(post.getStatus())
                .tags(post.getTags())
                .createdAt(post.getCreatedAt())
                .score(score)
                .viewCount(views)
                .answerCount(answers)
                .build();
    }

    /**
     * Maps raw SQL result array to AnswerDTO.
     */
    public AnswerDTO toAnswerDTO(Object[] row) {
        Long answerId = ((Number) row[0]).longValue();
        Long userId = ((Number) row[1]).longValue();
        int mongoContentId = Integer.parseInt(String.valueOf(row[3]));
        Boolean isAccepted = (Boolean) row[6];

        MongoContent mongoContent = mongoContentRepository.findByIntId(mongoContentId)
                .orElse(new MongoContent());

        return AnswerDTO.builder()
                .answerId(answerId)
                .content(mongoContent.getContent())
                .author(userInfoResolverService.fetchAuthorInfo(userId))
                .score(((Number) row[9]).intValue())
                .isAccepted(isAccepted)
                .createdAt(((java.sql.Timestamp) row[7]).toLocalDateTime())
                .updatedAt(row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime() : null)
                .build();
    }

    /**
     * Maps ForumComment entity to CommentDTO.
     */
    public CommentDTO toCommentDTO(ForumComment comment) {
        log.debug("Mapping ForumComment entity to DTO: commentId={}", comment.getCommentId());

        return CommentDTO.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContentText())
                .author(userInfoResolverService.fetchAuthorInfo(comment.getUserId()))
                .parentCommentId(comment.getParentCommentId())
                .replyPreview(comment.getReplyPreviewSnapshot())
                .postId(comment.getPostId())
                .answerId(comment.getAnswerId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Maps ForumPostFile entity to FileUploadDTO.
     */
    public FileUploadDTO mapToFileDTO(ForumPostFile file) {
        return FileUploadDTO.builder()
                .fileId(file.getFileId())
                .url(file.getStorageRef())
                .name(file.getFileName())
                .extension(file.getExtension())
                .size(file.getFileSize())
                .build();
    }
}

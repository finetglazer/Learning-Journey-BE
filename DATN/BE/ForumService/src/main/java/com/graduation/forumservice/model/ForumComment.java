package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment'
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(name = "content_text", columnDefinition = "text", nullable = false)
    private String contentText;

    @Column(name = "reply_preview_snapshot", length = 255)
    private String replyPreviewSnapshot;

    @CreationTimestamp // Matches CURRENT_TIMESTAMP in image_616148.png
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
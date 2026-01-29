package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment'
    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "plain_text_preview", length = 700)
    private String plainTextPreview;

    @Column(name = "mongo_content_id")
    private Integer mongoContentId;

    @Column(name = "is_accepted")
    private Boolean isAccepted = false;

    @Column(name = "upvote_count")
    private Integer upvoteCount = 0;

    @Column(name = "downvote_count")
    private Integer downvoteCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private AnswerStatus status = AnswerStatus.ACTIVE;

    /**
     * Calculates the net score of the answer.
     * net score = upvotes - downvotes
     */
    @Transient
    public Integer getScore() {
        int up = (upvoteCount == null) ? 0 : upvoteCount;
        int down = (downvoteCount == null) ? 0 : downvoteCount;
        return up - down;
    }

    public enum AnswerStatus {
        ACTIVE,
        HIDDEN,
    }
}
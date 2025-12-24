package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "answer_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment' in image_610b4c.png
    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "answer_id", nullable = false)
    private Long answerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Type: 1 for Upvote, -1 for Downvote.
     */
    @Column(name = "type", nullable = false)
    private Integer type;

    @CreationTimestamp // Matches 'CURRENT_TIMESTAMP'
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
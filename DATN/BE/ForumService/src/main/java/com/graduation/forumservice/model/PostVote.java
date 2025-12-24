package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_votes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment' in image_61e0a8.png
    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Type: 1 for Upvote, -1 for Downvote.
     */
    @Column(name = "type", nullable = false)
    private Integer type;

    @CreationTimestamp // Matches 'CURRENT_TIMESTAMP' default in image_61e0a8.png
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
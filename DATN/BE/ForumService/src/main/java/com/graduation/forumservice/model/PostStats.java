package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "post_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostStats {

    @Id
    @Column(name = "post_id")
    private Long postId;

    /**
     * Community consensus score (Upvotes - Downvotes).
     */
    @Column(name = "score")
    private Integer score = 0; // Matches default 0 in image_617ba9.png

    /**
     * Total number of times the post has been viewed.
     */
    @Column(name = "view_count")
    private Long viewCount = 0L; // Matches bigint default 0 in image_617ba9.png

    /**
     * Total count of top-level answers for this post.
     */
    @Column(name = "answer_count")
    private Integer answerCount = 0; // Matches integer default 0 in image_617ba9.png

    // Optional: Reference back to the main Post if you need bidirectional navigation
    @OneToOne
    @MapsId // Ensures post_id is used as the PK here
    @JoinColumn(name = "post_id")
    private ForumPost post;
}
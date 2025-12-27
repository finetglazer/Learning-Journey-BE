package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "forum_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment'
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 1000)
    private String title;

    @Column(name = "plain_text_preview", length = 700)
    private String plainTextPreview;

    @Column(name = "mongo_content_id")
    private Integer mongoContentId;

    @Column(name = "is_solved")
    private Boolean isSolved = false;

    @Column(columnDefinition = "varchar default 'ACTIVE'")
    private PostStatus status = PostStatus.ACTIVE; // Matches 'ACTIVE'::character varying

    // TSVector requires a custom mapping or can be handled as a String for JPA
    // but usually updated via DB triggers or native queries
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.ARRAY) // Matches 'text[]'
    @Column(name = "tags", columnDefinition = "text[]")
    private List<String> tags;
}
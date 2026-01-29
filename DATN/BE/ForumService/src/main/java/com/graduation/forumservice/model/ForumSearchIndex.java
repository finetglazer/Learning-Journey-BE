package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forum_search_index")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumSearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Long searchId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "answer_id")
    private Long answerId;

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private SearchDocType docType;

    @Column(name = "title")
    private String title;

    // IMPORTANT: 'columnDefinition = "text"' ensures PostgreSQL creates this
    // as a TEXT column (unlimited), not VARCHAR(255).
    @Column(name = "content_data", columnDefinition = "text")
    private String contentData;

    // This column is managed by the database/native queries, not standard JPA
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    public enum SearchDocType {
        POST,
        ANSWER
    }
}
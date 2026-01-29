package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumSearchIndex;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ForumSearchIndexRepository extends JpaRepository<ForumSearchIndex, Long> {

    void deleteByPostId(Long postId);
    void deleteByAnswerId(Long answerId);

    @Modifying
    @Transactional
    @Query(value = """
        UPDATE forum_search_index 
        SET search_vector = 
            setweight(to_tsvector('english', coalesce(:title, '')), 'A') || 
            setweight(to_tsvector('english', coalesce(:body, '')), 'B')
        WHERE search_id = :searchId
    """, nativeQuery = true)
    void updateSearchVector(Long searchId, String title, String body);
}
package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumAnswer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumAnswerRepository extends JpaRepository<ForumAnswer, Long> {

    @Query(value = """
        SELECT a.*, s.score
        FROM forum_answers a
        JOIN answer_stats s ON a.answer_id = s.answer_id
        WHERE a.post_id = :postId
        ORDER BY\s
            CASE WHEN :sort = 'MOST_HELPFUL' THEN s.score END DESC,
            CASE WHEN :sort = 'NEWEST' THEN a.created_at END DESC
       \s""", nativeQuery = true)
    List<Object[]> findAnswersByPostIdNative(
            @Param("postId") Long postId,
            @Param("sort") String sort,
            Pageable pageable);
}
package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, Long> {
    void deleteAllByAnswerId(Long answerId);

    /**
     * Fetches post comments sorted by creation date (Oldest first).
     * Used for sequential conversation display.
     */
    List<ForumComment> findAllByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    /**
     * Fetches answer comments sorted by creation date (Oldest first).
     */
    List<ForumComment> findAllByAnswerIdOrderByCreatedAtAsc(Long answerId, Pageable pageable);

    Long deleteAllByParentCommentId(Long parentCommentId);

    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long answerId, Pageable limits);

    List<ForumComment> findByAnswerIdOrderByCreatedAtAsc(Long answerId, Pageable pageable);
}

package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.PostStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PostStatsRepository extends JpaRepository<PostStats, Long> {
    /**
     * Increment view count atomically in the database.
     * Prevents race conditions compared to fetching, incrementing, and saving.
     */
    @Modifying
    @Transactional
    @Query("UPDATE PostStats s SET s.viewCount = s.viewCount + 1 WHERE s.postId = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    PostStats findByPostId(Long postId);

    @Query("SELECT s.answerCount FROM PostStats s WHERE s.postId = :postId")
    Integer findAnswerCountByPostId(@Param("postId") Long postId);

    /**
     * Atomically updates the score of a post.
     * Use this to avoid race conditions during concurrent voting.
     * * @param postId The ID of the post
     * @param change The amount to add (e.g., +1, -1, +2, -2)
     */
    @Modifying
    @Transactional
    @Query("UPDATE PostStats s SET s.score = s.score + :change WHERE s.postId = :postId")
    void updateScore(@Param("postId") Long postId, @Param("change") int change);
}

package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumAnswer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ForumAnswerRepository extends JpaRepository<ForumAnswer, Long> {

    /**
     * Fetches answers for a post and calculates the score on-the-fly for sorting.
     * Index 0: ForumAnswer entity
     * Index 1: Integer (calculated score)
     */
    @Query(value = """
    SELECT\s
        a.answer_id, a.user_id, a.post_id, a.mongo_content_id,\s
        a.upvote_count, a.downvote_count, a.is_accepted,\s
        a.created_at, a.updated_at,
        (a.upvote_count - a.downvote_count) as calculated_score
    FROM forum_answers a
    WHERE a.post_id = :postId
    ORDER BY\s
        CASE WHEN :sort = 'MOST_HELPFUL' THEN (a.upvote_count - a.downvote_count) END DESC,
        CASE WHEN :sort = 'NEWEST' THEN a.created_at END DESC
   \s""", nativeQuery = true)
    List<Object[]> findAnswersByPostIdNative(
            @Param("postId") Long postId,
            @Param("sort") String sort,
            Pageable pageable);

    /**
     * Resets the isAccepted flag for all answers of a specific post.
     * Use this before marking a new answer as 'Accepted' to ensure
     * only one solution exists per post.
     */
    @Modifying
    @Transactional
    @Query("UPDATE ForumAnswer a SET a.isAccepted = false " +
            "WHERE a.postId = :postId AND a.isAccepted = true")
    void unmarkAcceptedAnswersForPost(@Param("postId") Long postId);

    /**
     * Atomically updates vote counts for an answer.
     * Prevents race conditions during high-frequency voting.
     * * @param answerId The ID of the answer to update
     * @param upChange The amount to add to upvotes (e.g., +1, -1)
     * @param downChange The amount to add to downvotes (e.g., +1, -1)
     */
    @Modifying
    @Transactional
    @Query("UPDATE ForumAnswer a " +
            "SET a.upvoteCount = a.upvoteCount + :upChange, " +
            "    a.downvoteCount = a.downvoteCount + :downChange " +
            "WHERE a.answerId = :answerId")
    void updateAnswerVoteCounts(
            @Param("answerId") Long answerId,
            @Param("upChange") int upChange,
            @Param("downChange") int downChange);
}
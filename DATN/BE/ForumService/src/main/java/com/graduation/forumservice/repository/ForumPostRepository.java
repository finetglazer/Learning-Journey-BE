package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, Long> {

    /**
     * Fetches the forum feed by joining content with statistics.
     * Uses the limit + 1 strategy (handled by the passed Pageable).
     */
    @Query(value = """
        SELECT p.*, s.score, s.view_count, s.answer_count
        FROM forum_posts p
        JOIN post_stats s ON p.post_id = s.post_id
        WHERE (
            :filter = 'ALL'\s
            OR (:filter = 'MY_POSTS' AND p.user_id = :userId)
            OR (:filter = 'SAVED_POSTS' AND p.post_id IN (
                SELECT sp.post_id FROM saved_posts sp WHERE sp.user_id = :userId
            ))
        )
        AND (:search IS NULL OR p.search_vector @@ plainto_tsquery('english', :search))
        ORDER BY\s
            CASE WHEN :sort = 'NEWEST' THEN p.created_at END DESC,
            CASE WHEN :sort = 'HELPFUL' THEN s.score END DESC,
            CASE WHEN :sort = 'RELEVANT' THEN ts_rank(p.search_vector, plainto_tsquery('english', :search)) END DESC
       \s""",
            nativeQuery = true)
    List<Object[]> findFeedPostsNative(
            @Param("userId") Long userId,
            @Param("filter") String filter,
            @Param("sort") String sort,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Retrieves a post and its associated statistics.
     * Index 0: ForumPost entity
     * Index 1: Integer (score)
     * Index 2: Long (viewCount)
     */
    @Query("SELECT p, s.score, s.viewCount " +
            "FROM ForumPost p " +
            "JOIN PostStats s ON p.postId = s.postId " +
            "WHERE p.postId = :postId")
    Optional<Object[]> findPostDetailById(@Param("postId") Long postId);

}
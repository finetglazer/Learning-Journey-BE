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

    @Query(value = """
    SELECT
        p.post_id, 
        p.user_id, 
        p.title, 
        p.plain_text_preview, 
        p.mongo_content_id, 
        p.is_solved, 
        p.status, 
        array_to_string(p.tags, ',') as tags_str, 
        p.created_at, 
        s.score, 
        s.view_count, 
        s.answer_count,
        -- Calculate Max Rank (Highest match between Post and its best Answer)
        MAX(ts_rank(fsi.search_vector, websearch_to_tsquery('english', :search))) as rank_score
    FROM forum_posts p
    JOIN post_stats s ON p.post_id = s.post_id
    
    -- JOIN Search Index for BOTH the Post and its Answers
    -- We join on post_id, so we get rows for the Post itself AND its Answers
    LEFT JOIN forum_search_index fsi ON p.post_id = fsi.post_id 
    
    WHERE (
        (:filter = 'ALL')
        OR (:filter = 'MY_POSTS' AND p.user_id = :userId)
        OR (:filter = 'SAVED_POSTS' AND p.post_id IN (
            SELECT sp.post_id FROM saved_posts sp WHERE sp.user_id = :userId
        ))
        OR (:filter = 'MOST_HELPFUL' AND s.score > 0)
    )
    AND p.status = 'ACTIVE'
    
    -- CHECK IF MATCH EXISTS (In either Post or Answer)
    AND (:search IS NULL OR :search = '' OR fsi.search_vector @@ websearch_to_tsquery('english', :search))
    
    -- Group by Post to merge multiple answer matches into one Post result
    GROUP BY p.post_id, s.score, s.view_count, s.answer_count
    
    ORDER BY 
        -- Sort by the highest rank found (whether in title, body, or answer)
        CASE 
            WHEN :search IS NOT NULL AND :search != '' 
            THEN MAX(ts_rank(fsi.search_vector, websearch_to_tsquery('english', :search)))
            ELSE 0 
        END DESC,
        p.created_at DESC
    """,
            nativeQuery = true)
    List<Object[]> findFeedPostsNative(
            @Param("userId") Long userId,
            @Param("filter") String filter,
            @Param("search") String search,
            Pageable pageable);
    /**
     * Retrieves a post and its associated statistics.
     * Index 0: ForumPost entity
     * Index 1: Integer (score)
     * Index 2: Long (viewCount)
     */
    @Query(value = """
    SELECT\s
        p.post_id, p.user_id, p.title, p.plain_text_preview, p.mongo_content_id,\s
        p.is_solved, p.created_at, p.updated_at,\s
        array_to_string(p.tags, ',') as tags_str,
        s.score, s.view_count, s.answer_count
    FROM forum_posts p
    JOIN post_stats s ON p.post_id = s.post_id
    WHERE p.post_id = :postId
   \s""", nativeQuery = true)
    Optional<Object[]> findPostDetailByIdNative(@Param("postId") Long postId);
}
package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Long> {
    Optional<PostVote> findPostVoteByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT COUNT(v) FROM PostVote v WHERE v.postId = :postId AND v.type = 1")
    long countUpvotes(@Param("postId") Long postId);

    @Query("SELECT COUNT(v) FROM PostVote v WHERE v.postId = :postId AND v.type = -1")
    long countDownvotes(@Param("postId") Long postId);

    void deleteByPostId(Long postId);
}

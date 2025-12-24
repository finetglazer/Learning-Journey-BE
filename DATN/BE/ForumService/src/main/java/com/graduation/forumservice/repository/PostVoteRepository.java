package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.PostVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Long> {
    Optional<PostVote> findPostVoteByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);
}

package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.SavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);

    void deleteAllByPostId(Long postId);

    Optional<SavedPost> findByPostIdAndUserId(Long postId, Long userId);
}

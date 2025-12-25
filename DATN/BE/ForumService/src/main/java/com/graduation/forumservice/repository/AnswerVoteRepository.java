package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.AnswerVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnswerVoteRepository extends JpaRepository<AnswerVote, Long> {
    void deleteByAnswerId(Long answerId);

    Optional<AnswerVote> findByAnswerIdAndUserId(Long answerId, Long userId);
}

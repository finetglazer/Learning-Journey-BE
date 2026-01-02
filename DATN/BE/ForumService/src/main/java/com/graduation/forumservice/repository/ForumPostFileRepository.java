package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumPostFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumPostFileRepository extends JpaRepository<ForumPostFile, Long> {
    List<ForumPostFile> findAllByPostId(Long postId);
}

package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ForumTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ForumTagRepository extends JpaRepository<ForumTag, Long> {
    boolean existsByName(String name);

    Collection<Object> findByNameContainingIgnoreCase(String name);
}

package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.ProjectSavedPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectSavedPostRepository extends JpaRepository<ProjectSavedPost, Long> {
    /**
     * Deletes all entries across all projects that reference a specific post.
     * Uses the 'id.postId' path to access the field inside the @EmbeddedId.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ProjectSavedPost p WHERE p.id.postId = :postId")
    void deleteAllBookmarksForPost(@Param("postId") Long postId);

    /**
     * Finds a specific bookmark for a post within a project.
     * Accesses the 'projectId' and 'postId' inside the @EmbeddedId 'id'.
     */
    @Query("SELECT p FROM ProjectSavedPost p WHERE p.id.projectId = :projectId AND p.id.postId = :postId")
    Optional<ProjectSavedPost> findByProjectIdAndPostId(@Param("projectId") Long projectId,
            @Param("postId") Long postId);

    @Query("SELECT p FROM ProjectSavedPost p WHERE p.id.postId = :postId")
    List<ProjectSavedPost> findByPostId(@Param("postId") Long postId);

    /**
     * Retrieves all mapping records for a specific project.
     * This is used to populate the "Shared Resources" tab in the Project Manager.
     */
    @Query("SELECT p FROM ProjectSavedPost p WHERE p.id.projectId = :projectId")
    List<ProjectSavedPost> findByProjectId(@Param("projectId") Long projectId);
}

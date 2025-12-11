package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskCommentRepository extends JpaRepository<PM_TaskComment, Long> {

    /**
     * Find all comments for a task, ordered by creation time (oldest first)
     */
    List<PM_TaskComment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    /**
     * Find comment by ID with task validation
     */
    Optional<PM_TaskComment> findByCommentIdAndTaskId(Long commentId, Long taskId);

    /**
     * Check if comment exists and belongs to specific user
     */
    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * Get the project ID for a comment (for authorization)
     */
    @Query("SELECT d.projectId FROM PM_TaskComment c " +
            "JOIN PM_Task t ON c.taskId = t.taskId " +
            "JOIN PM_Phase p ON t.phaseId = p.phaseId " +
            "JOIN PM_Deliverable d ON p.deliverableId = d.deliverableId " +
            "WHERE c.commentId = :commentId")
    Optional<Long> findProjectIdByCommentId(@Param("commentId") Long commentId);

    /**
     * Get the project ID for a task (for authorization)
     */
    @Query("SELECT d.projectId FROM PM_Task t " +
            "JOIN PM_Phase p ON t.phaseId = p.phaseId " +
            "JOIN PM_Deliverable d ON p.deliverableId = d.deliverableId " +
            "WHERE t.taskId = :taskId")
    Optional<Long> findProjectIdByTaskId(@Param("taskId") Long taskId);

    @Modifying
    @Query("UPDATE PM_TaskComment c SET c.replyPreview = :newPreview WHERE c.parentCommentId = :parentId")
    void updateReplyPreviewByParentId(@Param("parentId") Long parentId, @Param("newPreview") String newPreview);
}

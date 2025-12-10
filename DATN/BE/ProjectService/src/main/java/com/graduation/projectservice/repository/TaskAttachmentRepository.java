package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_TaskAttachment;
import com.graduation.projectservice.model.PM_TaskAttachmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<PM_TaskAttachment, PM_TaskAttachmentId> {

    /**
     * Check if attachment already exists
     */
    boolean existsById(PM_TaskAttachmentId id);

    /**
     * Find all attachments for a task
     */
    List<PM_TaskAttachment> findByTaskId(Long taskId);

    /**
     * Count attachments for a task (dynamic count instead of counter field)
     */
    @Query("SELECT COUNT(a) FROM PM_TaskAttachment a WHERE a.taskId = :taskId")
    Integer countByTaskId(@Param("taskId") Long taskId);

    /**
     * Delete all attachments for a task (useful when deleting a task)
     */
    void deleteAllByTaskId(Long taskId);
}

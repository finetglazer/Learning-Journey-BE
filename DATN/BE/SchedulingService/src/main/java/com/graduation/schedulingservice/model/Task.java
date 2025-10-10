package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.ItemType;
//import com.graduation.schedulingservice.model.enums.TaskPriority;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@DiscriminatorValue("TASK")
@EqualsAndHashCode(callSuper = true)
public class Task extends CalendarItem {

    private Long parentBigTaskId;
    private Integer estimatedHours;
    private Integer actualHours;
    private LocalDate dueDate;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subtask> subtasks = new ArrayList<>();

    @PostLoad
    private void postLoad() {
        setType(ItemType.TASK);
    }

    @PrePersist
    private void prePersist() {
        setType(ItemType.TASK);
    }

    public void addSubtask(Subtask subtask) {
        subtasks.add(subtask);
        subtask.setParentTask(this);
    }

    public void removeSubtask(Subtask subtask) {
        subtasks.remove(subtask);
        subtask.setParentTask(null);
    }

    public int getCompletionPercentage() {
        if (subtasks.isEmpty()) {
            return 0;
        }
        long completed = subtasks.stream().filter(Subtask::getIsComplete).count();
        return (int) ((completed * 100) / subtasks.size());
    }
}
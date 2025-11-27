package com.graduation.schedulingservice.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@DiscriminatorValue("PROJECT_WORK")
@EqualsAndHashCode(callSuper = true)
public class ProjectTask extends CalendarItem {
    private Long pmTaskId;
}
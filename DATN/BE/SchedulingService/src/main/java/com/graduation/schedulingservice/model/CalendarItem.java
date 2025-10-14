package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.ItemStatus;
import com.graduation.schedulingservice.model.enums.ItemType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "calendar_items")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type", discriminatorType = DiscriminatorType.STRING)
public abstract class CalendarItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long calendarId;

    private Long weekPlanId;

    private Long monthPlanId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Embedded
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemStatus status = ItemStatus.INCOMPLETE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isScheduled() {
        return timeSlot != null && timeSlot.getStartTime() != null;
    }

    public void reschedule(TimeSlot newTimeSlot) {
        this.timeSlot = newTimeSlot;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDetails(String name, String note) {
        this.name = name;
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }
}
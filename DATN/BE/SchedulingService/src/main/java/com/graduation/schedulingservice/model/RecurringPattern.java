package com.graduation.schedulingservice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "recurring_patterns")
public class RecurringPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "pattern_days_of_week", joinColumns = @JoinColumn(name = "pattern_id"))
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private List<DayOfWeek> daysOfWeek = new ArrayList<>();

}
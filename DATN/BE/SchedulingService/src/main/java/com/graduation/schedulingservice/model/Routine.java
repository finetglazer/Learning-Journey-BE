package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.ItemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@DiscriminatorValue("ROUTINE")
@EqualsAndHashCode(callSuper = true)
public class Routine extends CalendarItem {

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "recurring_pattern_id")
    private RecurringPattern pattern;

    @ElementCollection
    @CollectionTable(name = "routine_exceptions", joinColumns = @JoinColumn(name = "routine_id"))
    @Column(name = "exception_date")
    private List<LocalDateTime> exceptions = new ArrayList<>();

    private LocalDateTime endDate;

    @PostLoad
    private void postLoad() {
        setType(ItemType.ROUTINE);
    }

    @PrePersist
    private void prePersist() {
        setType(ItemType.ROUTINE);
    }

    public void addException(LocalDateTime date) {
        if (!exceptions.contains(date)) {
            exceptions.add(date);
        }
    }

    public void removeException(LocalDateTime date) {
        exceptions.remove(date);
    }
}
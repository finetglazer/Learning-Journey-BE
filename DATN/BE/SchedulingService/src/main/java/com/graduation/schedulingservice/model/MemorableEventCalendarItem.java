package com.graduation.schedulingservice.model;

import com.graduation.schedulingservice.model.enums.ItemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@DiscriminatorValue("MEMORABLE_EVENT")
@EqualsAndHashCode(callSuper = true)
public class MemorableEventCalendarItem extends CalendarItem {

    @PostLoad
    private void postLoad() {
        setType(ItemType.MEMORABLE_EVENT);
    }

    @PrePersist
    private void prePersist() {
        setType(ItemType.MEMORABLE_EVENT);
    }
}
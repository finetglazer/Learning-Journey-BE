package com.graduation.schedulingservice.model;

//import com.graduation.schedulingservice.model.enums.EventType;
import com.graduation.schedulingservice.model.enums.ItemType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@DiscriminatorValue("EVENT")
@EqualsAndHashCode(callSuper = true)
public class Event extends CalendarItem {


    @PostLoad
    private void postLoad() {
        setType(ItemType.EVENT);
    }

    @PrePersist
    private void prePersist() {
        setType(ItemType.EVENT);
    }

}
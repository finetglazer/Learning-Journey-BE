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

    private String location;

    @ElementCollection
    @CollectionTable(name = "event_attendees", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "attendee_email")
    private List<String> attendees = new ArrayList<>();

    private Boolean isAllDay = false;

    @PostLoad
    private void postLoad() {
        setType(ItemType.EVENT);
    }

    @PrePersist
    private void prePersist() {
        setType(ItemType.EVENT);
    }

    public void addAttendee(String email) {
        if (!attendees.contains(email)) {
            attendees.add(email);
        }
    }

    public void removeAttendee(String email) {
        attendees.remove(email);
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
package com.graduation.schedulingservice.payload.request;

import lombok.Data;

import java.util.List;

@Data
public class EventDetailsDTO {
    private String location;
    private List<String> attendees;
    private Boolean isAllDay;
}
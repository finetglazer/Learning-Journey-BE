package com.graduation.userservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InvitationSentEvent extends ApplicationEvent {

    private final String email;
    private final String displayName;
    private final String projectName;
    private final String token;

    public InvitationSentEvent(Object source, String email, String displayName,
                               String projectName, String token) {
        super(source);
        this.email = email;
        this.displayName = displayName;
        this.projectName = projectName;
        this.token = token;
    }
}
package com.graduation.userservice.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InvitationSentEvent extends ApplicationEvent {

    private final String email;
    private final String displayName;
    private final String projectName;
    private final String token;
    private final Long projectId;

    public InvitationSentEvent(Object source, String email, String displayName,
                               String projectName, String token, Long projectId) {
        super(source);
        this.email = email;
        this.displayName = displayName;
        this.projectName = projectName;
        this.token = token;
        this.projectId = projectId;
    }
}
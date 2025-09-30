package com.graduation.userservice.event; // Or your preferred event package

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PasswordResetRequestedEvent extends ApplicationEvent {
    private final String email;
    private final String resetToken;

    public PasswordResetRequestedEvent(Object source, String email, String resetToken) {
        super(source);
        this.email = email;
        this.resetToken = resetToken;
    }
}
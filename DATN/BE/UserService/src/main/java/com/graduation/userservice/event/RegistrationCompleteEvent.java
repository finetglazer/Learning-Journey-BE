// Create a new file: RegistrationCompleteEvent.java
package com.graduation.userservice.event; // Or your preferred package

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RegistrationCompleteEvent extends ApplicationEvent {
    private final String email;
    private final String verificationCode;

    public RegistrationCompleteEvent(Object source, String email, String verificationCode) {
        super(source);
        this.email = email;
        this.verificationCode = verificationCode;
    }
}
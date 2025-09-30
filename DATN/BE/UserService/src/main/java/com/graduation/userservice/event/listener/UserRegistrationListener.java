// Create a new file: UserRegistrationListener.java
package com.graduation.userservice.event.listener; // Or your preferred package

import com.graduation.userservice.event.RegistrationCompleteEvent;
import com.graduation.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegistrationListener {

    private final EmailService emailService;

    @Async // This makes the method run in a background thread
    @EventListener
    public void handleRegistrationCompleteEvent(RegistrationCompleteEvent event) {
        log.info("Listener received registration event for email: {}", event.getEmail());
        try {
            emailService.sendVerificationEmail(event.getEmail(), event.getVerificationCode());
        } catch (Exception e) {
            // The @Async method's exception won't be caught by the publisher.
            // Logging is crucial here to track failures.
            log.error("Failed to send verification email asynchronously for: {}", event.getEmail(), e);
        }
    }
}

package com.graduation.userservice.event.listener; // Or your listener package

import com.graduation.userservice.event.PasswordResetRequestedEvent;
import com.graduation.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetListener {
    private final EmailService emailService;

    @Async
    @EventListener
    public void handlePasswordResetRequestedEvent(PasswordResetRequestedEvent event) {
        log.info("Listener received password reset event for email: {}", event.getEmail());
        try {
            emailService.sendPasswordResetEmail(event.getEmail(), event.getResetToken());
        } catch (Exception e) {
            log.error("Failed to send password reset email asynchronously for: {}", event.getEmail(), e);
        }
    }
}


package com.graduation.userservice.event.listener;

import com.graduation.userservice.event.InvitationSentEvent;
import com.graduation.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvitationSentListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handleInvitationSent(InvitationSentEvent event) {
        try {
            emailService.sendInvitationEmail(
                    event.getEmail(),
                    event.getDisplayName(),
                    event.getProjectName(),
                    event.getToken(),
                    event.getProjectId()
            );
            log.info("Invitation email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Failed to send invitation email to: {}", event.getEmail(), e);
            // Consider adding retry logic or dead letter queue here
        }
    }
}

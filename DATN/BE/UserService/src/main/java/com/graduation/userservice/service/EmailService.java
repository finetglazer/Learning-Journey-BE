package com.graduation.userservice.service;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send email verification email to user
     * @param email recipient email address
     * @param verificationCode verification code to include in email
     * @throws Exception if email sending fails
     */
    void sendVerificationEmail(String email, String verificationCode) throws Exception;

    /**
     * Send password reset email to user
     * @param email recipient email address
     * @param resetToken password reset token to include in email
     * @throws Exception if email sending fails
     */
    void sendPasswordResetEmail(String email, String resetToken) throws Exception;

    /**
     * Send welcome email to newly registered user
     * @param email recipient email address
     * @param displayName user's display name
     * @throws Exception if email sending fails
     */
    void sendWelcomeEmail(String email, String displayName) throws Exception;

    /**
     * Send password change confirmation email
     * @param email recipient email address
     * @param displayName user's display name
     * @throws Exception if email sending fails
     */
    void sendPasswordChangedEmail(String email, String displayName) throws Exception;

    /**
     * Send account locked notification email
     * @param email recipient email address
     * @param displayName user's display name
     * @throws Exception if email sending fails
     */
    void sendAccountLockedEmail(String email, String displayName) throws Exception;

    void sendInvitationEmail(String email, String displayName, String projectName, String token) throws Exception;
}
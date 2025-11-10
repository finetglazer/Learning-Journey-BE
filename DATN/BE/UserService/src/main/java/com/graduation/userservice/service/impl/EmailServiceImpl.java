package com.graduation.userservice.service.impl;

import com.graduation.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:noreply@graduation.com}")
    private String fromEmail;

    @Value("${app.mail.base-url:http://localhost:3000}")
    private String baseUrl;

    @Value("${app.name:Graduation App}")
    private String appName;

    @Override
    public void sendVerificationEmail(String email, String verificationCode) throws Exception {
        log.info("Sending verification email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Verify Your Email Address");

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("verificationUrl", baseUrl + "/auth/verify?token=" + verificationCode);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/verification", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for verification email");
                htmlContent = createVerificationEmailFallback(verificationCode);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new Exception("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String email, String resetToken) throws Exception {
        log.info("Sending password reset email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Reset Your Password");

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetUrl", baseUrl + "/auth/forgot-password/reset-password?token=" + resetToken);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/password-reset", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for password reset email");
                htmlContent = createPasswordResetEmailFallback(resetToken);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new Exception("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendWelcomeEmail(String email, String displayName) throws Exception {
        log.info("Sending welcome email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Welcome to " + appName + "!");

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("displayName", displayName);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/welcome", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for welcome email");
                htmlContent = createWelcomeEmailFallback(displayName);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", email, e);
            throw new Exception("Failed to send welcome email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordChangedEmail(String email, String displayName) throws Exception {
        log.info("Sending password changed email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Password Changed Successfully");

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("displayName", displayName);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/password-changed", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for password changed email");
                htmlContent = createPasswordChangedEmailFallback(displayName);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password changed email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send password changed email to: {}", email, e);
            throw new Exception("Failed to send password changed email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendAccountLockedEmail(String email, String displayName) throws Exception {
        log.info("Sending account locked email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Account Security Alert");

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("displayName", displayName);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/account-locked", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for account locked email");
                htmlContent = createAccountLockedEmailFallback(displayName);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account locked email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", email, e);
            throw new Exception("Failed to send account locked email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendInvitationEmail(String email, String displayName, String projectName, String token) throws Exception {
        log.info("Sending project invitation email to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("You've been invited to join " + projectName);

            // Create Thymeleaf context
            Context context = new Context(Locale.getDefault());
            context.setVariable("displayName", displayName);
            context.setVariable("projectName", projectName);
            context.setVariable("acceptUrl", baseUrl + "/projects/accept-invitation?token=" + token);
            context.setVariable("declineUrl", baseUrl + "/projects/decline-invitation?token=" + token);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);

            // Generate HTML content from template (fallback to simple HTML if template not found)
            String htmlContent;
            try {
                htmlContent = templateEngine.process("email/project-invitation", context);
            } catch (Exception e) {
                log.warn("Template not found, using fallback HTML for invitation email");
                htmlContent = createInvitationEmailFallback(displayName, projectName, token);
            }

            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invitation email sent successfully to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send invitation email to: {}", email, e);
            throw new Exception("Failed to send invitation email: " + e.getMessage(), e);
        }
    }


    // Fallback HTML templates when Thymeleaf templates are not available

    private String createVerificationEmailFallback(String verificationCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Email Verification</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Verify Your Email Address</h2>
                    <p>Thank you for registering with %s! To complete your registration, please verify your email address.</p>
                    <p>Your verification code is: <strong style="font-size: 18px; color: #e74c3c;">%s</strong></p>
                    <p>Or click the link below:</p>
                    <a href="%s/auth/verify?token=%s" style="background-color: #3498db; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Verify Email</a>
                    <p style="margin-top: 20px; font-size: 12px; color: #7f8c8d;">
                        This verification link will expire in 24 hours. If you didn't create an account, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """, appName, verificationCode, baseUrl, verificationCode);
    }

    private String createPasswordResetEmailFallback(String resetToken) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Reset</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Reset Your Password</h2>
                    <p>We received a request to reset your password for your %s account.</p>
                    <p>Click the link below to reset your password:</p>
                    <a href="%s/auth/reset-password?token=%s" style="background-color: #e74c3c; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Reset Password</a>
                    <p style="margin-top: 20px; font-size: 12px; color: #7f8c8d;">
                        This password reset link will expire in 1 hour. If you didn't request this reset, please ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """, appName, baseUrl, resetToken);
    }

    private String createWelcomeEmailFallback(String displayName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Welcome to %s!</h2>
                    <p>Hi %s,</p>
                    <p>Welcome to %s! Your account has been successfully created and verified.</p>
                    <p>You can now start using all our features. If you have any questions, feel free to contact our support team.</p>
                    <a href="%s" style="background-color: #27ae60; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">Get Started</a>
                    <p style="margin-top: 20px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>
            </body>
            </html>
            """, appName, displayName, appName, baseUrl, appName);
    }

    private String createPasswordChangedEmailFallback(String displayName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Password Changed</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Password Changed Successfully</h2>
                    <p>Hi %s,</p>
                    <p>Your password has been successfully changed for your %s account.</p>
                    <p>If you did not make this change, please contact our support team immediately.</p>
                    <p style="margin-top: 20px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>
            </body>
            </html>
            """, displayName, appName, appName);
    }

    private String createAccountLockedEmailFallback(String displayName) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Account Security Alert</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #e74c3c;">Account Security Alert</h2>
                    <p>Hi %s,</p>
                    <p>Your %s account has been temporarily locked due to security reasons.</p>
                    <p>Please contact our support team to unlock your account.</p>
                    <p style="margin-top: 20px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>
            </body>
            </html>
            """, displayName, appName, appName);
    }

    private String createInvitationEmailFallback(String displayName, String projectName, String token) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Project Invitation</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">You've been invited!</h2>
                    <p>Hi %s,</p>
                    <p>You've been invited to join the project <strong>%s</strong> on %s.</p>
                    <p>Click the button below to accept the invitation and start collaborating:</p>
                    <a href="%s/projects/accept-invitation?token=%s" style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0;">Accept Invitation</a>
                    <p style="margin-top: 20px; font-size: 12px; color: #7f8c8d;">
                        This invitation link will expire in 7 days. If you didn't expect this invitation, you can safely ignore this email.
                    </p>
                    <p>
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>
            </body>
            </html>
            """, displayName, projectName, appName, baseUrl, token, appName);
    }
}
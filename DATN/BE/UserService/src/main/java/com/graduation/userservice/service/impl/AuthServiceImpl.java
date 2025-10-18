package com.graduation.userservice.service.impl;

import com.graduation.userservice.client.SchedulingServiceClient;
import com.graduation.userservice.event.PasswordResetRequestedEvent;
import com.graduation.userservice.event.RegistrationCompleteEvent;
import com.graduation.userservice.payload.request.ChangePasswordRequest;
import com.graduation.userservice.payload.response.BaseResponse;
import com.graduation.userservice.constant.Constant;
import com.graduation.userservice.model.*;
import com.graduation.userservice.payload.request.LoginRequest;
import com.graduation.userservice.payload.request.RegisterRequest;
import com.graduation.userservice.payload.response.LoginResponse;
import com.graduation.userservice.repository.*;
//import com.graduation.userservice.security.ForgotPasswordRateLimiterService;
//import com.graduation.userservice.security.JwtProvider;
//import com.graduation.userservice.security.TokenBlacklistService;
import com.graduation.userservice.security.JwtProvider;
import com.graduation.userservice.service.AuthService;
import com.graduation.userservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final OAuthProviderRepository oAuthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtProvider jwtProvider;
    private final SchedulingServiceClient schedulingServiceClient;
//    private final TokenBlacklistService blacklistService;
//    private final ForgotPasswordRateLimiterService rateLimiterService;

    @Value("${app.verification-token-expiration:24}")
    private int tokenExpirationHours;

    @Value("${app.session-expiration:720}")
    private int sessionExpirationHours;

    @Value("${app.password-reset-expiration:1}")
    private int passwordResetExpirationHours;

    @Override
    @Transactional
    public BaseResponse<?> register(RegisterRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return new BaseResponse<>(0, Constant.MSG_EMAIL_ALREADY_EXISTS, null);
            }

            if (userRepository.existsByDisplayName(request.getDisplayName())) {
                return new BaseResponse<>(0, Constant.MSG_DISPLAY_NAME_EXISTS, null);
            }

            User newUser = new User();
            newUser.setEmail(request.getEmail());
            newUser.setDisplayName(request.getDisplayName());
            newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            newUser.setIsActive(false);

            User savedUser = userRepository.save(newUser);

            EmailVerification verification = EmailVerification.createForExistingUser(
                    savedUser.getId(),
                    tokenExpirationHours
            );
            emailVerificationRepository.save(verification);

            eventPublisher.publishEvent(new RegistrationCompleteEvent(
                    this,
                    savedUser.getEmail(),
                    verification.getVerificationCode()
            ));

            logger.info(Constant.LOG_AUTH_REGISTRATION_SUCCESS, request.getEmail());

            return new BaseResponse<>(1, Constant.MSG_REGISTRATION_SUCCESS, null);

        } catch (Exception e) {
            logger.error(Constant.LOG_AUTH_REGISTRATION_FAILED, request.getEmail(), e);
            return new BaseResponse<>(0, Constant.MSG_REGISTRATION_FAILED + request.getEmail(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> verifyUser(String verificationCode) {
        try {
            Optional<EmailVerification> verificationOpt =
                    emailVerificationRepository.findByVerificationCodeAndIsUsedFalse(verificationCode);

            if (verificationOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_VERIFICATION_NOT_FOUND, null);
            }

            EmailVerification verification = verificationOpt.get();

            if (verification.isExpired()) {
                userRepository.deleteById(verification.getUserId());
                emailVerificationRepository.delete(verification);
                return new BaseResponse<>(0, Constant.MSG_VERIFICATION_EXPIRED_REGISTER_AGAIN, null);
            }

            Optional<User> userOpt = userRepository.findById(verification.getUserId());
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            User user = userOpt.get();
            user.activate();
            userRepository.save(user);

            verification.markAsUsed();
            emailVerificationRepository.save(verification);

            // ADD THIS: Create default calendar after activation
            try {
                schedulingServiceClient.createDefaultCalendar(user.getId());
            } catch (Exception e) {
                logger.warn("Failed to create default calendar for user {}, but verification succeeded",
                        user.getId(), e);
                // Don't fail the verification if calendar creation fails
            }

            logger.info(Constant.LOG_AUTH_ACCOUNT_ACTIVATED, user.getEmail());
            return new BaseResponse<>(1, Constant.MSG_VERIFICATION_SUCCESS, null);

        } catch (Exception e) {
            logger.error(Constant.LOG_AUTH_VERIFICATION_FAILED, verificationCode, e);
            return new BaseResponse<>(0, Constant.MSG_VERIFICATION_FAILED + verificationCode, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> login(LoginRequest request) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_INVALID_CREDENTIALS, null);
            }

            User user = userOpt.get();

            if (!user.getIsActive()) {
                return new BaseResponse<>(0, Constant.MSG_ACCOUNT_NOT_VERIFIED_LOGIN, null);
            }

            if (!user.hasPassword() || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return new BaseResponse<>(0, Constant.MSG_INVALID_CREDENTIALS, null);
            }

            user.updateLastLogin();
            userRepository.save(user);

            // Generate BOTH tokens
            String accessToken = jwtProvider.generateAccessToken(user);
            String refreshToken = jwtProvider.generateRefreshToken(user);

            // Store refresh token in UserSession
            UserSession session = UserSession.create(user.getId(), refreshToken);
            userSessionRepository.save(session);

            // Build response with both tokens
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName()
            );
            LoginResponse loginResponse = new LoginResponse(accessToken, refreshToken, userInfo);

            logger.info(Constant.LOG_AUTH_LOGIN_SUCCESS, user.getEmail());
            return new BaseResponse<>(1, Constant.MSG_LOGIN_SUCCESS, loginResponse);

        } catch (Exception e) {
            logger.error(Constant.LOG_AUTH_LOGIN_FAILED, request.getEmail(), e);
            return new BaseResponse<>(0, Constant.MSG_LOGIN_FAILED + request.getEmail(), null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!jwtProvider.validateToken(refreshToken)) {
                return new BaseResponse<>(0, "Invalid or expired refresh token", null);
            }

            // Check token type
            String tokenType = jwtProvider.getTokenType(refreshToken);
            if (!"refresh".equals(tokenType)) {
                return new BaseResponse<>(0, "Invalid token type", null);
            }

            // Find active session
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionIdAndIsActiveTrue(refreshToken);
            if (sessionOpt.isEmpty()) {
                return new BaseResponse<>(0, "Session not found or expired", null);
            }

            UserSession session = sessionOpt.get();

            // Get user
            Optional<User> userOpt = userRepository.findById(session.getUserId());
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(0, "User not found", null);
            }

            User user = userOpt.get();

            // Generate new access token
            String newAccessToken = jwtProvider.generateAccessToken(user);

            // Update session last accessed time
            session.refresh();
            userSessionRepository.save(session);

            logger.info("Token refreshed for user: {}", user.getEmail());
            return new BaseResponse<>(1, "Token refreshed successfully", newAccessToken);

        } catch (Exception e) {
            logger.error("Token refresh failed", e);
            return new BaseResponse<>(0, "Token refresh failed", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> logout(String token) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionIdAndIsActiveTrue(token);

            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                session.invalidate();
                userSessionRepository.save(session);
                logger.info(Constant.LOG_AUTH_SESSION_INVALIDATED, token.substring(token.length() - 6));
            } else {
                logger.warn(Constant.LOG_AUTH_LOGOUT_NO_SESSION, token.substring(token.length() - 6));
            }

            return new BaseResponse<>(1, Constant.MSG_LOGOUT_SUCCESS, null);

        } catch (Exception e) {
            logger.error(Constant.LOG_AUTH_LOGOUT_ERROR, e);
            return new BaseResponse<>(0, Constant.MSG_LOGOUT_SERVER_FAIL, null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> changePassword(ChangePasswordRequest request, Principal principal, String currentSessionToken) {
        try {
            // Get user from principal
            Optional<User> userOpt = userRepository.findByEmail(principal.getName());
            if (userOpt.isEmpty()) {
                // This should not happen if the user is authenticated
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }
            User user = userOpt.get();

            // Pre-condition check: User has a password (not OAuth-only)
            if (!user.hasPassword()) {
                return new BaseResponse<>(0, "Account does not have a password set.", null);
            }

            // 3a. Current password incorrect
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                return new BaseResponse<>(0, Constant.MSG_CURRENT_PASSWORD_INCORRECT, null);
            }

            // 5a. New password and confirmation don't match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return new BaseResponse<>(0, Constant.MSG_PASSWORD_MISMATCH, null);
            }



            // Update password
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Post-Condition: Invalidate all other user sessions
            userSessionRepository.invalidateAllOtherUserSessions(user.getId(), currentSessionToken);

            logger.info(Constant.LOG_AUTH_PASSWORD_CHANGED, user.getEmail());
            return new BaseResponse<>(1, Constant.MSG_PASSWORD_CHANGE_SUCCESS, null);

        } catch (Exception e) {
            logger.error(Constant.LOG_AUTH_PASSWORD_CHANGE_FAILED, principal.getName(), e);
            return new BaseResponse<>(0, "An unexpected error occurred while changing the password.", null);
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> forgotPassword(String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);

            // **MODIFICATION**: Explicitly check if the user exists.
            if (userOpt.isEmpty()) {
                logger.warn("Password reset requested for a non-existent account: {}", email);
                // Return a specific error message if the email is not found.
                return new BaseResponse<>(0, Constant.MSG_EMAIL_NOT_FOUND, null);
            }

            User user = userOpt.get();

            // **MODIFICATION**: Explicitly check if the user has a password.
            if (!user.hasPassword()) {
                logger.warn("Password reset attempted for an account without a password (OAuth-only): {}", email);
                // Return a specific error for accounts that cannot reset passwords.
                return new BaseResponse<>(0, Constant.MSG_NO_PASSWORD_SET, null);
            }

            // not active user
            if (!user.getIsActive()) {
                logger.warn("Password reset attempted for an inactive account: {}", email);
                return new BaseResponse<>(0, Constant.MSG_ACCOUNT_NOT_VERIFIED_LOGIN, null);
            }

            // --- The rest of the logic remains the same ---

            // Invalidate any existing, unused tokens for this user.
            passwordResetTokenRepository.invalidateAllUserTokens(user.getId());

            // Create and save a new password reset token.
            PasswordResetToken resetToken = PasswordResetToken.generate(
                    user.getId(),
                    passwordResetExpirationHours
            );
            passwordResetTokenRepository.save(resetToken);

            // Publish an event to send the email asynchronously.
            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    this,
                    user.getEmail(),
                    resetToken.getResetToken()
            ));

            // Return the generic success message only when a token is actually generated.
            return new BaseResponse<>(1, Constant.MSG_PASSWORD_RESET_SENT, null);

        } catch (Exception e) {
            logger.error("An unexpected error occurred during forgotPassword for email: {}", email, e);
            return new BaseResponse<>(0, "An unexpected error occurred.", null);
        }
    }


    @Override
    @Transactional
    public BaseResponse<?> resetPassword(String token, String newPassword, String confirmPassword) {
        try {
            // 10a. New password and confirmation don't match
            if (!newPassword.equals(confirmPassword)) {
                return new BaseResponse<>(0, Constant.MSG_PASSWORD_MISMATCH, null);
            }

            // 7 & 7a. Find and validate the reset token
            Optional<PasswordResetToken> tokenOpt =
                    passwordResetTokenRepository.findByResetTokenAndIsUsedFalse(token);

            if (tokenOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_PASSWORD_RESET_INVALID, null);
            }

            PasswordResetToken resetToken = tokenOpt.get();

            // 8. Check if the token is valid (not expired or already used)
            if (!resetToken.validate()) {
                // The token is expired, delete it to keep the database clean
                passwordResetTokenRepository.delete(resetToken);
                return new BaseResponse<>(0, Constant.MSG_PASSWORD_RESET_INVALID, null);
            }

            // 9. Find the associated user and update the password
            Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
            if (userOpt.isEmpty()) {
                return new BaseResponse<>(0, Constant.MSG_USER_NOT_FOUND, null);
            }

            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            // 11. Invalidate the reset token
            resetToken.invalidate();
            passwordResetTokenRepository.save(resetToken);

            // 12. Invalidate all active user sessions for security
            userSessionRepository.invalidateAllUserSessions(user.getId());

            logger.info("Password reset successfully for user: {}", user.getEmail());
            // 13. Return success message
            return new BaseResponse<>(1, Constant.MSG_PASSWORD_RESET_SUCCESS, null);

        } catch (Exception e) {
            logger.error("Password reset failed for token: {}", token, e);
            return new BaseResponse<>(0, "An error occurred during password reset.", null);
        }
    }
}
package com.graduation.userservice.security; // <-- Make sure the package is correct

import com.graduation.userservice.model.OAuthProvider;
import com.graduation.userservice.model.User;
import com.graduation.userservice.model.UserSession;
import com.graduation.userservice.repository.OAuthProviderRepository;
import com.graduation.userservice.repository.UserRepository;
import com.graduation.userservice.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component; // <-- This annotation is crucial
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component // <-- This tells Spring to create a bean of this class
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuthProviderRepository oAuthProviderRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getName();

        log.info("OAuth2 login success for email: {}", email);

        User user = findOrCreateUser(email, name);
        linkOAuthProvider(user, providerId, email);

        String token = jwtProvider.generateToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        UserSession session = UserSession.create(user.getId(), token);
        userSessionRepository.save(session);
        log.info("Created new session for user {} via OAuth2", user.getId());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/auth/login-success")
                .queryParam("token", token)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", user.getId())
                .queryParam("displayName", user.getDisplayName())
                .queryParam("email", user.getEmail())
                .encode(StandardCharsets.UTF_8)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User findOrCreateUser(String email, String displayName) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.updateLastLogin();
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setDisplayName(displayName);
                    newUser.setIsActive(true);
                    newUser.updateLastLogin();
                    log.info("Creating new user with email: {}", email);
                    return userRepository.save(newUser);
                });
    }

    private void linkOAuthProvider(User user, String providerId, String email) {
        if (!oAuthProviderRepository.existsByUserIdAndProviderAndIsActiveTrue(user.getId(), OAuthProvider.ProviderType.GOOGLE)) {
            OAuthProvider provider = OAuthProvider.linkAccount(
                    user.getId(),
                    OAuthProvider.ProviderType.GOOGLE,
                    providerId,
                    email
            );
            oAuthProviderRepository.save(provider);
            log.info("Linked GOOGLE provider for user {}", user.getEmail());
        }
    }
}

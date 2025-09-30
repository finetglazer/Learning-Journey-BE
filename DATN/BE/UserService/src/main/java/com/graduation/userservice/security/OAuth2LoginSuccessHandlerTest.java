
//package com.graduation.userservice.security;
//
//import com.graduation.userservice.model.OAuthProvider;
//import com.graduation.userservice.repository.OAuthProviderRepository;
//import com.graduation.userservice.repository.UserRepository;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collections;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@Transactional // Ensures database changes are rolled back after each test
//class OAuth2LoginSuccessHandlerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private OAuthProviderRepository oAuthProviderRepository;
//
//    @Test
//    void whenOAuth2LoginSuccess_shouldCreateNewUserAndProviderLink() throws Exception {
//        // 1. Define the mock user attributes, just like Google would provide
//        Map<String, Object> attributes = Map.of(
//                "sub", "123456789012345", // This is the 'providerId'
//                "name", "Test User",
//                "email", "test.user@example.com"
//        );
//
//        // 2. Create a mock OAuth2User
//        DefaultOAuth2User mockUser = new DefaultOAuth2User(
//                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
//                attributes,
//                "email" // The attribute to use as the principal's name
//        );
//
//        // 3. Check that the user does not exist before the test
//        assertFalse(userRepository.findByEmail("test.user@example.com").isPresent());
//
//        // 4. Perform a request to a secured endpoint.
//        // The .with(oauth2Login()...) simulates a successful OAuth2 authentication.
//        // Spring will then trigger our OAuth2LoginSuccessHandler.
//        mockMvc.perform(get("/any-secured-endpoint") // This endpoint doesn't even need to exist
//                        .with(oauth2Login().oauth2User(mockUser).clientRegistrationId("google")))
//                .andExpect(status().is3xxRedirection()) // Expect a redirect
//                .andExpect(redirectedUrlPattern("http://localhost:3000/login-success*")); // Expect redirect to frontend
//
//        // 5. Assert that the user and provider link were created in the database
//        Optional<com.graduation.userservice.model.User> createdUserOpt = userRepository.findByEmail("test.user@example.com");
//        assertTrue(createdUserOpt.isPresent(), "User should have been created");
//
//        com.graduation.userservice.model.User createdUser = createdUserOpt.get();
//        assertEquals("Test User", createdUser.getDisplayName());
//
//        boolean providerLinkExists = oAuthProviderRepository.existsByUserIdAndProviderAndIsActiveTrue(createdUser.getId(), OAuthProvider.ProviderType.GOOGLE);
//        assertTrue(providerLinkExists, "OAuth provider link should have been created");
//    }
//}

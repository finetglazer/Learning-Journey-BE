package com.graduation.forumservice.service.helper;

import com.graduation.forumservice.client.UserServiceClient;
import com.graduation.forumservice.model.UserInfoCache;
import com.graduation.forumservice.payload.response.PostAuthorDTO;
import com.graduation.forumservice.repository.UserInfoCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for resolving user information with caching strategy.
 * Uses a tiered approach:
 * 1. Check local UserInfoCache (PostgresSQL).
 * 2. If missing, call User Service and update the local cache.
 * 3. Fallback to placeholder if User Service also fails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoResolverService {

    private final UserInfoCacheRepository userInfoCacheRepository;
    private final UserServiceClient userServiceClient;

    /**
     * Resolves a userId into a PostAuthorDTO using a tiered caching approach.
     */
    public PostAuthorDTO fetchAuthorInfo(Long userId) {
        log.debug("Resolving author info for userId: {}", userId);

        // 1. Try to find in local cache first
        return userInfoCacheRepository.findById(userId).map(cache -> {
            log.debug("Cache hit for userId: {}", userId);
            return PostAuthorDTO.builder()
                    .userId(cache.getUserId())
                    .name(cache.getDisplayName())
                    .email(cache.getEmail())
                    .avatar(cache.getAvatarUrl()).build();
        })
                // 2. Cache miss: Call Remote User Service
                .orElseGet(() -> {
                    log.info("Cache miss for userId: {}. Fetching from User Service.", userId);

                    return userServiceClient.getUserById(userId).map(user -> {
                        // Update local cache for next time
                        UserInfoCache newCache = new UserInfoCache(
                                user.getUserId(),
                                user.getName(),
                                user.getAvatarUrl(),
                                user.getEmail());
                        userInfoCacheRepository.save(newCache);

                        return PostAuthorDTO.builder()
                                .userId(user.getUserId())
                                .name(user.getName())
                                .avatar(user.getAvatarUrl())
                                .email(user.getEmail())
                                .build();
                    })
                            // 3. Ultimate Fallback: User doesn't exist in system
                            .orElseGet(() -> {
                                log.warn("User ID {} not found in User Service. Returning placeholder.", userId);
                                return PostAuthorDTO.builder().userId(userId).name("Unknown User").avatar(null).build();
                            });
                });
    }
}

package com.graduation.notificationservice.repository;

import com.graduation.notificationservice.model.UserInfoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for UserInfoCache entity.
 * Used to cache user display information for notification senders.
 */
@Repository
public interface UserInfoCacheRepository extends JpaRepository<UserInfoCache, Long> {
}

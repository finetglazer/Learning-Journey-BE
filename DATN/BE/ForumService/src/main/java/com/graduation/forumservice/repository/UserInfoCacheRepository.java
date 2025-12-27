package com.graduation.forumservice.repository;

import com.graduation.forumservice.model.UserInfoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoCacheRepository extends JpaRepository<UserInfoCache,Long> {
}

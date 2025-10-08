package com.graduation.userservice.repository;

import com.graduation.userservice.model.UserConstraints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserConstraintsRepository extends JpaRepository<UserConstraints, Long> {
    Optional<UserConstraints> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
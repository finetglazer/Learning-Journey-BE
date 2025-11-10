package com.graduation.schedulingservice.repository;

import com.graduation.schedulingservice.model.MemorableEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemorableEventRepository extends JpaRepository<MemorableEvent, Long> {

    List<MemorableEvent> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
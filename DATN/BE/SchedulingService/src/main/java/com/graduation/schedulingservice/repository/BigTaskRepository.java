package com.graduation.schedulingservice.repository;// BigTaskRepository.java (Corrected)

import com.graduation.schedulingservice.model.BigTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BigTaskRepository extends JpaRepository<BigTask, Long> {

    // Use findByMonthPlan_Id to traverse the relationship
    // This tells JPA: "Find by the 'id' property of the 'monthPlan' field"
    List<BigTask> findByMonthPlan_Id(Long monthPlanId);
}
package com.graduation.projectservice.repository;

import com.graduation.projectservice.model.PM_RiskAssignee;
import com.graduation.projectservice.model.RiskAssigneeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiskAssigneeRepository extends JpaRepository<PM_RiskAssignee, RiskAssigneeId> {
    void deleteAllByRiskId(Long riskId);
}
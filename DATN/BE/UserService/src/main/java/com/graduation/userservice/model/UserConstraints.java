package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = Constant.TABLE_USER_CONSTRAINTS,
        indexes = {
                @Index(name = "idx_user_constraints_user_id", columnList = "userId")
        })
public class UserConstraints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @ElementCollection
    @CollectionTable(
            name = "user_sleep_hours",
            joinColumns = @JoinColumn(name = "user_constraints_id")
    )
    private List<TimeRange> sleepHours = new ArrayList<>();


    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(
            name = "user_daily_limits",
            joinColumns = @JoinColumn(name = "user_constraints_id")
    )
    @MapKeyColumn(name = "item_type")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "hours_limit")
    private Map<String, Integer> dailyLimits = new HashMap<>();

    // ADDED: A single boolean to enable/disable the entire feature
    @Column(name = "daily_limit_feature_enabled", nullable = false)
    private Boolean dailyLimitFeatureEnabled = false;

    // REMOVED: The old map for enabling/disabling individual limits
    // @ElementCollection ... private Map<String, Boolean> dailyLimitEnabled = new HashMap<>();

    public static UserConstraints createDefault(Long userId) {
        UserConstraints constraints = new UserConstraints();
        constraints.setUserId(userId);
        constraints.setSleepHours(new ArrayList<>());
        constraints.setDailyLimitFeatureEnabled(false); // CHANGED: Initialize the new field
        constraints.setUpdatedAt(LocalDateTime.now());
        return constraints;
    }

    public void updateSleepHours(List<TimeRange> newSleepHours) {
        this.sleepHours.clear();
        if (newSleepHours != null) {
            this.sleepHours.addAll(newSleepHours);
        }
        this.updatedAt = LocalDateTime.now();
    }

    // CHANGED: Method signature no longer needs 'enabled' per item
    public void updateDailyLimit(String itemType, Integer hours) {
        if (hours != null) {
            this.dailyLimits.put(itemType, hours);
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void removeDailyLimit(String itemType) {
        this.dailyLimits.remove(itemType);
        // REMOVED: No longer need to remove from the enabled map
        // this.dailyLimitEnabled.remove(itemType);
        this.updatedAt = LocalDateTime.now();
    }
}
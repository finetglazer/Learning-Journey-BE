package com.graduation.userservice.model;

import com.graduation.userservice.constant.Constant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "allow_overlapping", nullable = false)
    private Boolean allowOverlapping = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static UserConstraints createDefault(Long userId) {
        UserConstraints constraints = new UserConstraints();
        constraints.setUserId(userId);
        constraints.setSleepHours(new ArrayList<>());
        constraints.setAllowOverlapping(false);
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
}
package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Matches 'auto increment' in image_6177ca.png
    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @CreationTimestamp // Matches 'CURRENT_TIMESTAMP' default in image_6177ca.png
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
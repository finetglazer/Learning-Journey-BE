package com.graduation.forumservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProjectSavedPostId implements Serializable {

    @Column(name = "project_id")
    private Long projectId; // Matches bigint primary key

    @Column(name = "post_id")
    private Long postId;    // Matches bigint primary key
}
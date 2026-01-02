package com.graduation.forumservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_post_files")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "storage_ref")
    private String storageRef;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "extension")
    private String extension;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
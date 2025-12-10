package com.graduation.documentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentThread {

    private String id;

    private Long authorId;

    private String authorName;

    private String content;

    private Boolean resolved;

    private Long resolvedBy;

    private String resolvedByName;

    private LocalDateTime resolvedAt;

    private String resolvedReason; // "MANUAL" or "ORPHANED"

    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentReply> replies = new ArrayList<>();
}

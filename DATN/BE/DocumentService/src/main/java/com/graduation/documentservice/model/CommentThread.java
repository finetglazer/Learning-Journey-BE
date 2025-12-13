package com.graduation.documentservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("threadId")
    private String threadId;

    // ✅ FIX 1: Map frontend 'userId' to backend 'authorId'
    @JsonProperty("userId")
    private Long authorId;

    // ✅ FIX 2: Map frontend 'userName' to backend 'authorName'
    @JsonProperty("userName")
    private String authorName;

    // ✅ FIX 3: Add Avatar field and map it
    @JsonProperty("userAvatar")
    private String authorAvatar;

    private String content;

    private Boolean resolved;

    private Long resolvedBy;

    private String resolvedByName;

    private LocalDateTime resolvedAt;

    private String resolvedReason;

    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentReply> replies = new ArrayList<>();
}
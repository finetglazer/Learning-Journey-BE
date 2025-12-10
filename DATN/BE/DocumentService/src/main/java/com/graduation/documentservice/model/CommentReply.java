package com.graduation.documentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentReply {

    private String id;

    private Long authorId;

    private String authorName;

    private String content;

    private LocalDateTime createdAt;
}

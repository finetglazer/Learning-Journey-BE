package com.graduation.projectservice.payload.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentDTO {

    private Long commentId;

    private Long userId;

    private String userName;

    private String userAvatar;

    private String content;

    private Boolean isEdited;

    private LocalDateTime createdAt;

    private ReplyInfoDTO replyInfo;
}

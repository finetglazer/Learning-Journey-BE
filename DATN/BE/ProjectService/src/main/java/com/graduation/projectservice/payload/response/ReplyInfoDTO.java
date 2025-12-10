package com.graduation.projectservice.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyInfoDTO {
    private Long replyToUserId;
    private String replyPreview;
}

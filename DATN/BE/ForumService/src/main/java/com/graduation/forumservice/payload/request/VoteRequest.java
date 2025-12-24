package com.graduation.forumservice.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VoteRequest {

    /**
     * The type of vote:
     * 1 = Upvote
     * -1 = Downvote
     */
    @NotNull(message = "Vote type is required")
    private Integer voteType;
}
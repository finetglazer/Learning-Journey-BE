package com.graduation.forumservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForumActivityEvent {
    // Actor Details (The person doing the action)
    private Long actorId;
    private String actorName;
    private String actorAvatarUrl;

    private Long recipientId;

    // Content Context
    private Long postId;
    private String postTitle;

    private Long answerId;   // Nullable if not related to an answer
    private Long commentId;  // Nullable if not related to a comment

    private ForumEventType type;

    public enum ForumEventType {
        ANSWER_ON_POST,      // User answers a question
        COMMENT_ON_POST,     // User comments directly on a post
        COMMENT_ON_ANSWER,   // User comments on an answer
        REPLY_ON_COMMENT     // User replies to another comment
    }
}
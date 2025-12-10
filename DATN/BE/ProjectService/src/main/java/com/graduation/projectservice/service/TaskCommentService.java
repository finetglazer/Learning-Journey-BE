package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.request.CreateCommentRequest;
import com.graduation.projectservice.payload.request.UpdateCommentRequest;
import com.graduation.projectservice.payload.response.BaseResponse;

public interface TaskCommentService {

    BaseResponse<?> getComments(Long userId, Long taskId);

    BaseResponse<?> createComment(Long userId, Long taskId, CreateCommentRequest request);

    BaseResponse<?> updateComment(Long userId, Long commentId, UpdateCommentRequest request);

    BaseResponse<?> deleteComment(Long userId, Long commentId);
}

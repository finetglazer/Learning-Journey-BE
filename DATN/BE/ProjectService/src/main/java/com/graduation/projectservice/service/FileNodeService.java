package com.graduation.projectservice.service;

import com.graduation.projectservice.payload.response.BaseResponse;
import com.graduation.projectservice.payload.response.TaskAttachmentDetailDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileNodeService {
    BaseResponse<?> getFiles(Long userId, Long projectId, Long parentNodeId);
    BaseResponse<?> createFolder(Long userId, Long projectId, Long parentNodeId, String name);
    BaseResponse<?> uploadFile(Long userId, Long projectId, Long parentNodeId, MultipartFile file) throws IOException;

    // Updated Signature
    BaseResponse<?> deleteNode(Long userId, Long projectId, Long nodeId);
    BaseResponse<?> searchFiles(Long userId, Long projectId, String keyword);
}
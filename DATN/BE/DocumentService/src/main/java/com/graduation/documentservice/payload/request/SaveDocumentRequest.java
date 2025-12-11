package com.graduation.documentservice.payload.request;

import com.graduation.documentservice.model.CommentThread;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveDocumentRequest {
    private Map<String, Object> content;
    private List<CommentThread> threads;
}

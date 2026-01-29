package com.graduation.forumservice.service.helper;

import com.graduation.forumservice.model.ForumTag;
import com.graduation.forumservice.repository.ForumTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for content-related utilities like text extraction and tag
 * synchronization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentHelperService {

    private final ForumTagRepository forumTagRepository;

    /**
     * Synchronizes tags with the database, creating new ones if they don't exist.
     */
    public List<String> syncTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty())
            return new ArrayList<>();

        return tagNames.stream().map(name -> {
            String normalized = name.trim();
            if (!forumTagRepository.existsByName(normalized)) {
                ForumTag newTag = ForumTag.builder().name(normalized).build();
                forumTagRepository.save(newTag);
            }
            return normalized;
        }).collect(Collectors.toList());
    }

    /**
     * Extracts plain text from TipTap JSON content structure.
     * UPDATED: Returns FULL text (unlimited length) for Search Indexing.
     * The Caller (Service layer) is responsible for truncating this if saving to a limited DB column.
     */
    public String extractPlainText(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try {
            Object rootContent = content.get("content");
            if (rootContent instanceof List<?> nodes) {
                for (Object node : nodes) {
                    traverseTipTapNode(node, sb);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse TipTap content: {}", e.getMessage());
        }

        // Clean up multiple spaces, but RETURN THE FULL STRING
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    /**
     * Recursively traverses TipTap nodes to find "text" types.
     */
    private void traverseTipTapNode(Object nodeObj, StringBuilder sb) {
        if (!(nodeObj instanceof Map<?, ?> node))
            return;

        if ("text".equals(node.get("type"))) {
            Object text = node.get("text");
            if (text instanceof String str) {
                sb.append(str);
            }
        }

        Object nestedContent = node.get("content");
        if (nestedContent instanceof List<?> children) {
            for (Object child : children) {
                traverseTipTapNode(child, sb);
            }
            sb.append(" "); // Add space between blocks to prevent words merging
        }
    }

    /**
     * Extracts file extension from filename.
     */
    public String getFileExtension(String filename) {
        return filename != null && filename.lastIndexOf(".") != -1
                ? filename.substring(filename.lastIndexOf(".") + 1)
                : "";
    }
}
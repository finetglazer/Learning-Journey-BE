package com.graduation.forumservice.service;

import com.graduation.forumservice.model.ForumSearchIndex;
import com.graduation.forumservice.repository.ForumSearchIndexRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncService {

    private final ForumSearchIndexRepository searchIndexRepository;

    @Async
    @Transactional
    public void syncPost(Long postId, Long userId, String title, String rawContent) {
        log.info("Async: Syncing Post ID {} to Search Index", postId);

        searchIndexRepository.deleteByPostId(postId);

        ForumSearchIndex searchEntry = ForumSearchIndex.builder()
                .postId(postId)
                .userId(userId)
                .docType(ForumSearchIndex.SearchDocType.POST)
                .title(title)
                .contentData(rawContent)
                .build();

        searchEntry = searchIndexRepository.save(searchEntry);

        // Updated: Pass raw strings. The Repository handles SQL escaping safely.
        updateSearchVectorNative(searchEntry.getSearchId(), title, rawContent);
    }

    // Update the method signature to accept postId
    @Async
    @Transactional
    public void syncAnswer(Long answerId, Long postId, Long userId, String rawContent) {
        log.info("Async: Syncing Answer ID {} (Post {}) to Search Index", answerId, postId);

        searchIndexRepository.deleteByAnswerId(answerId);

        ForumSearchIndex searchEntry = ForumSearchIndex.builder()
                .answerId(answerId)
                .postId(postId)
                .userId(userId)
                .docType(ForumSearchIndex.SearchDocType.ANSWER)
                .contentData(rawContent)
                .build();

        searchEntry = searchIndexRepository.save(searchEntry);
        updateSearchVectorNative(searchEntry.getSearchId(), null, rawContent);
    }

    @Async
    @Transactional
    public void deletePostIndex(Long postId) {
        searchIndexRepository.deleteByPostId(postId);
    }

    @Async
    @Transactional
    public void deleteAnswerIndex(Long answerId) {
        searchIndexRepository.deleteByAnswerId(answerId);
    }

    // Helper to force the TSVector update using Native SQL
    private void updateSearchVectorNative(Long searchId, String title, String body) {
        // We do NOT manual replace("'", "") anymore.
        // The Repository @Query uses parameters, which is safe.
        searchIndexRepository.updateSearchVector(searchId, title, body);
    }
}
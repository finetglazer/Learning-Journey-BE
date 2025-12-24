package com.graduation.forumservice.service.impl;

import com.graduation.forumservice.client.UserServiceClient;
import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.exception.NotFoundException;
import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.request.CreatePostRequest;
import com.graduation.forumservice.payload.request.UpdateSavePostStatusRequest;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.*;
import com.graduation.forumservice.service.ForumService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumServiceImpl implements ForumService {

    private final ForumPostRepository forumPostRepository;
    private final PostVoteRepository postVoteRepository;
    private final SavedPostRepository savedPostRepository;
    private final ProjectSavedPostRepository projectSavedPostRepository;
    private final PostStatsRepository postStatsRepository;
    private final ForumTagRepository forumTagRepository;
    private final ForumAnswerRepository forumAnswerRepository;
    private final MongoContentRepository mongoContentRepository;
    private final UserServiceClient userServiceClient;

    @Override
    public BaseResponse<?> getPostFeed(Long userId, int page, int limit, String filter, String sort, String search) {
        log.info("Fetching forum feed for userId={}, filter={}, sort={}, page={}, limit={}",
                userId, filter, sort, page, limit);

        // 1. Validate filter and sort parameters
        if (!isValidFilter(filter) || !isValidSort(sort)) {
            log.warn("Invalid filter ({}) or sort ({}) parameter", filter, sort);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    Constant.INVALID_PARAM,
                    null);
        }

        // 2. Create Pageable with limit + 1 strategy
        // Spring uses 0-based page indexing
        Pageable pageable = PageRequest.of(page - 1, limit + 1);

        // 3. Query posts using Native Query
        // This query joins forum_posts and post_stats
        List<Object[]> rawResults = forumPostRepository.findFeedPostsNative(userId, filter, sort, search, pageable);
        log.debug("Retrieved {} raw records from database", rawResults.size());

        // 4. Calculate hasMore using limit+1 strategy
        boolean hasMore = rawResults.size() > limit;
        if (hasMore) {
            rawResults = rawResults.subList(0, limit);
        }

        // 5. Convert entities to DTOs
        List<PostFeedDTO> postDTOs = new ArrayList<>();
        for (Object[] row : rawResults) {
            postDTOs.add(toPostFeedDTO(row));
        }

        // 6. Build pagination and response data
        PaginationDTO pagination = new PaginationDTO(page, hasMore);
        PostListResponse data = new PostListResponse(postDTOs, pagination);

        log.info("Successfully retrieved {} posts for feed", postDTOs.size());
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                Constant.FEED_RETRIEVED,
                data);
    }

    private PostFeedDTO toPostFeedDTO(Object[] row) {
        // Mapping logic matching your database schema
        ForumPost post = (ForumPost) row[0];
        return PostFeedDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .preview(post.getPlainTextPreview())
                .tags(post.getTags()) // text[] mapped via Hibernate 6
                .score((Integer) row[1])
                .viewCount((Long) row[2])
                .answerCount((Integer) row[3])
                .isSolved(post.getIsSolved())
                .createdAt(post.getCreatedAt())
                .build();
    }

    private boolean isValidFilter(String filter) {
        return List.of("ALL", "MY_POSTS", "SAVED_POSTS").contains(filter);
    }

    private boolean isValidSort(String sort) {
        return List.of("NEWEST", "HELPFUL", "RELEVANT").contains(sort);
    }

    @Override
    @Transactional
    public BaseResponse<?> createNewPost(Long userId, CreatePostRequest request) {
        log.info("Creating new post: userId={}, title={}", userId, request.getTitle());

        try {
            // 1. Extract plain text preview for PostgresSQL storage
            String plainTextPreview = extractPlainText(request.getContent());

            // 2. Save rich content to MongoDB first to get the Document ID
            MongoContent mongoContent = new MongoContent();
            mongoContent.setContent(request.getContent());
            mongoContent = mongoContentRepository.save(mongoContent);
            String mongoId = mongoContent.getIdAsString();

            // 3. Handle Tags: Sync incoming tags with the master dictionary
            List<String> synchronizedTags = syncTags(request.getTags());

            // 4. Create and save the ForumPost entity in PostgresSQL
            ForumPost post = ForumPost.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .plainTextPreview(plainTextPreview)
                    .mongoContentId(mongoId)
                    .tags(synchronizedTags)
                    .status(PostStatus.ACTIVE)
                    .isSolved(false)
                    .build();

            post = forumPostRepository.save(post);
            Long postId = post.getPostId();

            // 5. Initialize Post Statistics (Score: 0, Views: 0, Answers: 0)
            PostStats stats = PostStats.builder()
                    .postId(postId)
                    .score(0)
                    .viewCount(0L)
                    .answerCount(0)
                    .build();
            postStatsRepository.save(stats);

            log.info("Successfully created post with ID: {}", postId);
            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    "Post created successfully",
                    Map.of("postId", postId));

        } catch (Exception e) {
            log.error("Failed to create post: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }

    private List<String> syncTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new ArrayList<>();

        // Normalize names and find existing tags
        return tagNames.stream().map(name -> {
            String normalized = name.trim();
            if (!forumTagRepository.existsByName(normalized)) {
                ForumTag newTag = ForumTag.builder().name(normalized).build();
                forumTagRepository.save(newTag); // Create if not exists
            }
            return normalized;
        }).collect(Collectors.toList());
    }

    @Override
    public BaseResponse<?> getPostDetail(Long userId, Long postId) {
        log.info("Fetching post details: postId={}, requestedBy={}", postId, userId);

        // 1. Fetch Post and Stats from PostgresSQL using a custom projection/DTO row
        Optional<Object[]> optionalRow = forumPostRepository.findPostDetailById(postId);
        if (optionalRow.isEmpty()) {
            log.warn("Post not found with id: {}", postId);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    "Post not found!",
                    null
            );
        }
        Object[] row = optionalRow.get();
        ForumPost post = (ForumPost) row[0];
        Integer score = (Integer) row[1];
        Long viewCount = (Long) row[2];

        // 2. Fetch Rich Content from MongoDB
        MongoContent mongoContent = mongoContentRepository.findById(post.getMongoContentId())
                .orElse(new MongoContent()); // Fallback to empty content if missing

        // 3. Check current user's interaction (Vote and Saved status)
        Integer userVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId).orElse(new PostVote()).getType();
        boolean isSaved = savedPostRepository.existsByPostIdAndUserId(userId, postId);

        // 4. Update View Count (Asynchronous or direct increment)
        // Note: In a production app, use Redis to buffer these updates
        postStatsRepository.incrementViewCount(postId);

        // 5. Build the detailed DTO
        PostDetailDTO detailDTO = PostDetailDTO.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(mongoContent.getContent()) // The rich JSON map
                .author(fetchAuthorInfo(post.getUserId()))
                .tags(post.getTags())
                .stats(PostStatsDTO.builder()
                        .score(score)
                        .viewCount(viewCount + 1) // +1 to reflect current view
                        .isSolved(post.getIsSolved())
                        .build())
                .userVote(userVote) // 1, -1, or 0
                .isSaved(isSaved)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();

        log.info("Successfully retrieved details for post: {}", postId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                "Post details retrieved successfully",
                Map.of("post", detailDTO));
    }

    @Override
    @Transactional
    public BaseResponse<?> votePost(Long userId, Long postId, Integer voteType) {
        log.info("Processing vote: postId={}, userId={}, voteType={}", postId, userId, voteType);

        // 1. Check if user has already voted on this post
        Optional<PostVote> existingVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId);
        int scoreChange = 0;

        if (existingVote.isPresent()) {
            PostVote vote = existingVote.get();
            if (vote.getType().equals(voteType)) {
                // Case A: User clicks the same vote type again -> Remove the vote
                scoreChange = -vote.getType();
                postVoteRepository.delete(vote);
                voteType = 0; // Signal that no vote is now active
            } else {
                // Case B: User switches vote (e.g., from +1 to -1)
                scoreChange = voteType - vote.getType(); // e.g., -1 - (1) = -2
                vote.setType(voteType);
                postVoteRepository.save(vote);
            }
        } else {
            // Case C: New vote
            scoreChange = voteType;
            PostVote newVote = PostVote.builder()
                    .postId(postId)
                    .userId(userId)
                    .type(voteType)
                    .build();
            postVoteRepository.save(newVote);
        }

        // 2. Atomically update the total score in PostStat
        if (scoreChange != 0) {
            postStatsRepository.updateScore(postId, scoreChange);
        }

        // 3. Fetch the updated score to return to the frontend
        Integer newTotalScore = postStatsRepository.findByPostId(postId).getScore();

        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                "Your vote has been recorded",
                Map.of("newScore", newTotalScore, "userVote", voteType)
        );
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSolveStatus(Long userId, Long postId, Boolean isSolved) {
        log.info("Updating solve status: postId={}, userId={}, isSolved={}", postId, userId, isSolved);

        // 1. Fetch post and verify existence
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        // 2. Security Check: Only the post owner can mark it as solved
        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized solve status update attempt by userId={}", userId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Only the author can mark this post as solved", null);
        }

        // 3. Update and save
        post.setIsSolved(isSolved);
        forumPostRepository.save(post);

        log.info("Successfully updated solve status for post {}", postId);
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post solve status updated successfully", Map.of("isSolved", isSolved));
    }

    @Override
    @Transactional
    public BaseResponse<?> deletePostStrict(Long userId, Long postId) {
        log.info("Attempting strict delete for post: postId={}, requestedBy={}", postId, userId);

        // 1. Fetch the post and its statistics
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        // 2. Security Check: Only the owner (or an admin) can delete
        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt: userId {} tried to delete post {}", userId, postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to delete this post", null);
        }

        // 3. Strict Logic Check: Query answer_count from post_stats
        Integer answerCount = postStatsRepository.findAnswerCountByPostId(postId);

        if (answerCount != null && answerCount > 0) {
            log.warn("Delete blocked: Post {} has {} answers", postId, answerCount);
            return new BaseResponse<>(
                    Constant.ERROR_STATUS,
                    "Cannot delete a question that has answers.",
                    null
            );
        }

        try {
            // 3. CLEANUP: Remove bookmarks from all users/projects

            // Remove from private saved_posts
            savedPostRepository.deleteAllByPostId(postId);
            log.debug("Cleared private bookmarks for post {}", postId);

            // Remove from project_saved_posts
            projectSavedPostRepository.deleteAllBookmarksForPost(postId);
            log.debug("Cleared project space links for post {}", postId);

            // 4. CLEANUP: Metadata and Rich Content
            postVoteRepository.deleteByPostId(postId);
            postStatsRepository.deleteById(postId);

            if (post.getMongoContentId() != null) {
                mongoContentRepository.deleteById(post.getMongoContentId());
            }

            // 5. Final SQL Delete
            forumPostRepository.delete(post);

            log.info("Deep delete complete for post {}", postId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to execute deep delete: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSavePostStatus(Long userId, Long postId, UpdateSavePostStatusRequest request) {
        log.info("Processing save/unsave request: userId={}, postId={}, wannaSave={}",
                userId, postId, request.isWannaSave());

        // 1. Verify the post exists before attempting to bookmark it
        if (!forumPostRepository.existsById(postId)) {
            log.warn("Save failed: Post {} not found", postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Post not found!", null);
        }

        try {
            // 2. Handle Project-level Saving (Team Space)
            boolean wannaSave = request.isWannaSave();
            
            if (request.getTarget().equals("PROJECT")) {
                if (request.getProjectId() == null) {
                    return new BaseResponse<>(Constant.ERROR_STATUS, "Project ID is required for project save", null);
                }
                handleProjectSave(userId, postId, request.getProjectId(), wannaSave);
            }
            // 3. Handle Private Saving (Personal Bookmarks)
            else {
                handlePrivateSave(userId, postId, wannaSave);
            }

            log.info("Post {} {} successfully for user {}", postId, wannaSave ? "saved" : "unsaved", userId);
            return new BaseResponse<>(
                    Constant.SUCCESS_STATUS,
                    wannaSave ? "Post saved successfully" : "Post removed from saved",
                    null
            );

        } catch (Exception e) {
            log.error("Error toggling save status: {}", e.getMessage());
            throw e;
        }
    }

    private void handlePrivateSave(Long userId, Long postId, boolean wannaSave) {
        Optional<SavedPost> existing = savedPostRepository.findByPostIdAndUserId(userId, postId);
        if (existing.isPresent() && !wannaSave) {
            savedPostRepository.delete(existing.get());
        }
        else if (existing.isEmpty() && wannaSave) {
            SavedPost newSave = SavedPost.builder()
                    .userId(userId)
                    .postId(postId)
                    .createdAt(LocalDateTime.now())
                    .build();
            savedPostRepository.save(newSave);
        }
    }

    private void handleProjectSave(Long userId, Long postId, Long projectId, boolean wannaSave) {
        Optional<ProjectSavedPost> existing = projectSavedPostRepository.findByProjectIdAndPostId(projectId, postId);

        if (existing.isPresent() && !wannaSave) {
            projectSavedPostRepository.delete(existing.get());
        }
        else if (existing.isEmpty() && wannaSave) {
            ProjectSavedPost projectSave = ProjectSavedPost.builder()
                    .id(new ProjectSavedPostId(projectId, postId))
                    .savedByUserId(userId)
                    .savedAt(LocalDateTime.now())
                    .build();
            projectSavedPostRepository.save(projectSave);
        }
    }

    @Override
    public BaseResponse<?> getAnswersForPost(Long postId, int page, int limit, String sort) {
        log.info("Fetching answers for postId={}, sort={}, page={}, limit={}", postId, sort, page, limit);

        // 1. Validate sort parameters
        if (!List.of("NEWEST", "MOST_HELPFUL").contains(sort)) {
            log.warn("Invalid answer sort parameter: {}", sort);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Invalid sort parameter", null);
        }

        // 2. Pageable with limit + 1 strategy
        Pageable pageable = PageRequest.of(page - 1, limit + 1);

        // 3. Query PostgresSQL for metadata and scores
        List<Object[]> rawResults = forumAnswerRepository.findAnswersByPostIdNative(postId, sort, pageable);

        // 4. Handle Pagination
        boolean hasMore = rawResults.size() > limit;
        if (hasMore) {
            rawResults = rawResults.subList(0, limit);
        }

        // 5. Build DTOs with MongoDB content join
        List<AnswerDTO> answerDTOs = new ArrayList<>();
        for (Object[] row : rawResults) {
            answerDTOs.add(toAnswerDTO(row));
        }

        PaginationDTO pagination = new PaginationDTO(page, hasMore);
        AnswerListResponse data = new AnswerListResponse(answerDTOs, pagination);

        log.info("Retrieved {} answers for post {}", answerDTOs.size(), postId);
        return new BaseResponse<>(
                Constant.SUCCESS_STATUS,
                "Answers retrieved successfully",
                data);
    }

    private AnswerDTO toAnswerDTO(Object[] row) {
        ForumAnswer answer = (ForumAnswer) row[0];
        Integer score = (Integer) row[1];

        // Fetch rich text from MongoDB
        MongoContent mongoContent = mongoContentRepository.findById(answer.getMongoContentId())
                .orElse(new MongoContent());

        return AnswerDTO.builder()
                .answerId(answer.getAnswerId())
                .content(mongoContent.getContent())
                .author(fetchAuthorInfo(answer.getUserId()))
                .score(score)
                .isAccepted(answer.getIsAccepted())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
    }

    /**
     * Resolves a userId into a PostAuthorDTO by calling the User Service.
     * Includes a fallback mechanism for deleted or missing users.
     */
    private PostAuthorDTO fetchAuthorInfo(Long userId) {
        log.debug("Resolving author info for userId: {}", userId);

        // 1. Call the internal User Service client
        return userServiceClient.getUserById(userId)
                .map(user -> {
                    log.debug("Found user profile for ID {}: {}", userId, user.getName());
                    return PostAuthorDTO.builder()
                            .userId(user.getUserId())
                            .name(user.getName())
                            .avatar(user.getAvatarUrl())
                            .build();
                })
                // 2. Fallback logic: If user is not found, provide a placeholder
                .orElseGet(() -> {
                    log.warn("User profile not found for ID: {}. Using placeholder.", userId);
                    return PostAuthorDTO.builder()
                            .userId(userId)
                            .name("Unknown User")
                            .avatar(null)
                            .build();
                });
    }

    private String extractPlainText(Map<String, Object> content) {
        // Simple logic to extract text from your JSON structure
        // This should be optimized based on your Notion-style block structure
        StringBuilder sb = new StringBuilder();
        // ... extraction logic ...
        String result = sb.toString();
        return result.length() > 700 ? result.substring(0, 697) + "..." : result; //
    }
}
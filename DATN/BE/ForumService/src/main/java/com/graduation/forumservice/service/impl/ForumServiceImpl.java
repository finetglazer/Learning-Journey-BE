package com.graduation.forumservice.service.impl;

import com.graduation.forumservice.client.ProjectServiceClient;
import com.graduation.forumservice.client.UserServiceClient;
import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.exception.NotFoundException;
import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.*;
import com.graduation.forumservice.service.ForumService;
import com.graduation.forumservice.service.SequenceGeneratorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
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
    private final AnswerVoteRepository answerVoteRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final UserInfoCacheRepository userInfoCacheRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final ProjectServiceClient projectServiceClient;
    private final ForumPostFileRepository forumPostFileRepository;

    @Override
    public BaseResponse<?> getPostFeed(Long userId, int page, int limit, String filter, String sort, String search) {
        log.info("Fetching forum feed for userId={}, filter={}, sort={}, page={}, limit={}", userId, filter, sort, page, limit);

        // 1. Validate filter and sort parameters
        if (!isValidFilter(filter)) {
            log.warn("Invalid filter ({}) or sort ({}) parameter", filter, sort);
            return new BaseResponse<>(Constant.ERROR_STATUS, Constant.INVALID_PARAM, null);
        }

        // 2. Create Pageable with limit + 1 strategy
        // Spring uses 0-based page indexing
        Pageable pageable = PageRequest.of(page - 1, limit + 1);

        // 3. Query posts using Native Query
        // This query joins forum_posts and post_stats
        List<Object[]> rawResults = forumPostRepository.findFeedPostsNative(userId, filter.toUpperCase(), search, pageable);
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
        return new BaseResponse<>(Constant.SUCCESS_STATUS, Constant.FEED_RETRIEVED, data);
    }

    @Override
    @Transactional
    public BaseResponse<?> createNewPost(Long userId, CreatePostRequest request, List<MultipartFile> files) {
        log.info("Creating new post with files: userId={}, title={}", userId, request.getTitle());

        try {
            // 1. Extract plain text preview for PostgresSQL
            String plainTextPreview = extractPlainText(request.getContent());

            // 2. Save rich content to MongoDB
            MongoContent mongoContent = new MongoContent();
            mongoContent.setContent(request.getContent());
            mongoContent.setId(sequenceGeneratorService.generateSequence("mongo_content_sequence"));
            mongoContent = mongoContentRepository.save(mongoContent);

            // 3. Handle Tags
            List<String> synchronizedTags = syncTags(request.getTags());

            // 4. Create and save the ForumPost entity in PostgresSQL
            ForumPost post = ForumPost.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .plainTextPreview(plainTextPreview)
                    .mongoContentId(mongoContent.getId())
                    .tags(synchronizedTags)
                    .status(PostStatus.ACTIVE)
                    .isSolved(false)
                    .build();

            post = forumPostRepository.save(post);
            Long postId = post.getPostId();

            // 5. Handle File Uploads via ProjectServiceClient
            if (files != null && !files.isEmpty()) {
                // Internal call to ProjectService to upload to GCS
                // We use a dummy projectId (e.g., 0L) or a specific one if your forum posts belong to projects
                BaseResponse<?> uploadResponse = projectServiceClient.uploadMultipleFiles(userId, 0L, files);

                if (uploadResponse.getStatus() == Constant.SUCCESS_STATUS) {
                    // Extract the list of URLs or FileUploadDTOs from the response
                    Map<String, Object> data = (Map<String, Object>) uploadResponse.getData();
                    List<String> fileUrls = (List<String>) data.get("urls");

                    // 6. Save File Metadata to PostgresSQL (forum_post_files table)
                    List<ForumPostFile> postFiles = new ArrayList<>();
                    for (int i = 0; i < files.size(); i++) {
                        MultipartFile file = files.get(i);
                        String url = fileUrls.get(i);

                        postFiles.add(ForumPostFile.builder()
                                .postId(postId)
                                .storageRef(url)
                                .fileName(file.getOriginalFilename())
                                .extension(getFileExtension(file.getOriginalFilename()))
                                .fileSize(file.getSize())
                                .build());
                    }
                    forumPostFileRepository.saveAll(postFiles);
                    log.info("Attached {} files to post {}", postFiles.size(), postId);
                } else {
                    log.error("File upload failed via ProjectService");
                    // Decide if you want to fail the whole post or continue without files
                }
            }

            log.info("Successfully created post with ID: {}", postId);
            PostAuthorDTO author = fetchAuthorInfo(userId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post created successfully",
                    Map.of("post", toPostFeedDTO(post, author, 0, 0L, 0)));

        } catch (Exception e) {
            log.error("Failed to create post with files: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }



    @Override
    @Transactional
    public BaseResponse<?> updatePost(Long userId, Long postId, UpdatePostRequest request, List<MultipartFile> files) {
        log.info("Updating post with file management: postId={}, userId={}", postId, userId);

        try {
            // 1. Authorization and Existence Check
            ForumPost post = forumPostRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found!"));

            if (!post.getUserId().equals(userId)) {
                return new BaseResponse<>(Constant.ERROR_STATUS, "Unauthorized access", null);
            }

            // 2. Handle Old File Removal
            if (request.getFilesToRemove() != null && !request.getFilesToRemove().isEmpty()) {
                List<ForumPostFile> filesToDelete = forumPostFileRepository.findAllById(request.getFilesToRemove());

                // Extract storage references to clean up physical storage in GCS
                List<String> storageRefs = filesToDelete.stream()
                        .map(ForumPostFile::getStorageRef)
                        .toList();

                // Internal call to ProjectService to delete physical files
                projectServiceClient.deleteMultipleFiles(userId, storageRefs);

                // Remove metadata from PostgresSQL
                forumPostFileRepository.deleteAllInBatch(filesToDelete);
                log.info("Removed {} old files from post {}", filesToDelete.size(), postId);
            }

            // 3. Handle New File Uploads
            if (files != null && !files.isEmpty()) {
                BaseResponse<?> uploadRes = projectServiceClient.uploadMultipleFiles(userId, 0L, files);

                if (uploadRes.getStatus() == Constant.SUCCESS_STATUS) {
                    Map<String, Object> data = (Map<String, Object>) uploadRes.getData();
                    List<String> newStorageRefs = (List<String>) data.get("urls");

                    List<ForumPostFile> newFiles = new ArrayList<>();
                    for (int i = 0; i < files.size(); i++) {
                        newFiles.add(ForumPostFile.builder()
                                .postId(postId)
                                .storageRef(newStorageRefs.get(i))
                                .fileName(files.get(i).getOriginalFilename())
                                .extension(files.get(i).getContentType())
                                .fileSize(files.get(i).getSize())
                                .build());
                    }
                    forumPostFileRepository.saveAll(newFiles);
                    log.info("Attached {} new files to post {}", newFiles.size(), postId);
                }
            }

            // 4. Update Content (MongoDB) and Metadata (Postgres)
            if (request.getContent() != null) {
                MongoContent mongoContent = mongoContentRepository.findByIntId(post.getMongoContentId())
                        .orElseThrow(() -> new NotFoundException("Content not found!"));
                mongoContent.setContent(request.getContent());
                mongoContentRepository.save(mongoContent);
                post.setPlainTextPreview(extractPlainText(request.getContent()));
            }

            if (request.getTitle() != null) post.setTitle(request.getTitle());
            if (request.getTags() != null) post.setTags(syncTags(request.getTags()));
            post.setUpdatedAt(LocalDateTime.now());

            forumPostRepository.save(post);

            PostAuthorDTO author = fetchAuthorInfo(userId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post updated successfully",
                    toPostFeedDTO(post, author, 0, 0L, 0));

        } catch (Exception e) {
            log.error("Failed to update post {}: {}", postId, e.getMessage());
            throw e; // Triggers rollback for all SQL operations
        }
    }

    @Override
    public BaseResponse<?> getPostDetail(Long userId, Long postId) {
        log.info("Fetching post detail with files: postId={}", postId);

        // 1. Fetch Post Detail (Native Mapping)
        Optional<Object[]> optionalResult = forumPostRepository.findPostDetailByIdNative(postId);
        if (optionalResult.isEmpty()) {
            return new BaseResponse<>(Constant.ERROR_STATUS, "Post not found!", null);
        }

        Object[] data = (Object[]) optionalResult.get()[0];
        Long authorUserId = ((Number) data[1]).longValue();
        int mongoContentId = Integer.parseInt(String.valueOf(data[4]));
        String tagsStr = (String) data[8];
        List<String> tags = (tagsStr != null && !tagsStr.isEmpty())
                ? Arrays.asList(tagsStr.split(","))
                : Collections.emptyList();

        // 2. NEW: Fetch Attached Files from PostgresSQL
        List<FileUploadDTO> attachedFiles = forumPostFileRepository.findAllByPostId(postId)
                .stream()
                .map(this::mapToFileDTO)
                .toList();

        // 3. Fetch Answers and Comments
        List<AnswerDTO> answers = fetchTopAnswers(postId);
        List<Long> answerIds = answers.stream().map(AnswerDTO::getAnswerId).toList();
        List<CommentDTO> rootComments = fetchRootComments(postId, answerIds);

        // 4. Resolve Content and Author
        MongoContent postContent = mongoContentRepository.findByIntId(mongoContentId).orElse(new MongoContent());
        PostAuthorDTO postAuthor = fetchAuthorInfo(authorUserId);

        // 5. Interaction checks (Fixed parameter order for isSaved)
        Integer userVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId)
                .map(PostVote::getType).orElse(0);
        boolean isSaved = savedPostRepository.existsByPostIdAndUserId(postId, userId); //
        List<Long> savedToProjectIds = projectSavedPostRepository.findByPostId(postId)
                .stream().map(item -> item.getId().getProjectId())
                .collect(Collectors.toList());

        // 6. Build Final DTO
        PostDetailDTO detailDTO = PostDetailDTO.builder()
                .postId(postId)
                .title((String) data[2])
                .content(postContent.getContent())
                .author(postAuthor)
                .tags(tags)
                .files(attachedFiles) // Include the new files list
                .stats(PostStatsDTO.builder()
                        .score(((Number) data[9]).intValue())
                        .viewCount(((Number) data[10]).longValue() + 1)
                        .isSolved((Boolean) data[5])
                        .answerCount(((Number) data[11]).intValue())
                        .build())
                .answers(answers)
                .savedToProjectIds(savedToProjectIds)
                .comments(rootComments)
                .userVote(userVote)
                .isSaved(isSaved)
                .createdAt(((java.sql.Timestamp) data[6]).toLocalDateTime())
                .updatedAt(data[7] != null ? ((java.sql.Timestamp) data[7]).toLocalDateTime() : null)
                .build();

        postStatsRepository.incrementViewCount(postId);
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Success", detailDTO);
    }

    private FileUploadDTO mapToFileDTO(ForumPostFile file) {
        return FileUploadDTO.builder()
                .fileId(file.getFileId())
                .url(file.getStorageRef())
                .name(file.getFileName())
                .extension(file.getExtension())
                .size(file.getFileSize())
                .build();
    }

    private List<AnswerDTO> fetchTopAnswers(Long postId) {
        Pageable answerPageable = PageRequest.of(0, 5);
        List<Object[]> answerRows = forumAnswerRepository.findAnswersByPostIdNative(postId, "MOST_HELPFUL", answerPageable);

        return answerRows.stream().map(row -> {
            Object[] a = (row[0] instanceof Object[]) ? (Object[]) row[0] : row;
            Long answerId = ((Number) a[0]).longValue();

            return AnswerDTO.builder()
                    .answerId(answerId)
                    .author(fetchAuthorInfo(((Number) a[1]).longValue()))
                    .content(mongoContentRepository.findByIntId(Integer.parseInt(String.valueOf(a[3])))
                            .map(MongoContent::getContent).orElse(null))
                    .score(((Number) a[9]).intValue())
                    .isAccepted((Boolean) a[6])
                    .createdAt(((java.sql.Timestamp) a[7]).toLocalDateTime())
                    .build();
        }).toList();
    }

    private List<CommentDTO> fetchRootComments(Long postId, List<Long> answerIds) {
        Pageable limit = PageRequest.of(0, 2);

        // 1. Fetch comments belonging directly to the Post
        List<CommentDTO> postComments = forumCommentRepository.findByPostIdOrderByCreatedAtAsc(postId, limit)
                .stream().map(this::mapToCommentDTO).toList();
        List<CommentDTO> allComments = new ArrayList<>(postComments);

        // 2. Fetch comments for each Answer
        for (Long answerId : answerIds) {
            List<CommentDTO> answerComments = forumCommentRepository.findByAnswerIdOrderByCreatedAtAsc(answerId, limit)
                    .stream().map(this::mapToCommentDTO).toList();
            allComments.addAll(answerComments);
        }

        // 3. Optional: Sort the final flat list by creation date
        return allComments.stream()
                .sorted(Comparator.comparing(CommentDTO::getCreatedAt))
                .toList();
    }

    private CommentDTO mapToCommentDTO(ForumComment comment) {
        return CommentDTO.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .answerId(comment.getAnswerId())
                .content(comment.getContentText())
                .author(fetchAuthorInfo(comment.getUserId())) // Uses local cache
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public BaseResponse<?> votePost(Long userId, Long postId, Integer voteType) {
        log.info("Processing vote: postId={}, userId={}, voteType={}", postId, userId, voteType);

        // 1. Check if user has already voted on this post
        Optional<PostVote> existingVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId);
        int scoreChange;

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
            PostVote newVote = PostVote.builder().postId(postId).userId(userId).type(voteType).build();
            postVoteRepository.save(newVote);
        }

        // 2. Atomically update the total score in PostStat
        if (scoreChange != 0) {
            postStatsRepository.updateScore(postId, scoreChange);
        }

        // 3. Fetch the updated score to return to the frontend
        Integer newTotalScore = postStatsRepository.findByPostId(postId).getScore();

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Your vote has been recorded", Map.of("newScore", newTotalScore, "userVote", voteType));
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSolveStatus(Long userId, Long postId, Boolean isSolved) {
        log.info("Updating solve status: postId={}, userId={}, isSolved={}", postId, userId, isSolved);

        // 1. Fetch post and verify existence
        ForumPost post = forumPostRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found!"));

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
        ForumPost post = forumPostRepository.findById(postId).orElseThrow(() -> new NotFoundException("Post not found!"));

        // 2. Security Check: Only the owner (or an admin) can delete
        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt: userId {} tried to delete post {}", userId, postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to delete this post", null);
        }

        // 3. Strict Logic Check: Query answer_count from post_stats
        Integer answerCount = postStatsRepository.findAnswerCountByPostId(postId);

        if (answerCount != null && answerCount > 0) {
            log.warn("Delete blocked: Post {} has {} answers", postId, answerCount);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Cannot delete a question that has answers.", null);
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
        log.info("Processing save/unsave request: userId={}, postId={}, wannaSave={}", userId, postId, request.isWannaSave());

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
            return new BaseResponse<>(Constant.SUCCESS_STATUS, wannaSave ? "Post saved successfully" : "Post removed from saved", null);

        } catch (Exception e) {
            log.error("Error toggling save status: {}", e.getMessage());
            throw e;
        }
    }

    private void handlePrivateSave(Long userId, Long postId, boolean wannaSave) {
        Optional<SavedPost> existing = savedPostRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent() && !wannaSave) {
            savedPostRepository.delete(existing.get());
        } else if (existing.isEmpty() && wannaSave) {
            SavedPost newSave = SavedPost.builder().userId(userId).postId(postId).createdAt(LocalDateTime.now()).build();
            savedPostRepository.save(newSave);
        }
    }

    private void handleProjectSave(Long userId, Long postId, Long projectId, boolean wannaSave) {
        Optional<ProjectSavedPost> existing = projectSavedPostRepository.findByProjectIdAndPostId(projectId, postId);

        if (existing.isPresent() && !wannaSave) {
            projectSavedPostRepository.delete(existing.get());
        } else if (existing.isEmpty() && wannaSave) {
            ProjectSavedPost projectSave = ProjectSavedPost.builder().id(new ProjectSavedPostId(projectId, postId)).savedByUserId(userId).savedAt(LocalDateTime.now()).build();
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
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answers retrieved successfully", data);
    }

    @Override
    @Transactional
    public BaseResponse<?> submitAnswer(Long userId, Long postId, CreateAnswerRequest request) {
        log.info("Submitting new answer: userId={}, postId={}", userId, postId);

        // 1. Verify the parent post exists
        if (!forumPostRepository.existsById(postId)) {
            log.warn("Answer submission failed: Post {} not found", postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Post not found!", null);
        }

        try {
            // 2. Extract plain text preview for PostgresSQL storage (e.g., for notifications or search)
            String plainTextPreview = extractPlainText(request.getContent());

            // 3. Save rich content to MongoDB
            MongoContent mongoContent = new MongoContent();
            mongoContent.setContent(request.getContent());
            mongoContent.setId(sequenceGeneratorService.generateSequence("mongo_content_sequence"));
            mongoContent = mongoContentRepository.save(mongoContent);

            // 4. Create and save the ForumAnswer entity using your updated model
            ForumAnswer answer = ForumAnswer
                    .builder()
                    .postId(postId)
                    .userId(userId)
                    .plainTextPreview(plainTextPreview)
                    .mongoContentId(mongoContent.getId())
                    .isAccepted(false)
                    .upvoteCount(0)
                    .downvoteCount(0)
                    .build();

            answer = forumAnswerRepository.save(answer);
            Long answerId = answer.getAnswerId();

            // 6. Atomic Increment: Update the answer_count on the parent post
            postStatsRepository.incrementAnswerCount(postId);

            log.info("Successfully submitted answer {} for post {}", answerId, postId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer submitted successfully", Map.of("answerId", answerId));

        } catch (Exception e) {
            log.error("Failed to submit answer: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> editAnswer(Long userId, Long answerId, EditAnswerRequest request) {
        log.info("Editing answer: answerId={}, userId={}", answerId, userId);

        // 1. Fetch and verify the answer exists and belongs to the user
        ForumAnswer answer = forumAnswerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found!"));

        if (!answer.getUserId().equals(userId)) {
            log.warn("Unauthorized edit attempt: userId {} on answerId {}", userId, answerId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You can only edit your own answers!", null);
        }

        try {
            // 2. Update Rich Content in MongoDB
            // We use the existing intId from the PostgresSQL record to find the Mongo document
            int mongoId = answer.getMongoContentId();

            MongoContent mongoContent = mongoContentRepository.findByIntId(mongoId)
                    .orElseThrow(() -> new NotFoundException("Content not found in MongoDB!"));

            mongoContent.setContent(request.getContent());
            mongoContentRepository.save(mongoContent);

            // 3. Update PostgresSQL Metadata
            String plainTextPreview = extractPlainText(request.getContent());
            answer.setPlainTextPreview(plainTextPreview);
            answer.setUpdatedAt(LocalDateTime.now()); // Ensure your @Entity has this field

            forumAnswerRepository.save(answer);

            log.info("Successfully updated answer ID: {}", answerId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer updated successfully", null);

        } catch (Exception e) {
            log.error("Failed to edit answer: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback across SQL and Mongo (if configured)
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> switchAnswerAcceptStatus(Long userId, Long answerId) {
        log.info("Attempting to switch answer accept status: answerId={}, by userId={}", answerId, userId);

        // 1. Fetch the answer and its parent post
        ForumAnswer answer = forumAnswerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found!"));

        // Fetch parent post directly using the answer's post relationship
        ForumPost post = forumPostRepository.findById(answer.getPostId())
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        // 2. Security Check: Only the post author can accept an answer
        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized switch: userId {} is not the author of post {}", userId, post.getPostId());
            return new BaseResponse<>(Constant.ERROR_STATUS,
                    "Only the author of the post can switch accept status of an answer.", null);
        }

        try {
            // 3. Determine the new state (Toggle logic)
            // If it was already accepted, we are unaccepting it.
            // If it wasn't, we are accepting it and must clear others.
            boolean currentlyAccepted = Boolean.TRUE.equals(answer.getIsAccepted());
            boolean targetState = !currentlyAccepted;

            if (targetState) {
                // If we are accepting this answer, unmark all other answers for this post first
                forumAnswerRepository.unmarkAcceptedAnswersForPost(post.getPostId());
            }

            // 4. Update the current answer status
            answer.setIsAccepted(targetState);
            forumAnswerRepository.save(answer);

            // 5. Update the parent post status
            // The post is "solved" if any answer is currently accepted.
            post.setIsSolved(targetState);
            forumPostRepository.save(post);

            log.info("Answer {} is now isAccepted={} for post {}", answerId, targetState, post.getPostId());
            return new BaseResponse<>(Constant.SUCCESS_STATUS,
                    "Answer accepted status updated successfully",
                    Map.of("postId", post.getPostId(), "answerId", answerId, "isAccepted", targetState));

        } catch (Exception e) {
            log.error("Failed to update answer status: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteAnswer(Long userId, Long answerId) {
        log.info("Executing deep delete for answer: answerId={}, requestedBy={}", answerId, userId);

        // 1. Fetch the answer to verify existence and ownership
        ForumAnswer answer = forumAnswerRepository.findById(answerId).orElseThrow(() -> new NotFoundException("Answer not found!"));

        // 2. Security Check
        if (!answer.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt by userId {}", userId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Permission denied", null);
        }

        try {
            // 3. Clean up Answer Votes
            // This removes records from the answer_votes table
            answerVoteRepository.deleteByAnswerId(answerId);
            log.debug("Cleared votes for answer {}", answerId);

            // 4. Clean up Answer Comments
            forumCommentRepository.deleteAllByAnswerId(answerId);
            log.debug("Cleared all comments for answer {}", answerId);

            // 5. Clean up Answer's own MongoDB rich text
            if (answer.getMongoContentId() != null) {
                mongoContentRepository.deleteById(answer.getMongoContentId());
            }

            // 6. Update parent post statistics
            postStatsRepository.decrementAnswerCount(answer.getPostId());

            // 7. Final SQL Delete of the Answer itself
            forumAnswerRepository.delete(answer);

            log.info("Deep delete complete for answer {}", answerId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer and associated data removed", null);

        } catch (Exception e) {
            log.error("Deep delete failed: {}", e.getMessage());
            throw e; // Triggers rollback for all SQL deletions
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> voteAnswer(Long userId, Long answerId, Integer voteType) {
        log.info("Processing answer vote: answerId={}, userId={}, voteType={}", answerId, userId, voteType);

        Optional<AnswerVote> existingVote = answerVoteRepository.findByAnswerIdAndUserId(answerId, userId);

        int upvoteChange = 0;
        int downvoteChange = 0;

        if (existingVote.isPresent()) {
            AnswerVote vote = existingVote.get();

            if (vote.getType().equals(voteType)) {
                // Case A: Remove existing vote (Clicking same button twice)
                if (voteType == 1) upvoteChange = -1;
                else downvoteChange = -1;

                answerVoteRepository.delete(vote);
                voteType = 0; // Reset for response
            } else {
                // Case B: Switch vote (e.g., Up to Down)
                if (voteType == 1) { // Switching to Upvote
                    upvoteChange = 1;
                    downvoteChange = -1;
                } else { // Switching to Downvote
                    upvoteChange = -1;
                    downvoteChange = 1;
                }
                vote.setType(voteType);
                answerVoteRepository.save(vote);
            }
        } else {
            // Case C: New vote
            if (voteType == 1) upvoteChange = 1;
            else downvoteChange = 1;

            AnswerVote newVote = AnswerVote.builder().answerId(answerId).userId(userId).type(voteType).build();
            answerVoteRepository.save(newVote);
        }

        // 2. Atomic update of counts in the forum_answers table
        forumAnswerRepository.updateAnswerVoteCounts(answerId, upvoteChange, downvoteChange);

        // 3. Get updated state for the response
        // We refresh the answer from the DB to get the latest atomic counts
        Optional<ForumAnswer> optionalUpdatedAnswer = forumAnswerRepository.findById(answerId);
        if (optionalUpdatedAnswer.isEmpty()) {
            throw new NotFoundException("Answer not found!");
        }
        ForumAnswer updatedAnswer = optionalUpdatedAnswer.get();
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Vote recorded", Map.of("newScore", updatedAnswer.getScore(), "upvoteCount", updatedAnswer.getUpvoteCount(), "downvoteCount", updatedAnswer.getDownvoteCount(), "userVote", voteType));
    }

    @Override
    public BaseResponse<?> getComments(String targetType, Long targetId, int page, int limit) {
        log.info("Fetching SQL-based comments: type={}, id={}, page={}, limit={}", targetType, targetId, page, limit);

        // 1. Setup Pageable (limit + 1 strategy)
        Pageable pageable = PageRequest.of(page - 1, limit);
        List<ForumComment> rawComments;

        // 2. Fetch directly from SQL repositories
        if ("POST".equalsIgnoreCase(targetType)) {
            rawComments = forumCommentRepository.findAllByPostIdOrderByCreatedAtAsc(targetId, pageable);
        } else if ("ANSWER".equalsIgnoreCase(targetType)) {
            rawComments = forumCommentRepository.findAllByAnswerIdOrderByCreatedAtAsc(targetId, pageable);
        } else {
            return new BaseResponse<>(Constant.ERROR_STATUS, "Invalid target type", null);
        }

        // 3. Handle Pagination
        boolean hasMore = rawComments.size() > limit;
        if (hasMore) {
            rawComments = rawComments.subList(0, limit);
        }

        // 4. Map to DTO
        List<CommentDTO> commentDTOs = rawComments.stream().map(this::toCommentDTO).toList();

        PaginationDTO pagination = new PaginationDTO(page, hasMore);
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comments retrieved successfully", Map.of("comments", commentDTOs, "pagination", pagination));
    }

    @Override
    @Transactional
    public BaseResponse<?> addComment(Long userId, CreateCommentRequest request) {
        log.info("Adding new comment: userId={}, targetType={}, targetId={}",
                userId, request.getTargetType(), request.getTargetId());

        try {
            Long postId = null;
            Long answerId = null;
            Long parentCommentId = null;

            // 1. Resolve Target and Validate Existence
            if ("POST".equalsIgnoreCase(request.getTargetType())) {
                if (!forumPostRepository.existsById(request.getTargetId())) {
                    throw new NotFoundException("Target post not found!");
                }
                postId = request.getTargetId();
            } else if ("ANSWER".equalsIgnoreCase(request.getTargetType())) {
                if (!forumAnswerRepository.existsById(request.getTargetId())) {
                    throw new NotFoundException("Target answer not found!");
                }
                answerId = request.getTargetId();
            } else if ("COMMENT".equalsIgnoreCase(request.getTargetType())) {
                // Case: User is replying to another comment
                ForumComment targetComment = forumCommentRepository.findById(request.getTargetId())
                        .orElseThrow(() -> new NotFoundException("Target comment not found!"));

                parentCommentId = targetComment.getCommentId();
                // Inherit the root scope (Post or Answer) from the parent comment
                postId = targetComment.getPostId();
                answerId = targetComment.getAnswerId();
            }

            // 2. Handle Reply Preview Snapshot
            String previewSnapshot = null;
            if (parentCommentId != null) {
                previewSnapshot = forumCommentRepository.findById(parentCommentId).map(parent -> {
                    String text = parent.getContentText();
                    return (text != null && text.length() > 50) ? text.substring(0, 47) + "..." : text;
                }).orElse(null);
            }

            // 3. Build and Save Entity
            ForumComment comment = ForumComment.builder()
                    .userId(userId)
                    .postId(postId)
                    .answerId(answerId)
                    .parentCommentId(parentCommentId)
                    .contentText(request.getContent())
                    .replyPreviewSnapshot(previewSnapshot)
                    .build();

            comment = forumCommentRepository.save(comment);

            log.info("Successfully saved comment with ID: {}", comment.getCommentId());
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment added successfully", toCommentDTO(comment));

        } catch (Exception e) {
            log.error("Failed to add comment: {}", e.getMessage());
            throw e; // Triggers @Transactional rollback
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> editComment(Long userId, Long commentId, UpdateCommentRequest request) {
        log.info("Editing comment: commentId={}, userId={}", commentId, userId);

        try {
            // 1. Find the comment
            ForumComment comment = forumCommentRepository.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found!"));

            // 2. Check ownership (Security check)
            if (!comment.getUserId().equals(userId)) {
                return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to edit this comment", null);
            }

            // 3. Update the content
            comment.setContentText(request.getContent());
            comment.setUpdatedAt(LocalDateTime.now());

            // 4. Save (Automatic via @Transactional, but explicit save is fine)
            ForumComment updatedComment = forumCommentRepository.save(comment);

            log.info("Successfully updated comment ID: {}", commentId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment updated successfully", toCommentDTO(updatedComment));

        } catch (NotFoundException e) {
            log.error("Edit failed: {}", e.getMessage());
            return new BaseResponse<>(Constant.ERROR_STATUS, e.getMessage(), null);
        } catch (Exception e) {
            log.error("Unexpected error during comment edit: {}", e.getMessage());
            throw e; // Triggers rollback
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteComment(Long userId, Long commentId) {
        log.info("Attempting to delete comment: commentId={}, requestedBy={}", commentId, userId);

        // 1. Fetch the comment and verify existence
        ForumComment comment = forumCommentRepository.findById(commentId).orElseThrow(() -> new NotFoundException("Comment not found!"));

        // 2. Security Check: Only the author can delete their comment
        if (!comment.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt: userId {} tried to delete comment {}", userId, commentId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to delete this comment", null);
        }

        try {
            // 3. Recursive Cleanup: Delete all replies to this comment
            // This prevents Foreign Key constraint violations if you have self-referencing keys
            Long deletedReplies = forumCommentRepository.deleteAllByParentCommentId(commentId);
            log.debug("Deleted {} child replies for comment {}", deletedReplies, commentId);

            // 4. Delete the main comment
            forumCommentRepository.delete(comment);

            log.info("Successfully deleted comment {}", commentId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Comment and its replies deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to delete comment {}: {}", commentId, e.getMessage());
            throw e; // Triggers rollback
        }
    }

    @Override
    public BaseResponse<?> searchTags(String query) {
        log.info("Searching for tags matching query: '{}'", query);

        // 1. Handle empty or too short queries to prevent heavy scans
        if (query == null || query.trim().isEmpty()) {
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Search query too short", new ArrayList<>());
        }

        // 2. Fetch matching tags from repository
        // We limit to top 10 results for better UX in dropdowns
        List<String> matchingTags = forumTagRepository.findByNameContainingIgnoreCase(query.trim()).stream().map(obj -> ((ForumTag) obj).getName()).limit(10).toList();

        log.info("Found {} tags matching '{}'", matchingTags.size(), query);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Tags retrieved successfully", Map.of("tags", matchingTags));
    }

    private CommentDTO toCommentDTO(ForumComment comment) {
        log.debug("Mapping ForumComment entity to DTO: commentId={}", comment.getCommentId());

        return CommentDTO.builder().commentId(comment.getCommentId()).content(comment.getContentText()) // Maps to the 'text' column in PostgresSQL
                .author(fetchAuthorInfo(comment.getUserId())) // Calls your User Service resolver
                .parentCommentId(comment.getParentCommentId()) // Null for top-level, Long for replies
                .replyPreview(comment.getReplyPreviewSnapshot()) // Context snippet for the reply
                .postId(comment.getPostId()).answerId(comment.getAnswerId()).createdAt(comment.getCreatedAt()).updatedAt(comment.getUpdatedAt()).build();
    }

    private PostFeedDTO toPostFeedDTO(Object[] row) {
        // 1. Handle Tags String to List conversion (Index 7)
        String tagsStr = (String) row[7];
        List<String> tags = (tagsStr != null && !tagsStr.isEmpty())
                ? Arrays.asList(tagsStr.split(","))
                : Collections.emptyList();

        // 2. Fix: Handle Numeric Ordinal for PostStatus (Index 6)
        PostStatus status;
        Object statusRaw = row[6];

        if (statusRaw instanceof Number) {
            // Standard numeric ordinal from DB
            status = PostStatus.values()[((Number) statusRaw).intValue()];
        } else if (statusRaw instanceof String statusStr) {
            // Check if the string is actually a numeric index (like "0")
            if (statusStr.matches("\\d+")) {
                int index = Integer.parseInt(statusStr);
                status = PostStatus.values()[index];
            } else {
                // Standard string name (like "ACTIVE")
                status = PostStatus.valueOf(statusStr);
            }
        } else {
            status = PostStatus.ACTIVE;
        }

        // 3. Resolve Author Information using Cache-Aside
        Long userId = ((Number) row[1]).longValue();
        PostAuthorDTO author = fetchAuthorInfo(userId);

        // 4. Construct DTO with safe casting
        return PostFeedDTO.builder()
                .postId(((Number) row[0]).longValue())
                .userId(userId)
                .title((String) row[2])
                .preview((String) row[3])
                // mongoContentId is at Index 4, ensure it's in your DTO if needed
                .isSolved((Boolean) row[5])
                .status(status)
                .tags(tags)
                .authorId(author.getUserId())
                .authorName(author.getName())
                .authorAvatar(author.getAvatar())
                .createdAt(((java.sql.Timestamp) row[8]).toLocalDateTime())
                .score(((Number) row[9]).intValue())
                .viewCount(((Number) row[10]).longValue())
                .answerCount(((Number) row[11]).intValue())
                .build();
    }

    /**
     * Maps a ForumPost entity and its related metadata into a PostFeedDTO.
     * Used to return a consistent structure after post creation.
     */
    private PostFeedDTO toPostFeedDTO(ForumPost post, PostAuthorDTO author, Integer score, Long views, Integer answers) {
        return PostFeedDTO.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .authorId(author.getUserId())
                .authorName(author.getName())
                .authorAvatar(author.getAvatar())
                .title(post.getTitle())
                .preview(post.getPlainTextPreview())
                .isSolved(post.getIsSolved())
                .status(post.getStatus())
                .tags(post.getTags())
                .createdAt(post.getCreatedAt())
                .score(score)
                .viewCount(views)
                .answerCount(answers)
                .build();
    }

    private boolean isValidFilter(String filter) {
        return List.of("ALL", "MY_POSTS", "SAVED_POSTS", "MOST_HELPFUL").contains(filter.toUpperCase());
    }

    private AnswerDTO toAnswerDTO(Object[] row) {
        // Index Mapping based on the SELECT above:
        // 0: answer_id, 1: user_id, 2: post_id, 3: mongo_content_id,
        // 4: upvote_count, 5: downvote_count, 6: is_accepted,
        // 7: created_at, 8: updated_at, 9: calculated_score

        Long answerId = ((Number) row[0]).longValue();
        Long userId = ((Number) row[1]).longValue();
        int mongoContentId = Integer.parseInt(String.valueOf(row[3]));
        Boolean isAccepted = (Boolean) row[6];

        // Fetch rich text from MongoDB using the ID string
        MongoContent mongoContent = mongoContentRepository.findByIntId(mongoContentId)
                .orElse(new MongoContent());

        // Construct DTO
        return AnswerDTO.builder()
                .answerId(answerId)
                .content(mongoContent.getContent())
                .author(fetchAuthorInfo(userId)) // Uses your UserInfoCache
                .score(((Number) row[9]).intValue())
                .isAccepted(isAccepted)
                .createdAt(((java.sql.Timestamp) row[7]).toLocalDateTime())
                .updatedAt(row[8] != null ? ((java.sql.Timestamp) row[8]).toLocalDateTime() : null)
                .build();
    }

    /**
     * Resolves a userId into a PostAuthorDTO using a tiered approach:
     * 1. Check local UserInfoCache (PostgresSQL).
     * 2. If missing, call User Service and update the local cache.
     * 3. Fallback to placeholder if User Service also fails.
     */
    private PostAuthorDTO fetchAuthorInfo(Long userId) {
        log.debug("Resolving author info for userId: {}", userId);

        // 1. Try to find in local cache first
        return userInfoCacheRepository.findById(userId).map(cache -> {
                    log.debug("Cache hit for userId: {}", userId);
                    return PostAuthorDTO.builder()
                            .userId(cache.getUserId())
                            .name(cache.getDisplayName())
                            .email(cache.getEmail())
                            .avatar(cache.getAvatarUrl()).build();
                })
                // 2. Cache miss: Call Remote User Service
                .orElseGet(() -> {
                    log.info("Cache miss for userId: {}. Fetching from User Service.", userId);

                    return userServiceClient.getUserById(userId).map(user -> {
                                // Update local cache for next time
                                UserInfoCache newCache = new UserInfoCache(
                                        user.getUserId(),
                                        user.getName(),
                                        user.getAvatarUrl(),
                                        user.getEmail()
                                );
                                userInfoCacheRepository.save(newCache);

                                return PostAuthorDTO.builder()
                                        .userId(user.getUserId())
                                        .name(user.getName())
                                        .avatar(user.getAvatarUrl())
                                        .email(user.getEmail())
                                        .build();
                            })
                            // 3. Ultimate Fallback: User doesn't exist in system
                            .orElseGet(() -> {
                                log.warn("User ID {} not found in User Service. Returning placeholder.", userId);
                                return PostAuthorDTO.builder().userId(userId).name("Unknown User").avatar(null).build();
                            });
                });
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

    private String extractPlainText(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        try {
            // TipTap JSON structure always starts with a "content" array at the root
            Object rootContent = content.get("content");
            if (rootContent instanceof List<?> nodes) {
                for (Object node : nodes) {
                    traverseTipTapNode(node, sb);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse TipTap content: {}", e.getMessage());
        }

        // Clean up: normalize whitespace and handle the 700-character limit
        String result = sb.toString().trim().replaceAll("\\s+", " ");
        return result.length() > 700 ? result.substring(0, 697) + "..." : result;
    }

    /**
     * Recursively traverses TipTap nodes to find "text" types
     */
    private void traverseTipTapNode(Object nodeObj, StringBuilder sb) {
        if (!(nodeObj instanceof Map<?, ?> node)) return;

        // 1. If it's a text node, append its content
        if ("text".equals(node.get("type"))) {
            Object text = node.get("text");
            if (text instanceof String str) {
                sb.append(str);
            }
        }

        // 2. If it has nested content (like a list or paragraph), recurse into it
        Object nestedContent = node.get("content");
        if (nestedContent instanceof List<?> children) {
            for (Object child : children) {
                traverseTipTapNode(child, sb);
            }
            // Add a space after block-level elements to prevent words sticking together
            sb.append(" ");
        }
    }

    private String getFileExtension(String filename) {
        return filename != null && filename.lastIndexOf(".") != -1
                ? filename.substring(filename.lastIndexOf(".") + 1)
                : "";
    }

    @Override
    @Transactional
    public BaseResponse<?> saveFileToProject(SaveFileToProjectRequest request) {
        try {
            return projectServiceClient.saveFileToProject(request);

        } catch (Exception e) {
            log.error("Failed to copy attachment to project manager: {}", e.getMessage());
            return new BaseResponse<>(0, "Failed to save file to project", null);
        }
    }
}
package com.graduation.forumservice.service.impl;

import com.graduation.forumservice.client.ProjectServiceClient;
import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.exception.NotFoundException;
import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.request.*;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.*;
import com.graduation.forumservice.service.PostService;
import com.graduation.forumservice.service.SearchSyncService;
import com.graduation.forumservice.service.SequenceGeneratorService;
import com.graduation.forumservice.service.helper.ContentHelperService;
import com.graduation.forumservice.service.helper.ForumDTOMapper;
import com.graduation.forumservice.service.helper.UserInfoResolverService;
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
public class PostServiceImpl implements PostService {

    private final ForumPostRepository forumPostRepository;
    private final PostVoteRepository postVoteRepository;
    private final SavedPostRepository savedPostRepository;
    private final ProjectSavedPostRepository projectSavedPostRepository;
    private final PostStatsRepository postStatsRepository;
    private final ForumTagRepository forumTagRepository;
    private final MongoContentRepository mongoContentRepository;
    private final ForumPostFileRepository forumPostFileRepository;
    private final ForumAnswerRepository forumAnswerRepository;
    private final ProjectServiceClient projectServiceClient;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final UserInfoResolverService userInfoResolverService;
    private final ContentHelperService contentHelperService;
    private final ForumDTOMapper forumDTOMapper;
    private final ForumCommentRepository forumCommentRepository;
    private final SearchSyncService searchSyncService;

    @Override
    public BaseResponse<?> getPostFeed(Long userId, int page, int limit, String filter, String sort, String search) {
        log.info("Fetching forum feed for userId={}, filter={}, sort={}, page={}, limit={}", userId, filter, sort, page,
                limit);

        if (!isValidFilter(filter)) {
            log.warn("Invalid filter ({}) or sort ({}) parameter", filter, sort);
            return new BaseResponse<>(Constant.ERROR_STATUS, Constant.INVALID_PARAM, null);
        }

        Pageable pageable = PageRequest.of(page - 1, limit + 1);
        List<Object[]> rawResults = forumPostRepository.findFeedPostsNative(userId, filter.toUpperCase(), search,
                pageable);
        log.debug("Retrieved {} raw records from database", rawResults.size());

        boolean hasMore = rawResults.size() > limit;
        if (hasMore) {
            rawResults = rawResults.subList(0, limit);
        }

        List<PostFeedDTO> postDTOs = new ArrayList<>();
        for (Object[] row : rawResults) {
            postDTOs.add(forumDTOMapper.toPostFeedDTO(row));
        }

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
            String fullSearchText = contentHelperService.extractPlainText(request.getContent());

            // 2. Create the SHORT preview for the Main Table (Max 700)
            String shortPreview = (fullSearchText.length() > 700)
                    ? fullSearchText.substring(0, 700)
                    : fullSearchText;

            MongoContent mongoContent = new MongoContent();
            mongoContent.setContent(request.getContent());
            mongoContent.setId(sequenceGeneratorService.generateSequence("mongo_content_sequence"));
            mongoContent = mongoContentRepository.save(mongoContent);

            List<String> synchronizedTags = contentHelperService.syncTags(request.getTags());

            ForumPost post = ForumPost.builder()
                    .userId(userId)
                    .title(request.getTitle())
                    .plainTextPreview(shortPreview)
                    .mongoContentId(mongoContent.getId())
                    .tags(synchronizedTags)
                    .status(PostStatus.ACTIVE)
                    .isSolved(false)
                    .build();

            post = forumPostRepository.save(post);
            Long postId = post.getPostId();

            if (files != null && !files.isEmpty()) {
                BaseResponse<?> uploadResponse = projectServiceClient.uploadMultipleFiles(userId, 0L, files);

                if (uploadResponse.getStatus() == Constant.SUCCESS_STATUS) {
                    Map<String, Object> data = (Map<String, Object>) uploadResponse.getData();
                    List<String> fileUrls = (List<String>) data.get("urls");

                    List<ForumPostFile> postFiles = new ArrayList<>();
                    for (int i = 0; i < files.size(); i++) {
                        MultipartFile file = files.get(i);
                        String url = fileUrls.get(i);

                        postFiles.add(ForumPostFile.builder()
                                .postId(postId)
                                .storageRef(url)
                                .fileName(file.getOriginalFilename())
                                .extension(contentHelperService.getFileExtension(file.getOriginalFilename()))
                                .fileSize(file.getSize())
                                .build());
                    }
                    forumPostFileRepository.saveAll(postFiles);
                    log.info("Attached {} files to post {}", postFiles.size(), postId);
                } else {
                    log.error("File upload failed via ProjectService");
                }
            }

            searchSyncService.syncPost(
                    post.getPostId(),
                    userId,
                    post.getTitle(),
                    fullSearchText);

            log.info("Successfully created post with ID: {}", postId);
            PostAuthorDTO author = userInfoResolverService.fetchAuthorInfo(userId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post created successfully",
                    Map.of("post", forumDTOMapper.toPostFeedDTO(post, author, 0, 0L, 0)));

        } catch (Exception e) {
            log.error("Failed to create post with files: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updatePost(Long userId, Long postId, UpdatePostRequest request, List<MultipartFile> files) {
        log.info("Updating post with file management: postId={}, userId={}", postId, userId);

        try {
            ForumPost post = forumPostRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found!"));

            if (!post.getUserId().equals(userId)) {
                return new BaseResponse<>(Constant.ERROR_STATUS, "Unauthorized access", null);
            }

            // --- 1. Handle File Removal ---
            if (request.getFilesToRemove() != null && !request.getFilesToRemove().isEmpty()) {
                List<ForumPostFile> filesToDelete = forumPostFileRepository.findAllById(request.getFilesToRemove());
                List<String> storageRefs = filesToDelete.stream()
                        .map(ForumPostFile::getStorageRef)
                        .toList();
                projectServiceClient.deleteMultipleFiles(userId, storageRefs);
                forumPostFileRepository.deleteAllInBatch(filesToDelete);
                log.info("Removed {} old files from post {}", filesToDelete.size(), postId);
            }

            // --- 2. Handle File Upload ---
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
                                .extension(contentHelperService.getFileExtension(files.get(i).getOriginalFilename()))
                                .fileSize(files.get(i).getSize())
                                .build());
                    }
                    forumPostFileRepository.saveAll(newFiles);
                    log.info("Attached {} new files to post {}", newFiles.size(), postId);
                }
            }

            // --- 3. Handle Content & Search Text Logic (FIXED FOR MAP) ---
            String fullSearchText;

            if (request.getContent() != null) {
                // CASE A: User IS updating the content
                MongoContent mongoContent = mongoContentRepository.findByIntId(post.getMongoContentId())
                        .orElseThrow(() -> new NotFoundException("Content not found!"));

                // Update Mongo with the new Map
                mongoContent.setContent(request.getContent());
                mongoContentRepository.save(mongoContent);

                // Extract FULL text from the new Map
                fullSearchText = contentHelperService.extractPlainText(request.getContent());

                // Create SHORT preview for Postgres (Max 700 chars)
                String shortPreview = (fullSearchText.length() > 700)
                        ? fullSearchText.substring(0, 700)
                        : fullSearchText;

                post.setPlainTextPreview(shortPreview);

            } else {
                // CASE B: User is NOT updating content (Only Title/Files changed)
                // We must fetch the full content from Mongo to preserve the Search Index.

                MongoContent existingContent = mongoContentRepository.findByIntId(post.getMongoContentId())
                        .orElse(new MongoContent());

                // Retrieve the Map<String, Object>
                Map<String, Object> existingMap = existingContent.getContent();

                if (existingMap != null && !existingMap.isEmpty()) {
                    // Extract text from the existing Map
                    fullSearchText = contentHelperService.extractPlainText(existingMap);
                } else {
                    // Fallback to DB preview if Mongo is empty
                    fullSearchText = post.getPlainTextPreview();
                }
            }

            // --- 4. Handle Title & Tags ---
            if (request.getTitle() != null)
                post.setTitle(request.getTitle());

            if (request.getTags() != null)
                post.setTags(contentHelperService.syncTags(request.getTags()));

            post.setUpdatedAt(LocalDateTime.now());

            forumPostRepository.save(post);

            PostAuthorDTO author = userInfoResolverService.fetchAuthorInfo(userId);

            // --- 5. Sync to Search Index ---
            // Now 'fullSearchText' is guaranteed to be the full content (not truncated)
            searchSyncService.syncPost(
                    post.getPostId(),
                    userId,
                    post.getTitle(),
                    fullSearchText);

            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post updated successfully",
                    forumDTOMapper.toPostFeedDTO(post, author, 0, 0L, 0));

        } catch (Exception e) {
            log.error("Failed to update post {}: {}", postId, e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseResponse<?> getPostDetail(Long userId, Long postId) {
        log.info("Fetching post detail with files: postId={}", postId);

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

        List<FileUploadDTO> attachedFiles = forumPostFileRepository.findAllByPostId(postId)
                .stream()
                .map(forumDTOMapper::mapToFileDTO)
                .toList();

        List<AnswerDTO> answers = fetchTopAnswers(postId, userId);
        List<Long> answerIds = answers.stream().map(AnswerDTO::getAnswerId).toList();
        List<CommentDTO> rootComments = fetchRootComments(postId, answerIds);

        MongoContent postContent = mongoContentRepository.findByIntId(mongoContentId).orElse(new MongoContent());
        PostAuthorDTO postAuthor = userInfoResolverService.fetchAuthorInfo(authorUserId);

        Integer userVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId)
                .map(PostVote::getType).orElse(0);
        boolean isSaved = savedPostRepository.existsByPostIdAndUserId(postId, userId);
        List<Long> savedToProjectIds = projectSavedPostRepository.findByPostId(postId)
                .stream().map(item -> item.getId().getProjectId())
                .collect(Collectors.toList());

        PostDetailDTO detailDTO = PostDetailDTO.builder()
                .postId(postId)
                .title((String) data[2])
                .content(postContent.getContent())
                .author(postAuthor)
                .tags(tags)
                .files(attachedFiles)
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

    @Override
    @Transactional
    public BaseResponse<?> votePost(Long userId, Long postId, Integer voteType) {
        log.info("Processing vote: postId={}, userId={}, voteType={}", postId, userId, voteType);

        Optional<PostVote> existingVote = postVoteRepository.findPostVoteByPostIdAndUserId(postId, userId);
        int scoreChange;

        if (existingVote.isPresent()) {
            PostVote vote = existingVote.get();
            if (vote.getType().equals(voteType)) {
                scoreChange = -vote.getType();
                postVoteRepository.delete(vote);
                voteType = 0;
            } else {
                scoreChange = voteType - vote.getType();
                vote.setType(voteType);
                postVoteRepository.save(vote);
            }
        } else {
            scoreChange = voteType;
            PostVote newVote = PostVote.builder().postId(postId).userId(userId).type(voteType).build();
            postVoteRepository.save(newVote);
        }

        if (scoreChange != 0) {
            postStatsRepository.updateScore(postId, scoreChange);
        }

        checkAndHidePost(postId);

        Integer newTotalScore = postStatsRepository.findByPostId(postId).getScore();

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Your vote has been recorded",
                Map.of("newScore", newTotalScore, "userVote", voteType));
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSolveStatus(Long userId, Long postId, Boolean isSolved) {
        log.info("Updating solve status: postId={}, userId={}, isSolved={}", postId, userId, isSolved);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized solve status update attempt by userId={}", userId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Only the author can mark this post as solved", null);
        }

        post.setIsSolved(isSolved);
        forumPostRepository.save(post);

        log.info("Successfully updated solve status for post {}", postId);
        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post solve status updated successfully",
                Map.of("isSolved", isSolved));
    }

    @Override
    @Transactional
    public BaseResponse<?> deletePostStrict(Long userId, Long postId) {
        log.info("Attempting strict delete for post: postId={}, requestedBy={}", postId, userId);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt: userId {} tried to delete post {}", userId, postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You do not have permission to delete this post", null);
        }

        Integer answerCount = postStatsRepository.findAnswerCountByPostId(postId);

        if (answerCount != null && answerCount > 0) {
            log.warn("Delete blocked: Post {} has {} answers", postId, answerCount);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Cannot delete a question that has answers.", null);
        }

        try {
            savedPostRepository.deleteAllByPostId(postId);
            log.debug("Cleared private bookmarks for post {}", postId);

            projectSavedPostRepository.deleteAllBookmarksForPost(postId);
            log.debug("Cleared project space links for post {}", postId);

            postVoteRepository.deleteByPostId(postId);
            postStatsRepository.deleteById(postId);

            if (post.getMongoContentId() != null) {
                mongoContentRepository.deleteById(post.getMongoContentId());
            }

            forumPostRepository.delete(post);

            searchSyncService.deletePostIndex(postId);

            log.info("Deep delete complete for post {}", postId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Post deleted successfully", null);

        } catch (Exception e) {
            log.error("Failed to execute deep delete: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> updateSavePostStatus(Long userId, Long postId, UpdateSavePostStatusRequest request) {
        log.info("Processing save/unsave request: userId={}, postId={}, wannaSave={}", userId, postId,
                request.isWannaSave());

        if (!forumPostRepository.existsById(postId)) {
            log.warn("Save failed: Post {} not found", postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Post not found!", null);
        }

        try {
            boolean wannaSave = request.isWannaSave();

            if (request.getTarget().equals("PROJECT")) {
                if (request.getProjectId() == null) {
                    return new BaseResponse<>(Constant.ERROR_STATUS, "Project ID is required for project save", null);
                }
                handleProjectSave(userId, postId, request.getProjectId(), wannaSave);
            } else {
                handlePrivateSave(userId, postId, wannaSave);
            }

            log.info("Post {} {} successfully for user {}", postId, wannaSave ? "saved" : "unsaved", userId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS,
                    wannaSave ? "Post saved successfully" : "Post removed from saved", null);

        } catch (Exception e) {
            log.error("Error toggling save status: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public BaseResponse<?> searchTags(String query) {
        log.info("Searching for tags matching query: '{}'", query);

        if (query == null || query.trim().isEmpty()) {
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Search query too short", new ArrayList<>());
        }

        List<String> matchingTags = forumTagRepository.findByNameContainingIgnoreCase(query.trim()).stream()
                .map(obj -> ((ForumTag) obj).getName())
                .limit(10)
                .toList();

        log.info("Found {} tags matching '{}'", matchingTags.size(), query);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Tags retrieved successfully", Map.of("tags", matchingTags));
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

    @Override
    public BaseResponse<?> getSharedPostsByProject(Long projectId) {
        log.info("Internal: Fetching shared resources for projectId: {}", projectId);

        List<ProjectSavedPost> mappingRecords = projectSavedPostRepository.findByProjectId(projectId);

        if (mappingRecords.isEmpty()) {
            log.debug("No shared posts found for project {}", projectId);
            return new BaseResponse<>(1, "No resources shared with this project", List.of());
        }

        List<Long> postIds = mappingRecords.stream()
                .map(record -> record.getId().getPostId())
                .toList();

        List<PostFeedDTO> sharedPosts = postIds.stream()
                .map(pId -> {
                    Optional<Object[]> rawResult = forumPostRepository.findPostDetailByIdNative(pId);
                    return rawResult.map(result -> forumDTOMapper.toPostFeedDTO((Object[]) result[0])).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Successfully retrieved {} shared posts for project {}", sharedPosts.size(), projectId);

        return new BaseResponse<>(1, "Shared resources retrieved successfully", sharedPosts);
    }

    // =========== Private Helper Methods ===========

    private boolean isValidFilter(String filter) {
        return List.of("ALL", "MY_POSTS", "SAVED_POSTS", "MOST_HELPFUL").contains(filter.toUpperCase());
    }

    private void handlePrivateSave(Long userId, Long postId, boolean wannaSave) {
        Optional<SavedPost> existing = savedPostRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent() && !wannaSave) {
            savedPostRepository.delete(existing.get());
        } else if (existing.isEmpty() && wannaSave) {
            SavedPost newSave = SavedPost.builder().userId(userId).postId(postId).createdAt(LocalDateTime.now())
                    .build();
            savedPostRepository.save(newSave);
        }
    }

    private void handleProjectSave(Long userId, Long postId, Long projectId, boolean wannaSave) {
        Optional<ProjectSavedPost> existing = projectSavedPostRepository.findByProjectIdAndPostId(projectId, postId);

        if (existing.isPresent() && !wannaSave) {
            projectSavedPostRepository.delete(existing.get());
        } else if (existing.isEmpty() && wannaSave) {
            ProjectSavedPost projectSave = ProjectSavedPost.builder()
                    .id(new ProjectSavedPostId(projectId, postId))
                    .savedByUserId(userId)
                    .savedAt(LocalDateTime.now())
                    .build();
            projectSavedPostRepository.save(projectSave);
        }
    }

    private List<AnswerDTO> fetchTopAnswers(Long postId, Long userId) {
        Pageable answerPageable = PageRequest.of(0, 5);
        List<Object[]> answerRows = forumAnswerRepository.findAnswersByPostIdNative(postId, "MOST_HELPFUL",
                answerPageable);

        return answerRows.stream().map(row -> {
            Object[] a = (row[0] instanceof Object[]) ? (Object[]) row[0] : row;
            Long answerId = ((Number) a[0]).longValue();

            return AnswerDTO.builder()
                    .answerId(answerId)
                    .author(userInfoResolverService.fetchAuthorInfo(((Number) a[1]).longValue()))
                    .content(mongoContentRepository.findByIntId(Integer.parseInt(String.valueOf(a[3])))
                            .map(MongoContent::getContent).orElse(null))
                    .score(((Number) a[9]).intValue())
                    .isAccepted((Boolean) a[6])
                    .voteType(forumAnswerRepository.findUserVoteTypeOnAnswer(answerId, userId))
                    .createdAt(((java.sql.Timestamp) a[7]).toLocalDateTime())
                    .build();
        }).toList();
    }

    private List<CommentDTO> fetchRootComments(Long postId, List<Long> answerIds) {
        Pageable limit = PageRequest.of(0, 2);

        // Fetch comments belonging directly to the Post
        List<CommentDTO> postComments = forumCommentRepository.findByPostIdOrderByCreatedAtAsc(postId, limit)
                .stream().map(forumDTOMapper::toCommentDTO).toList();
        List<CommentDTO> allComments = new ArrayList<>(postComments);

        // Fetch comments for each Answer
        for (Long answerId : answerIds) {
            List<CommentDTO> answerComments = forumCommentRepository.findByAnswerIdOrderByCreatedAtAsc(answerId, limit)
                    .stream().map(forumDTOMapper::toCommentDTO).toList();
            allComments.addAll(answerComments);
        }

        // Sort the final flat list by creation date
        return allComments.stream()
                .sorted(Comparator.comparing(CommentDTO::getCreatedAt))
                .toList();
    }

    private void checkAndHidePost(Long postId) {
        long upvotes = postVoteRepository.countUpvotes(postId);
        long downvotes = postVoteRepository.countDownvotes(postId);
        long totalVotes = upvotes + downvotes;

        if (totalVotes >= 10 && downvotes > upvotes) {
            long positiveAnswerCount = forumAnswerRepository.countPositiveAnswers(postId);

            if (positiveAnswerCount == 0) {
                ForumPost post = forumPostRepository.findById(postId)
                        .orElseThrow(() -> new NotFoundException("Post not found"));

                if (post.getStatus() != PostStatus.HIDDEN) {
                    log.info("Auto-hiding Post {} due to negative feedback and no helpful answers.", postId);
                    post.setStatus(PostStatus.HIDDEN);
                    forumPostRepository.save(post);
                }
            }
        }
    }
}

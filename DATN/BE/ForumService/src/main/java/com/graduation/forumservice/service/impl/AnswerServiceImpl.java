package com.graduation.forumservice.service.impl;

import com.graduation.forumservice.config.KafkaConfig;
import com.graduation.forumservice.constant.Constant;
import com.graduation.forumservice.event.ForumActivityEvent;
import com.graduation.forumservice.exception.NotFoundException;
import com.graduation.forumservice.model.*;
import com.graduation.forumservice.payload.request.CreateAnswerRequest;
import com.graduation.forumservice.payload.request.EditAnswerRequest;
import com.graduation.forumservice.payload.response.*;
import com.graduation.forumservice.repository.*;
import com.graduation.forumservice.service.AnswerService;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final ForumAnswerRepository forumAnswerRepository;
    private final ForumPostRepository forumPostRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final ForumCommentRepository forumCommentRepository;
    private final MongoContentRepository mongoContentRepository;
    private final PostStatsRepository postStatsRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final UserInfoResolverService userInfoResolverService;
    private final ContentHelperService contentHelperService;
    private final ForumDTOMapper forumDTOMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SearchSyncService searchSyncService;

    @Override
    public BaseResponse<?> getAnswersForPost(Long postId, int page, int limit, String sort) {
        log.info("Fetching answers for postId={}, sort={}, page={}, limit={}", postId, sort, page, limit);

        if (!List.of("NEWEST", "MOST_HELPFUL").contains(sort)) {
            log.warn("Invalid answer sort parameter: {}", sort);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Invalid sort parameter", null);
        }

        Pageable pageable = PageRequest.of(page - 1, limit + 1);
        List<Object[]> rawResults = forumAnswerRepository.findAnswersByPostIdNative(postId, sort, pageable);

        boolean hasMore = rawResults.size() > limit;
        if (hasMore) {
            rawResults = rawResults.subList(0, limit);
        }

        List<AnswerDTO> answerDTOs = new ArrayList<>();
        for (Object[] row : rawResults) {
            answerDTOs.add(forumDTOMapper.toAnswerDTO(row));
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

        if (!forumPostRepository.existsById(postId)) {
            log.warn("Answer submission failed: Post {} not found", postId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Post not found!", null);
        }

        try {
            // 1. Extract FULL text (Unlimited length)
            String fullSearchText = contentHelperService.extractPlainText(request.getContent());

            // 2. Create SHORT preview for the main database (Max 700 chars)
            String shortPreview = (fullSearchText.length() > 700)
                    ? fullSearchText.substring(0, 700)
                    : fullSearchText;


            ForumPost post = forumPostRepository.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found!"));

            MongoContent mongoContent = new MongoContent();
            mongoContent.setContent(request.getContent());
            mongoContent.setId(sequenceGeneratorService.generateSequence("mongo_content_sequence"));
            mongoContent = mongoContentRepository.save(mongoContent);

            ForumAnswer answer = ForumAnswer.builder()
                    .postId(postId)
                    .userId(userId)
                    .status(ForumAnswer.AnswerStatus.ACTIVE)
                    .plainTextPreview(shortPreview)
                    .mongoContentId(mongoContent.getId())
                    .isAccepted(false)
                    .upvoteCount(0)
                    .downvoteCount(0)
                    .build();

            answer = forumAnswerRepository.save(answer);
            Long answerId = answer.getAnswerId();

            postStatsRepository.incrementAnswerCount(postId);

            if (!post.getUserId().equals(userId)) {
                PostAuthorDTO actor = userInfoResolverService.fetchAuthorInfo(userId);

                ForumActivityEvent event = new ForumActivityEvent(
                        userId,
                        actor.getName(),
                        actor.getAvatar(),
                        post.getUserId(),
                        postId,
                        post.getTitle(),
                        answerId,
                        null,
                        ForumActivityEvent.ForumEventType.ANSWER_ON_POST);

                sendKafkaNotification(event);
            }

            searchSyncService.syncAnswer(
                    answerId,
                    postId,
                    userId,
                    fullSearchText // We already extracted this earlier in your code
            );

            log.info("Successfully submitted answer {} for post {}", answerId, postId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer submitted successfully",
                    Map.of("answerId", answerId));

        } catch (Exception e) {
            log.error("Failed to submit answer: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> editAnswer(Long userId, Long answerId, EditAnswerRequest request) {
        log.info("Editing answer: answerId={}, userId={}", answerId, userId);

        ForumAnswer answer = forumAnswerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found!"));

        if (!answer.getUserId().equals(userId)) {
            log.warn("Unauthorized edit attempt: userId {} on answerId {}", userId, answerId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "You can only edit your own answers!", null);
        }

        try {
            int mongoId = answer.getMongoContentId();

            MongoContent mongoContent = mongoContentRepository.findByIntId(mongoId)
                    .orElseThrow(() -> new NotFoundException("Content not found in MongoDB!"));

            mongoContent.setContent(request.getContent());
            mongoContentRepository.save(mongoContent);

            // 1. Extract FULL text
            String fullSearchText = contentHelperService.extractPlainText(request.getContent());

            // 2. Create SHORT preview
            String shortPreview = (fullSearchText.length() > 700)
                    ? fullSearchText.substring(0, 700)
                    : fullSearchText;

            answer.setPlainTextPreview(shortPreview);
            answer.setUpdatedAt(LocalDateTime.now());

            forumAnswerRepository.save(answer);
            // === 3. UPDATE SEARCH INDEX ===
            searchSyncService.syncAnswer(
                    answerId,
                    answer.getPostId(),
                    userId,
                    fullSearchText
            );

            log.info("Successfully updated answer ID: {}", answerId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer updated successfully", null);

        } catch (Exception e) {
            log.error("Failed to edit answer: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> switchAnswerAcceptStatus(Long userId, Long answerId) {
        log.info("Attempting to switch answer accept status: answerId={}, by userId={}", answerId, userId);

        ForumAnswer answer = forumAnswerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found!"));

        ForumPost post = forumPostRepository.findById(answer.getPostId())
                .orElseThrow(() -> new NotFoundException("Post not found!"));

        if (!post.getUserId().equals(userId)) {
            log.warn("Unauthorized switch: userId {} is not the author of post {}", userId, post.getPostId());
            return new BaseResponse<>(Constant.ERROR_STATUS,
                    "Only the author of the post can switch accept status of an answer.", null);
        }

        try {
            boolean currentlyAccepted = Boolean.TRUE.equals(answer.getIsAccepted());
            boolean targetState = !currentlyAccepted;

            if (targetState) {
                forumAnswerRepository.unmarkAcceptedAnswersForPost(post.getPostId());
            }

            answer.setIsAccepted(targetState);
            forumAnswerRepository.save(answer);

            post.setIsSolved(targetState);
            forumPostRepository.save(post);

            log.info("Answer {} is now isAccepted={} for post {}", answerId, targetState, post.getPostId());
            return new BaseResponse<>(Constant.SUCCESS_STATUS,
                    "Answer accepted status updated successfully",
                    Map.of("postId", post.getPostId(), "answerId", answerId, "isAccepted", targetState));

        } catch (Exception e) {
            log.error("Failed to update answer status: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteAnswer(Long userId, Long answerId) {
        log.info("Executing deep delete for answer: answerId={}, requestedBy={}", answerId, userId);

        ForumAnswer answer = forumAnswerRepository.findById(answerId)
                .orElseThrow(() -> new NotFoundException("Answer not found!"));

        if (!answer.getUserId().equals(userId)) {
            log.warn("Unauthorized delete attempt by userId {}", userId);
            return new BaseResponse<>(Constant.ERROR_STATUS, "Permission denied", null);
        }

        try {
            answerVoteRepository.deleteByAnswerId(answerId);
            log.debug("Cleared votes for answer {}", answerId);

            forumCommentRepository.deleteAllByAnswerId(answerId);
            log.debug("Cleared all comments for answer {}", answerId);

            if (answer.getMongoContentId() != null) {
                mongoContentRepository.deleteById(answer.getMongoContentId());
            }

            postStatsRepository.decrementAnswerCount(answer.getPostId());

            forumAnswerRepository.delete(answer);

            searchSyncService.deleteAnswerIndex(answerId);

            log.info("Deep delete complete for answer {}", answerId);
            return new BaseResponse<>(Constant.SUCCESS_STATUS, "Answer and associated data removed", null);

        } catch (Exception e) {
            log.error("Deep delete failed: {}", e.getMessage());
            throw e;
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
                if (voteType == 1)
                    upvoteChange = -1;
                else
                    downvoteChange = -1;

                answerVoteRepository.delete(vote);
                voteType = 0;
            } else {
                if (voteType == 1) {
                    upvoteChange = 1;
                    downvoteChange = -1;
                } else {
                    upvoteChange = -1;
                    downvoteChange = 1;
                }
                vote.setType(voteType);
                answerVoteRepository.save(vote);
            }
        } else {
            if (voteType == 1)
                upvoteChange = 1;
            else
                downvoteChange = 1;

            AnswerVote newVote = AnswerVote.builder().answerId(answerId).userId(userId).type(voteType).build();
            answerVoteRepository.save(newVote);
        }

        forumAnswerRepository.updateAnswerVoteCounts(answerId, upvoteChange, downvoteChange);

        Optional<ForumAnswer> optionalUpdatedAnswer = forumAnswerRepository.findById(answerId);
        if (optionalUpdatedAnswer.isEmpty()) {
            throw new NotFoundException("Answer not found!");
        }
        ForumAnswer updatedAnswer = optionalUpdatedAnswer.get();
        checkAndHideAnswer(updatedAnswer);

        return new BaseResponse<>(Constant.SUCCESS_STATUS, "Vote recorded",
                Map.of("newScore", updatedAnswer.getScore(),
                        "upvoteCount", updatedAnswer.getUpvoteCount(),
                        "downvoteCount", updatedAnswer.getDownvoteCount(),
                        "userVote", voteType));
    }

    // =========== Private Helper Methods ===========

    private void checkAndHideAnswer(ForumAnswer answer) {
        long upvotes = answer.getUpvoteCount();
        long downvotes = answer.getDownvoteCount();
        long totalVotes = upvotes + downvotes;

        if (totalVotes >= 10 && downvotes > upvotes) {
            if (answer.getStatus() != ForumAnswer.AnswerStatus.HIDDEN) {
                log.info("Auto-hiding Answer {} due to negative community feedback.", answer.getAnswerId());
                answer.setStatus(ForumAnswer.AnswerStatus.HIDDEN);
                forumAnswerRepository.save(answer);
            }
        }
    }

    private void sendKafkaNotification(ForumActivityEvent event) {
        try {
            kafkaTemplate.send(KafkaConfig.TOPIC_FORUM_ACTIVITY, event);
            log.info("Sent forum activity Kafka event: {} by user {}", event.getType(), event.getActorId());
        } catch (Exception e) {
            log.error("Failed to send Kafka notification: {}", e.getMessage());
        }
    }
}

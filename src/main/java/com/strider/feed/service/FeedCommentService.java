package com.strider.feed.service;

import com.strider.feed.client.UserProfileClient;
import com.strider.feed.model.TargetType;
import com.strider.feed.model.entity.FeedComment;
import com.strider.feed.model.request.*;
import com.strider.feed.repository.FeedCommentRepository;
import com.strider.feed.repository.FeedLikeRepository;
import com.strider.feed.repository.FeedPostRepository;
import com.strider.feed.util.EnvelopeUtils;
import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import com.strider.feed.model.response.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCommentService {
    private final FeedCommentRepository commentRepository;
    private final FeedPostRepository postRepository;
    private final FeedLikeRepository likeRepository;
    private final UserProfileClient userProfileClient;
    private final com.strider.feed.kafka.producer.NotificationEventProducer notificationEventProducer;

    @Transactional
    public FeedCommentResult createComment(String userId, FeedCommentRequest request){
        // null 또는 공백만 있는 경우 예외 처리
        if (request.commentText() == null || request.commentText().isBlank()) {
            throw new StriderException(StriderErrorCodes.BAD_REQUEST);
        }

        // 존재하지 않는 피드일 때 예외 처리
        com.strider.feed.model.entity.FeedPost post = postRepository.findByFeedPostIdAndIsDeletedFalse(request.feedPostId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        String parentId = request.parentCommentId();
        String parentCommentAuthorId = null;
        if(parentId != null && !parentId.isBlank()){
            // 부모 댓글이 없을 때 예외 처리
            FeedComment parentComment = commentRepository
                    .findByCommentIdAndIsDeletedFalse(request.parentCommentId())
                    .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

            // parent가 같은 피드에 속하는지 확인
            if(!parentComment.getFeedPostId().equals(request.feedPostId())){
                throw new StriderException(StriderErrorCodes.BAD_REQUEST);
            }

            // parent가 최상위 댓글인지 확인
            if(parentComment.getParentCommentId() != null){
                throw new StriderException(StriderErrorCodes.BAD_REQUEST);
            }

            parentCommentAuthorId = parentComment.getUserId();
        } else {
            parentId = null;
        }

        FeedComment feedComment = FeedComment.builder()
                .feedPostId(request.feedPostId())
                .parentCommentId(parentId)
                .userId(userId)
                .commentText(request.commentText().trim())
                .build();

        FeedComment savedFeedComment = commentRepository.saveAndFlush(feedComment);

        // 알림 발행
        if(parentId == null){
            // F2: 게시물 댓글 → 게시물 작성자 (본인 댓글 제외)
            if(!post.getUserId().equals(userId)){
                notificationEventProducer.send(new com.strider.feed.kafka.event.NotificationEvent(
                        com.strider.feed.kafka.event.NotificationEvent.TYPE_COMMENT,
                        post.getUserId(),
                        userId,
                        request.feedPostId(),
                        "회원님의 피드에 댓글을 남겼어요!",
                        LocalDateTime.now()
                ));
            }
        } else {
            // F3: 대댓글(답글) → 부모 댓글 작성자 (본인 답글 제외)
            if(parentCommentAuthorId != null && !parentCommentAuthorId.equals(userId)){
                notificationEventProducer.send(new com.strider.feed.kafka.event.NotificationEvent(
                        com.strider.feed.kafka.event.NotificationEvent.TYPE_COMMENT,
                        parentCommentAuthorId,
                        userId,
                        request.feedPostId(),
                        "회원님의 댓글에 답글을 남겼어요!",
                        LocalDateTime.now()
                ));
            }
        }

        return FeedCommentResult.builder()
                .commentId(savedFeedComment.getCommentId())
                .feedPostId(savedFeedComment.getFeedPostId())
                .userId(savedFeedComment.getUserId())
                .parentCommentId(savedFeedComment.getParentCommentId())
                .commentText(savedFeedComment.getCommentText())
                .likeCount(savedFeedComment.getLikeCount())
                .userLike(false)
                .replyCount(0)
                .createdAt(savedFeedComment.getCreatedAt())
                .updatedAt(savedFeedComment.getUpdatedAt())
                .build();
    }

    public List<FeedCommentResponse> findCommentsByFeed(HttpHeaders headers, FeedCommentQuery request){
        postRepository.findByFeedPostIdAndIsDeletedFalse(request.feedId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        List<FeedComment> comments;

        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())){
            comments = commentRepository.findCommentsFirst(request.feedId(), PageRequest.of(0, request.size()));
        } else {
            comments = commentRepository.findCommentsAfterCursor(request.feedId(), request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        return feedCommentToFeedCommentResponse(headers, comments, request.userId());
    }

    public List<FeedCommentResponse> findRepliesByComment(HttpHeaders headers, CommentReplyQuery request){
        // TODO: 피드 삭제 시 댓글도 같은 트랜잭션에서 삭제를 안 한다면, 피드 존재 여부도 체크 필요
//        postRepository.findByFeedPostIdAndIsDeletedFalse(request.feedId())
//                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        commentRepository.findByCommentIdAndIsDeletedFalse(request.parentCommentId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        List<FeedComment> replies;

        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())){
            replies = commentRepository.findRepliesFirst(request.parentCommentId(), PageRequest.of(0, request.size()));
        } else {
            replies = commentRepository.findRepliesAfterCursor(request.parentCommentId(), request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        return feedCommentToFeedCommentResponse(headers, replies, request.userId());
    }

    private List<FeedCommentResponse> feedCommentToFeedCommentResponse(HttpHeaders headers, List<FeedComment> comments, String userId) {
        if(comments.isEmpty()) return List.of();

        List<String> commentIds = comments.stream()
                .map(FeedComment::getCommentId)
                .toList();

        Set<String> authorIdsSet = comments.stream()
                .map(FeedComment::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        List<String> authorIds = authorIdsSet.stream().toList();

        Map<String, Integer> replyCountMap = commentRepository.countRepliesGroupByParentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        FeedCommentRepository.ReplyCountRow::getParentId,
                        r -> (int) r.getCnt()
                ));

        Set<String> likedCommentIdSet = new HashSet<>(likeRepository.findLikedCommentIds(userId, commentIds));

        UserProfileListRequest userProfileListRequest = new UserProfileListRequest(authorIds);
        List<SimpleUserProfileResponse> userProfiles = EnvelopeUtils.getOrThrow(userProfileClient.getSimpleUserProfileList(userProfileListRequest), StriderErrorCodes.INTERNAL_SERVER_ERROR);

        Map<String, SimpleUserProfileResponse> userProfileMap = userProfiles.stream()
                .collect(Collectors.toMap(
                        SimpleUserProfileResponse::userId,
                        p -> p,
                        (a, b) -> a
                ));

        return comments.stream()
                .map(comment ->
                {
                    String commentId = comment.getCommentId();

                    int replyCount = replyCountMap.getOrDefault(commentId, 0);
                    boolean userLike = likedCommentIdSet.contains(commentId);

                    SimpleUserProfileResponse userProfile = userProfileMap.get(comment.getUserId());

                    return FeedCommentResponse.builder()
                            .commentId(comment.getCommentId())
                            .feedPostId(comment.getFeedPostId())
                            .userId(comment.getUserId())
                            .parentCommentId(comment.getParentCommentId())
                            .userProfileImg(userProfile.profileThumbImage())
                            .userNickname(userProfile.nickname())
                            .commentText(comment.getCommentText())
                            .likeCount(comment.getLikeCount())
                            .userLike(userLike)
                            .replyCount(replyCount)
                            .createdAt(comment.getCreatedAt())
                            .updatedAt(comment.getUpdatedAt())
                            .build();
                }).toList();
    }

       @Transactional
    public void deleteComment(String userId, String commentId) {
        FeedComment feedComment = commentRepository
                .findByCommentIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        // 작성자만 삭제 가능
        if(!feedComment.getUserId().equals(userId)){
            throw new StriderException(StriderErrorCodes.FORBIDDEN);
        }

        feedComment.setDeleted(true);
        feedComment.setDeletedAt(LocalDateTime.now());

        // TODO: 추후 대댓글 관련 처리
    }

    @Transactional
    public FeedLikeResponse likeComment(FeedLikeRequest request){
        // TODO: 피드 삭제 시 댓글도 같은 트랜잭션에서 삭제를 안 한다면, 피드 존재 여부도 체크 필요
        if (request.targetType() != TargetType.COMMENT) {
            throw new StriderException(StriderErrorCodes.BAD_REQUEST);
        }

        FeedComment comment = commentRepository.findByCommentIdAndIsDeletedFalse(request.targetId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int insertedCnt = likeRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                request.userId(),
                request.targetType().name(),
                request.targetId()
        );

        // 좋아요가 새로 눌린 경우
        if(insertedCnt == 1){
            int updatedCnt = commentRepository.incLikeCount(request.targetId());
            // 댓글이 없거나 삭제된 경우
            if(updatedCnt == 0) {
                throw new StriderException(StriderErrorCodes.NOT_FOUND);
            }

            // F4: 댓글 좋아요 → 댓글 작성자 (본인 좋아요 제외)
            if(!comment.getUserId().equals(request.userId())){
                notificationEventProducer.send(new com.strider.feed.kafka.event.NotificationEvent(
                        com.strider.feed.kafka.event.NotificationEvent.TYPE_COMMENT_LIKE,
                        comment.getUserId(),
                        request.userId(),
                        request.targetId(),
                        "회원님의 댓글에 좋아요를 눌렀어요!",
                        LocalDateTime.now()
                ));
            }
        }

        return FeedLikeResponse.builder()
                .targetType(request.targetType())
                .targetId(request.targetId())
                .liked(true)
                .build();
    }

    @Transactional
    public FeedLikeResponse unlikeComment(FeedLikeRequest request){
        // TODO: 피드 삭제 시 댓글도 같은 트랜잭션에서 삭제를 안 한다면, 피드 존재 여부도 체크 필요
        if (request.targetType() != TargetType.COMMENT) {
            throw new StriderException(StriderErrorCodes.BAD_REQUEST);
        }

        commentRepository.findByCommentIdAndIsDeletedFalse(request.targetId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int deletedCnt = likeRepository.deleteIfExists(request.userId(), request.targetType().name(), request.targetId());

        System.out.println(deletedCnt);
        // 좋아요를 삭제한 경우
        if(deletedCnt == 1){
            int updatedCnt = commentRepository.decLikeCount(request.targetId());
            System.out.println(updatedCnt);
            // 댓글이 없거나 삭제된 경우
            if(updatedCnt == 0) {
                throw new StriderException(StriderErrorCodes.NOT_FOUND);
            }
        }

        return FeedLikeResponse.builder()
                .targetType(request.targetType())
                .targetId(request.targetId())
                .liked(false)
                .build();
    }

    private Boolean isFirstSearch(LocalDateTime cursorCreatedAt, String cursorId){
        return cursorCreatedAt == null || cursorId == null;
    }
}
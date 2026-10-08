package com.strider.feed.service;

import com.strider.feed.client.UserProfileClient;
import com.strider.feed.model.TargetType;
import com.strider.feed.model.entity.FeedComment;
import com.strider.feed.model.entity.FeedMedia;
import com.strider.feed.model.entity.FeedPost;
import com.strider.feed.model.request.*;
import com.strider.feed.repository.*;
import com.strider.feed.util.EnvelopeUtils;
import com.strider.strider_common_lib.error.StriderErrorCodes;
import com.strider.strider_common_lib.exception.StriderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import com.strider.feed.model.response.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedService {
    @Value("${cloudfront.domain}")
    private String cloudFrontDomain;

    private final FeedPostRepository postRepository;
    private final FeedLikeRepository likeRepository;
    private final FeedSaveRepository saveRepository;
    private final FeedCommentRepository commentRepository;
    private final FeedMediaRepository mediaRepository;
    private final UserProfileClient userProfileClient;
    private final com.strider.feed.kafka.producer.NotificationEventProducer notificationEventProducer;

    @Transactional
    public FeedLikeResponse likeFeed(FeedLikeRequest request){
        if (request.targetType() != TargetType.FEED) {
            throw new StriderException(StriderErrorCodes.BAD_REQUEST);
        }

        // 대상 피드 존재 확인
        FeedPost post = postRepository.findByFeedPostIdAndIsDeletedFalse(request.targetId())
                    .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int insertedCnt = likeRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                request.userId(),
                request.targetType().name(),
                request.targetId()
        );

        // 좋아요가 새로 눌린 경우
        if(insertedCnt == 1){
            int updatedCnt = postRepository.incLikeCount(request.targetId());
            if(updatedCnt == 0){
                // 피드가 없거나 삭제된 경우
                throw new StriderException(StriderErrorCodes.NOT_FOUND);
            }

            // F1: 피드 좋아요 알림 (본인 좋아요는 제외)
            if(!post.getUserId().equals(request.userId())){
                notificationEventProducer.send(new com.strider.feed.kafka.event.NotificationEvent(
                        com.strider.feed.kafka.event.NotificationEvent.TYPE_LIKE,
                        post.getUserId(),
                        request.userId(),
                        request.targetId(),
                        "회원님의 피드에 좋아요를 눌렀어요!",
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
    public FeedLikeResponse unLikeFeed(FeedLikeRequest request){
        if (request.targetType() != TargetType.FEED) {
            throw new StriderException(StriderErrorCodes.BAD_REQUEST);
        }

        // 대상 피드 존재 확인
        postRepository.findByFeedPostIdAndIsDeletedFalse(request.targetId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int deletedCnt = likeRepository.deleteIfExists(request.userId(), request.targetType().name(), request.targetId());

        if(deletedCnt == 1){
            int updatedCnt = postRepository.decLikeCount(request.targetId());
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

    @Transactional
    public FeedSaveResponse saveFeed(FeedSaveRequest request) {
        // 대상 피드 존재 확인
        postRepository.findByFeedPostIdAndIsDeletedFalse(request.targetId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int insertedCnt = saveRepository.insertIfAbsent(
                UUID.randomUUID().toString(),
                request.userId(),
                request.targetId()
        );

        // 좋아요가 새로 눌린 경우
        if(insertedCnt == 1){
            int updatedCnt = postRepository.incSaveCount(request.targetId());
            if(updatedCnt == 0){
                // 피드가 없거나 삭제된 경우
                throw new StriderException(StriderErrorCodes.NOT_FOUND);
            }
        }

        return FeedSaveResponse.builder()
                .targetId(request.targetId())
                .saved(true)
                .build();
    }

    @Transactional
    public FeedSaveResponse unSaveFeed(FeedSaveRequest request) {
        // 대상 피드 존재 확인
        postRepository.findByFeedPostIdAndIsDeletedFalse(request.targetId())
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        int deletedCnt = saveRepository.deleteIfExists(request.userId(), request.targetId());

        if(deletedCnt == 1){
            int updatedCnt = postRepository.decSaveCount(request.targetId());
            if(updatedCnt == 0) {
                throw new StriderException(StriderErrorCodes.NOT_FOUND);
            }
        }

        return FeedSaveResponse.builder()
                .targetId(request.targetId())
                .saved(false)
                .build();
    }

    @Transactional
    public FeedPostResponse createFeed(String userId, FeedPostRequest request) {
        // TODO: 외부(feign) 호출은 transaction 외부에서 따로 호출하는게 좋음
        UserProfileResponse userProfile = EnvelopeUtils.getOrThrow(userProfileClient.getUserProfile(new UserProfileRequest(userId)), StriderErrorCodes.NOT_FOUND);

        FeedPost feedPost = FeedPost.builder()
                .exerciseId(request.exerciseId())
                .review(request.review())
                .userId(userId)
                .build();

        FeedPost newFeedPost = postRepository.save(feedPost);
        List<FeedMedia> savedMediaList = new ArrayList<>();

        if(request.mediaList() != null && !request.mediaList().isEmpty()){
            List<FeedMedia> mediaList = new ArrayList<>();

            for(int i=0; i<request.mediaList().size(); i++){
                var media = request.mediaList().get(i);
                mediaList.add(FeedMedia.builder()
                                .feedPostId(newFeedPost.getFeedPostId())
                                .sequence(media.sequence() == null ? i : media.sequence())
                                .mediaType(media.mediaType())
                                .fileUrl(media.fileUrl())
                                .thumbnailUrl(media.thumbnailUrl())
                                .build());
            }
            savedMediaList = mediaRepository.saveAll(mediaList);
        }

        // F5: 신규 게시물 → 작성자의 팔로워들에게 알림 (발행 실패해도 게시물 생성은 정상)
        try {
            List<UserFollowResponse> followers = EnvelopeUtils.getOrThrow(
                    userProfileClient.getFollowerIdList(userId), StriderErrorCodes.INTERNAL_SERVER_ERROR);
            String content = userProfile.nickname() + "님이 새 게시물을 올렸어요!";
            for (UserFollowResponse follower : followers) {
                if (follower.userId() == null || follower.userId().equals(userId)) continue;
                notificationEventProducer.send(new com.strider.feed.kafka.event.NotificationEvent(
                        com.strider.feed.kafka.event.NotificationEvent.TYPE_POST,
                        follower.userId(),
                        userId,
                        newFeedPost.getFeedPostId(),
                        content,
                        LocalDateTime.now()
                ));
            }
        } catch (Exception e) {
            log.warn("F5 신규 게시물 알림 발행 실패 (게시물 생성은 정상): {}", e.getMessage());
        }

        return FeedPostResponse.builder()
                .feedPostId(newFeedPost.getFeedPostId())
                .exerciseId(newFeedPost.getExerciseId())
                .review(newFeedPost.getReview())
                .userId(newFeedPost.getUserId())
                .userProfileImg(userProfile.profileImage())
                .userNickname(userProfile.nickname())
                .likeCount(newFeedPost.getLikeCount())
                .saveCount(newFeedPost.getSaveCount())
                .commentCount(0)
                .createdAt(newFeedPost.getCreatedAt())
                .updatedAt(newFeedPost.getUpdatedAt())
                .mediaList(savedMediaList)
                .userLike(false)
                .userSave(false)
                .build();
    }

    public List<FeedPostResponse> findFollowerFeeds(HttpHeaders headers, UserFeedQuery request) {
        List<UserFollowResponse> followingUsers = EnvelopeUtils.getOrThrow(userProfileClient.getFollowingIdList(request.userId()), StriderErrorCodes.INTERNAL_SERVER_ERROR);

        List<String> followingUserIds = followingUsers.stream().map(UserFollowResponse::userId).toList();

        List<FeedPost> posts;

        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())) {
            posts = postRepository.findFollowFeedsFirst(followingUserIds, PageRequest.of(0, request.size()));
        } else {
            posts = postRepository.findFollowFeedsAfterCursor(followingUserIds, request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        return postsToPostResponses(request.userId(), posts);
    }

    public List<FeedPostResponse> findUserFeeds(HttpHeaders headers, UserFeedQuery request) {
        // 피드 사용자 계정이 비공개인 경우, 팔로우 여부 확인
        Boolean isFeedUserPublic = EnvelopeUtils.getOrThrow(userProfileClient.isUserPublic(request.feedUserId()), StriderErrorCodes.INTERNAL_SERVER_ERROR);

        // TODO: follow 관련 api 수정에 따라 변경해야될 수도
        if(!isFeedUserPublic) {
            List<UserFollowResponse> followingUsers = EnvelopeUtils.getOrThrow(userProfileClient.getFollowerIdList(request.feedUserId()), StriderErrorCodes.INTERNAL_SERVER_ERROR);

            boolean isFollower = followingUsers.stream().anyMatch(f -> f.userId().equals(request.userId()));

            if(!isFollower) throw new StriderException(StriderErrorCodes.UNAUTHORIZED);
        }

        List<FeedPost> posts;
        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())) {
            posts = postRepository.findUserFeedsFirst(request.feedUserId(), PageRequest.of(0, request.size()));
        } else {
            posts = postRepository.findUserFeedsAfterCursor(request.feedUserId(), request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        return postsToPostResponses(request.userId(), posts);
    }

    public long countUserFeeds(String userId){
        return postRepository.countByUserIdAndIsDeletedFalse(userId);
    }

    public List<FeedPostResponse> findMyFeeds(UserFeedQuery request){
        List<FeedPost> posts;

        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())) {
            posts = postRepository.findUserFeedsFirst(request.userId(), PageRequest.of(0, request.size()));
        } else {
            posts = postRepository.findUserFeedsAfterCursor(request.userId(), request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        return postsToPostResponses(request.userId(), posts);
    }

    private List<FeedPostResponse> postsToPostResponses(String userId, List<FeedPost> posts){
        if(posts.isEmpty()) return List.of();

        List<String> feedPostIds = posts.stream().map(FeedPost::getFeedPostId).toList();
        List<String> writerIds = posts.stream().map(FeedPost::getUserId).distinct().toList();

        UserProfileListRequest userProfileListRequest = new UserProfileListRequest(writerIds);

        Map<String, SimpleUserProfileResponse> userProfileMap = EnvelopeUtils.getOrThrow(userProfileClient.getSimpleUserProfileList(userProfileListRequest), StriderErrorCodes.INTERNAL_SERVER_ERROR)
                                                                .stream()
                                                                .collect(Collectors.toMap(SimpleUserProfileResponse::userId, p -> p));

        Map<String, List<FeedMedia>> feedMediaMap = mediaRepository.findByFeedPostIdInAndIsDeletedFalseOrderBySequence(feedPostIds)
                .stream()
                .collect(Collectors.groupingBy(FeedMedia::getFeedPostId));

        Map<String, Integer> commentCountMap = commentRepository.countByFeedPostIds(feedPostIds)
                .stream()
                .collect(Collectors.toMap(
                        FeedCommentRepository.CommentCountResponse::getFeedPostId,
                        FeedCommentRepository.CommentCountResponse::getCommentCnt
                ));

        List<String> userLikePostIds = likeRepository.findByUserIdAndTargetIdInAndTargetType(userId, feedPostIds, TargetType.FEED);
        List<String> userSavePostIds = saveRepository.findByUserIdAndTargetIdIn(userId, feedPostIds);
        Set<String> likedSet = new HashSet<>(userLikePostIds);
        Set<String> savedSet = new HashSet<>(userSavePostIds);

        return posts.stream()
                .map(post ->
                {
                    SimpleUserProfileResponse profile = userProfileMap.get(post.getUserId());

                    List<FeedMedia> feedMediaList = feedMediaMap.getOrDefault(post.getFeedPostId(), List.of());
                    int commentCount = commentCountMap.getOrDefault(post.getFeedPostId(), 0);
                    boolean userLike = likedSet.contains(post.getFeedPostId());
                    boolean userSave = savedSet.contains(post.getFeedPostId());

                    return FeedPostResponse.builder()
                            .feedPostId(post.getFeedPostId())
                            .exerciseId(post.getExerciseId())
                            .review(post.getReview())
                            .userId(post.getUserId())
                            .userProfileImg(profile.profileThumbImage() != null ? profile.profileThumbImage() : profile.profileImage())
                            .userNickname(profile.nickname())
                            .likeCount(post.getLikeCount())
                            .saveCount(post.getSaveCount())
                            .commentCount(commentCount)
                            .createdAt(post.getCreatedAt())
                            .updatedAt(post.getUpdatedAt())
                            .mediaList(feedMediaList)
                            .userLike(userLike)
                            .userSave(userSave)
                            .build();
                }).toList();
    }

    // 알림 등 외부 서비스용 - 게시물 썸네일 배치 조회 (게시물당 첫 번째 미디어)
    public List<FeedThumbnailResponse> getThumbnailsByFeedPostIds(List<String> feedPostIds) {
        if (feedPostIds.isEmpty()) return List.of();

        Map<String, List<FeedMedia>> feedMediaMap = mediaRepository
                .findByFeedPostIdInAndIsDeletedFalseOrderBySequence(feedPostIds)
                .stream()
                .collect(Collectors.groupingBy(FeedMedia::getFeedPostId));

        return feedPostIds.stream()
                .map(feedPostId -> {
                    List<FeedMedia> mediaList = feedMediaMap.getOrDefault(feedPostId, List.of());
                    String thumbnailUrl = mediaList.isEmpty() ? null : firstThumbnailUrl(mediaList.get(0));

                    return FeedThumbnailResponse.builder()
                            .feedPostId(feedPostId)
                            .thumbnailUrl(thumbnailUrl)
                            .build();
                }).toList();
    }

    // 알림 등 외부 서비스용 - 댓글이 달린 게시물의 썸네일 배치 조회
    public List<CommentThumbnailResponse> getThumbnailsByCommentIds(List<String> commentIds) {
        if (commentIds.isEmpty()) return List.of();

        List<FeedComment> comments = commentRepository.findAllById(commentIds);
        Map<String, String> feedPostIdByCommentId = comments.stream()
                .collect(Collectors.toMap(FeedComment::getCommentId, FeedComment::getFeedPostId));

        List<String> feedPostIds = feedPostIdByCommentId.values().stream().distinct().toList();
        Map<String, String> thumbnailByFeedPostId = getThumbnailsByFeedPostIds(feedPostIds).stream()
                .collect(Collectors.toMap(FeedThumbnailResponse::feedPostId, FeedThumbnailResponse::thumbnailUrl));

        return commentIds.stream()
                .map(commentId -> {
                    String feedPostId = feedPostIdByCommentId.get(commentId);
                    String thumbnailUrl = feedPostId == null ? null : thumbnailByFeedPostId.get(feedPostId);

                    return CommentThumbnailResponse.builder()
                            .commentId(commentId)
                            .feedPostId(feedPostId)
                            .thumbnailUrl(thumbnailUrl)
                            .build();
                }).toList();
    }

    private String firstThumbnailUrl(FeedMedia media) {
        return media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getFileUrl();
    }

    public List<FeedMediaResponse> findFeedMedia() {
        List<FeedMedia> mediaList = mediaRepository.findAllByIsDeletedFalseAndSequence(0);

        return mediaList.stream()
                .map(media -> {
                    return FeedMediaResponse.builder()
                            .feedMediaId(media.getMediaId())
                            .feedPostId(media.getFeedPostId())
                            .fileUrl(media.getFileUrl())
                            .thumbnailUrl(media.getThumbnailUrl())
                            .mediaType(media.getMediaType())
                            .createdAt(media.getCreatedAt())
                            .build();
                }).toList();
    }

    public FeedPostResponse findFeed(String userId, String feedId) {
        // TODO: 추후 user가 follow한 사람들의 목록을 가져와서 팔로우한 사람들의 피드만 보여줘야함.
        // TODO: PUBLIC 등의 공개여부도 따져야 함.
        FeedPost post = postRepository.findByFeedPostIdAndIsDeletedFalse(feedId)
                .orElseThrow(() -> new StriderException(StriderErrorCodes.NOT_FOUND));

        UserProfileRequest userProfileRequest = new UserProfileRequest(post.getUserId());
        UserProfileResponse userProfile = EnvelopeUtils.getOrThrow(userProfileClient.getUserProfile(userProfileRequest), StriderErrorCodes.NOT_FOUND);

        int commentCount = commentRepository.countByFeedPostIdAndIsDeletedFalse(post.getFeedPostId());
        List<FeedMedia> feedMediaList = mediaRepository.findByFeedPostIdAndIsDeletedFalseOrderBySequence(post.getFeedPostId());

        boolean userLike = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, post.getFeedPostId(), TargetType.FEED).isPresent();
        boolean userSave = saveRepository.findByUserIdAndTargetId(userId, post.getFeedPostId()).isPresent();

        return FeedPostResponse.builder()
                .feedPostId(post.getFeedPostId())
                .exerciseId(post.getExerciseId())
                .review(post.getReview())
                .userId(post.getUserId())
                .userProfileImg(userProfile.profileImage())
                .userNickname(userProfile.nickname())
                .likeCount(post.getLikeCount())
                .saveCount(post.getSaveCount())
                .commentCount(commentCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .mediaList(feedMediaList)
                .userLike(userLike)
                .userSave(userSave)
                .build();
    }

    public List<SimpleUserFollowResponse> findFeedLikeUserProfileList(HttpHeaders headers, FeedLikeUserProfileQuery request) {
        // TODO: 나를 꼭 포함시킬건지 여부
        // 피드 좋아요를 누른 사용자id list 조회
        List<String> likeUserIds;
        if(isFirstSearch(request.cursorCreatedAt(), request.cursorId())) {
            likeUserIds = likeRepository.findLikeUserIdFirst(request.feedId(), PageRequest.of(0, request.size()));
        } else {
            likeUserIds = likeRepository.findLikeUserIdAfterCursor(request.feedId(), request.cursorCreatedAt(), request.cursorId(), PageRequest.of(0, request.size()));
        }

        if(likeUserIds.isEmpty()) return List.of();

        // 사용자들의 profile list 조회
        UserProfileListRequest userProfileListRequest = new UserProfileListRequest(likeUserIds);
        List<SimpleUserProfileResponse> userProfileList = EnvelopeUtils.getOrThrow(userProfileClient.getSimpleUserProfileList(userProfileListRequest), StriderErrorCodes.INTERNAL_SERVER_ERROR);
        // 사용자의 팔로잉 리스트
        List<UserFollowResponse> userFollowingList = EnvelopeUtils.getOrThrow(userProfileClient.getFollowingIdList(request.userId()), StriderErrorCodes.INTERNAL_SERVER_ERROR);
        // 사용자의 팔로우 리스트
        List<UserFollowResponse> userFollowedList = EnvelopeUtils.getOrThrow(userProfileClient.getFollowerIdList(request.userId()), StriderErrorCodes.INTERNAL_SERVER_ERROR);

        Map<String, SimpleUserProfileResponse> userProfileMap = userProfileList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        SimpleUserProfileResponse::userId,
                        p -> p,
                        (a, b) -> a
                ));

        Set<String> followingIdSet = userFollowingList.stream()
                .map(UserFollowResponse::userId)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> followerIdSet = userFollowedList.stream()
                .map(UserFollowResponse::userId)
                .collect(java.util.stream.Collectors.toSet());

        return likeUserIds.stream()
                .map(likeUserId -> {
                    SimpleUserProfileResponse p = userProfileMap.get(likeUserId);

                    // 프로필 누락 방어 (탈퇴/비공개/동기화 이슈 등)
                    if (p == null) {
                        return SimpleUserFollowResponse.builder()
                                .userId(likeUserId)
                                .profileId(null)
                                .nickname(null)
                                .profileImage(null)
                                .profileThumbImage(null)
                                .profileDesc(null)
                                .rankId(null)
                                .following(followingIdSet.contains(likeUserId))
                                .isFollowed(followerIdSet.contains(likeUserId))
                                .build();
                    }

                    return SimpleUserFollowResponse.builder()
                            .userId(p.userId())
                            .profileId(p.profileId())
                            .nickname(p.nickname())
                            .profileImage(p.profileImage())
                            .profileThumbImage(p.profileThumbImage())
                            .profileDesc(p.profileDesc())
                            .rankId(p.rankId())
                            .following(followingIdSet.contains(likeUserId))
                            .isFollowed(followerIdSet.contains(likeUserId))
                            .build();
                })
                .toList();
    }

    private Boolean isFirstSearch(LocalDateTime cursorCreatedAt, String cursorId){
        return cursorCreatedAt == null || cursorId == null;
    }

    /**
     * S3 Key를 받아 CloudFront URL을 생성
     */
    public String generateCloudFrontUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) return null;
        return "https://" + cloudFrontDomain + "/" + s3Key;
    }
}

package com.strider.feed.service;

import com.strider.feed.client.UserProfileClient;
import com.strider.feed.model.TargetType;
import com.strider.feed.model.entity.FeedMedia;
import com.strider.feed.model.entity.FeedSave;
import com.strider.feed.model.request.UserProfileRequest;
import com.strider.feed.model.response.*;
import com.strider.feed.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedSaveService {
    private final FeedPostRepository postRepository;
    private final FeedLikeRepository likeRepository;
    private final FeedSaveRepository saveRepository;
    private final FeedCommentRepository commentRepository;
    private final FeedMediaRepository mediaRepository;
    private final UserProfileClient userProfileClient;

    public List<FeedPostResponse> getSavedFeeds(String userId) {
        List<FeedSave> savedList = saveRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // targetId 기준으로 feedPost 목록 조회
        List<String> feedIds = savedList.stream()
                .map(FeedSave::getTargetId)
                .toList();

        return postRepository.findAllById(feedIds)
                .stream()
                .map(feed -> {
                    var mediaList = mediaRepository.findByFeedPostIdOrderBySequenceAsc(feed.getFeedPostId());
                    UserProfileRequest userProfileRequest = new UserProfileRequest(feed.getUserId());
                    Envelope<UserProfileResponse> userProfileEnvelope = userProfileClient.getUserProfile(userProfileRequest);

                    int commentCount = commentRepository.countByFeedPostIdAndIsDeletedFalse(feed.getFeedPostId());
                    boolean userLike = likeRepository.findByUserIdAndTargetIdAndTargetType(userId, feed.getFeedPostId(), TargetType.FEED).isPresent();

                    UserProfileResponse userProfile = null;
                    if(userProfileEnvelope.data() != null){
                        userProfile = userProfileEnvelope.data();
                    }

                    return FeedPostResponse.builder()
                            .feedPostId(feed.getFeedPostId())
                            .exerciseId(feed.getExerciseId())
                            .review(feed.getReview())
                            .userId(feed.getUserId())
                            .userProfileImg(userProfile != null ? userProfile.profileImage() : null)
                            .userNickname(userProfile != null ? userProfile.nickname() : "Unknown")
                            .likeCount(feed.getLikeCount())
                            .saveCount(feed.getSaveCount())
                            .commentCount(commentCount)
                            .createdAt(feed.getCreatedAt())
                            .updatedAt(feed.getUpdatedAt())
                            .mediaList(mediaList)
                            .userLike(userLike)
                            .userSave(true) // 저장 리스트 조회 중이라 항상 true
                            .build();
                })
                .toList();
    }

    public List<SavedFeedThumbnailResponse> getSavedFeedThumbnails(String userId) {
        return saveRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(save -> {
                    String feedPostId = save.getTargetId();
                    String thumbnail = mediaRepository.findFirstByFeedPostIdOrderBySequenceAsc(feedPostId)
                            .map(FeedMedia::getThumbnailUrl)
                            .orElse(null);
                    return SavedFeedThumbnailResponse.builder()
                            .feedPostId(feedPostId)
                            .thumbnailUrl(thumbnail)
                            .savedAt(save.getCreatedAt())
                            .build();
                })
                .toList();
    }

}
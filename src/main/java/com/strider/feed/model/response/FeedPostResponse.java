package com.strider.feed.model.response;

import com.strider.feed.model.entity.FeedMedia;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FeedPostResponse(
        String feedPostId,
        String exerciseId,
        String review,
        String userId,
        String userProfileImg,
        String userNickname,
        Integer likeCount,
        Integer saveCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<FeedMedia> mediaList,
        Boolean userLike,
        Boolean userSave
){ }

package com.strider.feed.model.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FeedCommentResponse (
    String commentId,
    String feedPostId,
    String userId,
    String parentCommentId,
    String userProfileImg,
    String userNickname,
    String commentText,
    Integer likeCount,
    Boolean userLike,
    Integer replyCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

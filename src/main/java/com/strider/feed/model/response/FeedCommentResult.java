package com.strider.feed.model.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FeedCommentResult(
    String commentId,
    String feedPostId,
    String userId,
    String parentCommentId,
    String commentText,
    Integer likeCount,
    Boolean userLike,
    Integer replyCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) { }

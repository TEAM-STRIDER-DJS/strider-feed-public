package com.strider.feed.model.request;


public record FeedCommentRequest(
    String feedPostId,
    String parentCommentId,
    String commentText
) {}

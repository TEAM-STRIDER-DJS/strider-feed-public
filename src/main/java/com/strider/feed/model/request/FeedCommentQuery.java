package com.strider.feed.model.request;

import java.time.LocalDateTime;

public record FeedCommentQuery(
    String userId,
    String feedId,
    int size,
    LocalDateTime cursorCreatedAt,
    String cursorId
) { }

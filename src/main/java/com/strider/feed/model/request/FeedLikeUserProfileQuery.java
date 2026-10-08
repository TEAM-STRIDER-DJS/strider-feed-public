package com.strider.feed.model.request;

import java.time.LocalDateTime;

public record FeedLikeUserProfileQuery(
    String feedId,
    String userId,
    int size,
    LocalDateTime cursorCreatedAt,
    String cursorId
) { }

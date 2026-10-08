package com.strider.feed.model.request;

import java.time.LocalDateTime;

public record UserFeedQuery(
    String userId,
    String feedUserId,
    int size,
    LocalDateTime cursorCreatedAt,
    String cursorId
) { }

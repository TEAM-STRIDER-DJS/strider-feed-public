package com.strider.feed.model.request;

import java.time.LocalDateTime;

public record CommentReplyQuery(
    String userId,
    String parentCommentId,
    int size,
    LocalDateTime cursorCreatedAt,
    String cursorId
) { }

package com.strider.feed.model.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SavedFeedThumbnailResponse(
        String feedPostId,
        String thumbnailUrl,
        LocalDateTime savedAt
) {}

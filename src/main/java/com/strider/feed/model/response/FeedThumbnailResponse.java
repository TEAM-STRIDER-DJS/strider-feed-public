package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record FeedThumbnailResponse(
        String feedPostId,
        String thumbnailUrl
) {}

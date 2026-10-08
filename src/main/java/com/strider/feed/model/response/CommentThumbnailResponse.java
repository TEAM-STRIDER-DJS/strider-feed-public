package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record CommentThumbnailResponse(
        String commentId,
        String feedPostId,
        String thumbnailUrl
) {}

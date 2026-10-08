package com.strider.feed.model.response;

import com.strider.feed.model.MediaType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FeedMediaResponse(
        String feedMediaId,
        String feedPostId,
        String fileUrl,
        String thumbnailUrl,
        MediaType mediaType,
        LocalDateTime createdAt
) { }

package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record FeedShareUrlResponse (
        String feedId,
        String url
) {}

package com.strider.feed.model.request;

import java.util.List;

public record FeedThumbnailBatchRequest(
        List<String> feedPostIds
) {}

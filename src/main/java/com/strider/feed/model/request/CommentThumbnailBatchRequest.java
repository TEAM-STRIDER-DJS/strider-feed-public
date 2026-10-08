package com.strider.feed.model.request;

import java.util.List;

public record CommentThumbnailBatchRequest(
        List<String> commentIds
) {}

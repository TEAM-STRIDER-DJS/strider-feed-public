package com.strider.feed.model.request;

import com.strider.feed.model.MediaType;

import java.util.List;

public record FeedPostRequest(
    String exerciseId,
    String review,
    List<FeedMediaItem> mediaList
) {
    public record FeedMediaItem(
       Integer sequence,
       MediaType mediaType,
       String fileUrl,
       String thumbnailUrl
    ){}
}

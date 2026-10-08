package com.strider.feed.model.request;

import lombok.Builder;

@Builder
public record UserProfileRequest(
    String userId
) { }

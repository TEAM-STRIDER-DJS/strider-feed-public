package com.strider.feed.model.request;

import lombok.Builder;
import java.util.List;

@Builder
public record UserProfileListRequest(
    List<String> userIds
) {}
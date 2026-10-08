package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record SimpleUserFollowResponse(
        String userId,
        String profileId,
        String nickname,
        String profileImage,
        String profileThumbImage,
        String profileDesc,
        String rankId,
        boolean following,
        boolean isFollowed
) { }

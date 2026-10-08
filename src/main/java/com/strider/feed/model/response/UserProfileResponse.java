package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record UserProfileResponse(
        String userId,
        String profileId,
        String nickname,
        String profileImage,
        String profileThumbImage,
        String profileDesc,
        String rankId,
        int postCount,
        int followingCount,
        int followerCount
) {}

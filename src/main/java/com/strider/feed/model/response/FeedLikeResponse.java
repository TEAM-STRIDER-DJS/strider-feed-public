package com.strider.feed.model.response;

import com.strider.feed.model.TargetType;
import lombok.Builder;

@Builder
public record FeedLikeResponse(
//        String likeId,
//        String userId,
        TargetType targetType,
        String targetId,
//        LocalDateTime createdAt,
        Boolean liked
        //  TODO: 나중에 필요한 경우(이미 좋아요가 눌려있거나 없을 때) changed를 넣어야 할 수도
) {}

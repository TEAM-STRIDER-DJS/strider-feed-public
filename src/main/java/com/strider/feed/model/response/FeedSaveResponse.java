package com.strider.feed.model.response;

import lombok.Builder;

@Builder
public record FeedSaveResponse(
        String targetId,
        Boolean saved
        //  TODO: 나중에 필요한 경우(이미 좋아요가 눌려있거나 없을 때) changed를 넣어야 할 수도
){}

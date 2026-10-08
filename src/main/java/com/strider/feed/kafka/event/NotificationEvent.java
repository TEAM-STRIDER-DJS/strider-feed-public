package com.strider.feed.kafka.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * strider-notification 으로 발행하는 알림 이벤트.
 *
 * 필드명/형식은 strider-notification 의 NotificationEvent 와 정확히 일치해야 한다
 * (consumer 가 타입헤더를 무시하고 JSON 필드명으로 역직렬화함).
 *
 * eventType 은 strider-notification 의 NotificationType enum 이름과 일치해야 한다.
 *   사용 값: LIKE, COMMENT, COMMENT_LIKE
 *   ※ COMMENT_LIKE 는 notification 서비스 enum 에 추가가 필요함(팀원 협의).
 */
public record NotificationEvent(
        String eventType,
        String receiverUserId,
        String actorUserId,
        String resourceId,
        String content,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_COMMENT_LIKE = "COMMENT_LIKE";
    public static final String TYPE_POST = "POST"; // 신규 게시물 → 팔로워에게
}

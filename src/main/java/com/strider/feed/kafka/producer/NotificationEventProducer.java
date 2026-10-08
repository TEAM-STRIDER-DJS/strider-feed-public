package com.strider.feed.kafka.producer;

import com.strider.feed.kafka.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 이벤트를 Kafka "notification" 토픽으로 발행한다.
 * strider-notification 이 소비하여 DB 저장 + FCM 전송.
 *
 * 발행 실패가 사용자 요청(좋아요/댓글 등)을 막지 않도록 예외는 삼키고 로그만 남긴다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventProducer {

    private static final String TOPIC = "notification";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(NotificationEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.receiverUserId(), event);
        } catch (Exception e) {
            log.error("notification event publish fail event={}", event, e);
        }
    }
}

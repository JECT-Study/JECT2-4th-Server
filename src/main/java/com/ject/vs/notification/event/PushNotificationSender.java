package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.PushToken;
import com.ject.vs.notification.domain.PushTokenRepository;
import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushNotificationSender {

    private final NotificationRepository notificationRepository;
    private final PushTokenRepository pushTokenRepository;
    private final PushSenderPort pushSender;
    private final Clock clock;

    // 알림 row를 만든 트랜잭션이 커밋된 뒤에 발송한다.
    // 평범한 @EventListener + @Async 조합은 발행 트랜잭션 커밋 전에 별도 스레드/트랜잭션에서
    // findAllById가 실행돼 빈 결과를 받고 발송이 통째로 누락되는 race가 있었다.
    // - @Async는 함께 쓸 수 없다(@TransactionalEventListener와 동시 사용 시 Spring이 거부).
    //   발행 스레드(투표 종료는 notif- 비동기 풀 스레드)에서 커밋 직후 동기로 실행되므로 문제없다.
    // - 커밋 이후 단계라 기존 트랜잭션이 없으므로 REQUIRES_NEW로 markSent/토큰 삭제를 새 트랜잭션에 커밋한다.
    // - fallbackExecution=true: 혹시 트랜잭션 밖에서 발행돼도 발송되도록 한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(NotificationCreatedEvent event) {
        List<Long> notificationIds = event.notificationIds();
        if (notificationIds.isEmpty()) return;

        // 1. 알림 조회
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        if (notifications.isEmpty()) return;

        // 2. userId 목록 추출
        List<Long> userIds = notifications.stream()
                .map(Notification::getUserId)
                .distinct()
                .toList();

        // 3. 토큰 보유 회원별 토큰 목록 조회
        Map<Long, List<String>> tokensByUserId = pushTokenRepository
                .findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.groupingBy(
                        PushToken::getUserId,
                        Collectors.mapping(PushToken::getToken, Collectors.toList())));

        if (tokensByUserId.isEmpty()) {
            // 토큰이 없어도 sent 처리
            markAllAsSent(notifications);
            return;
        }

        // 4. 회원별 FCM 발송
        List<String> allExpiredTokens = new ArrayList<>();

        for (Notification notification : notifications) {
            List<String> tokens = tokensByUserId.get(notification.getUserId());
            if (tokens == null || tokens.isEmpty()) {
                notification.markSent(clock);
                continue;
            }

            FcmPayload payload = new FcmPayload(
                    notification.getType(),
                    notification.getTitle(),
                    notification.getBody(),
                    notification.getId(),
                    notification.getVoteId(),
                    notification.getThumbnailUrl(),
                    notification.getCreatedAt());

            List<String> expired = pushSender.sendMulticast(tokens, payload);
            allExpiredTokens.addAll(expired);

            notification.markSent(clock);
        }

        // 5. 만료된 토큰 일괄 삭제
        if (!allExpiredTokens.isEmpty()) {
            pushTokenRepository.deleteAllByTokenIn(allExpiredTokens);
            log.info("Cleaned up {} expired FCM tokens", allExpiredTokens.size());
        }

        log.info("Sent {} push notifications", notifications.size());
    }

    private void markAllAsSent(List<Notification> notifications) {
        for (Notification notification : notifications) {
            notification.markSent(clock);
        }
    }
}

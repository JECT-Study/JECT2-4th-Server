package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.domain.PushToken;
import com.ject.vs.notification.domain.PushTokenRepository;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationCommandUseCase.NotificationCreateCommand;
import com.ject.vs.notification.port.out.FcmPayload;
import com.ject.vs.notification.port.out.PushSenderPort;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.domain.VoteRepository;
import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEndedNotificationHandler {

    private final VoteRepository voteRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final NotificationCommandUseCase notificationCommandUseCase;
    private final PushTokenRepository pushTokenRepository;
    private final PushSenderPort pushSender;
    private final Clock clock;

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void on(VoteEndedEvent event) {
        // 1. Vote 정보 조회
        Vote vote = voteRepository.findById(event.voteId()).orElse(null);
        if (vote == null) {
            log.warn("Vote not found for VoteEndedEvent voteId={}", event.voteId());
            return;
        }

        // 2. 참여 회원(userId != null) 목록 조회
        List<Long> participantUserIds = voteParticipationRepository
                .findAllUserIdsByVoteId(event.voteId());
        if (participantUserIds.isEmpty()) return;

        // 3. Notification batch insert — notificationId가 포함된 결과 반환
        List<NotificationCreateCommand> commands = participantUserIds.stream()
                .map(uid -> new NotificationCreateCommand(
                        uid, NotificationType.VOTE_ENDED,
                        vote.getId(), vote.getTitle(),
                        "투표 결과가 공개됐어요", vote.getThumbnailUrl()))
                .toList();
        List<Notification> created = notificationCommandUseCase.createBatch(commands);

        // 4. userId → notificationId 매핑
        Map<Long, Long> userNotifIdMap = created.stream()
                .collect(Collectors.toMap(Notification::getUserId, Notification::getId));

        // 5. 토큰 보유 회원별 토큰 목록 (토큰이 있으면 push ON 상태)
        Map<Long, List<String>> tokensByUserId = pushTokenRepository
                .findAllByUserIdIn(participantUserIds)
                .stream()
                .collect(Collectors.groupingBy(
                        PushToken::getUserId,
                        Collectors.mapping(PushToken::getToken, Collectors.toList())));

        if (tokensByUserId.isEmpty()) return;

        // 6. 회원별 FCM 발송 (notificationId가 회원마다 다르므로 개별 멀티캐스트)
        List<String> allExpiredTokens = new ArrayList<>();
        Instant now = Instant.now(clock);

        for (Map.Entry<Long, List<String>> entry : tokensByUserId.entrySet()) {
            Long userId = entry.getKey();
            List<String> tokens = entry.getValue();
            Long notifId = userNotifIdMap.get(userId);
            if (notifId == null) continue;

            FcmPayload payload = new FcmPayload(
                    NotificationType.VOTE_ENDED,
                    "투표 결과가 공개됐어요",
                    vote.getTitle(),
                    notifId,
                    vote.getId(),
                    vote.getThumbnailUrl(),
                    now);

            List<String> expired = pushSender.sendMulticast(tokens, payload);
            allExpiredTokens.addAll(expired);
        }

        // 7. 만료된 토큰 일괄 삭제
        if (!allExpiredTokens.isEmpty()) {
            pushTokenRepository.deleteAllByTokenIn(allExpiredTokens);
            log.info("Cleaned up {} expired FCM tokens", allExpiredTokens.size());
        }
    }
}

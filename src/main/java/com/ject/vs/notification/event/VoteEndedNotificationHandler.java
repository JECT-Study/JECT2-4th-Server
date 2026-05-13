package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.NotificationSetting;
import com.ject.vs.notification.domain.NotificationSettingRepository;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.domain.PushSubscription;
import com.ject.vs.notification.domain.PushSubscriptionRepository;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationCommandUseCase.NotificationCreateCommand;
import com.ject.vs.notification.port.out.PushPayload;
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

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEndedNotificationHandler {

    private final VoteRepository voteRepository;
    private final VoteParticipationRepository voteParticipationRepository;
    private final NotificationCommandUseCase notificationCommandUseCase;
    private final NotificationSettingRepository settingRepository;
    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final PushSenderPort pushSender;

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

        // 3. Notification batch insert
        List<NotificationCreateCommand> commands = participantUserIds.stream()
                .map(uid -> new NotificationCreateCommand(
                        uid, NotificationType.VOTE_RESULT_PUBLISHED,
                        vote.getId(), vote.getTitle(),
                        "투표 결과가 공개됐어요", vote.getThumbnailUrl()))
                .toList();
        notificationCommandUseCase.createBatch(commands);

        // 4. push_enabled=true 인 사용자 필터
        List<Long> enabledUserIds = settingRepository.findAllById(participantUserIds).stream()
                .filter(NotificationSetting::isPushEnabled)
                .map(NotificationSetting::getUserId)
                .toList();
        if (enabledUserIds.isEmpty()) return;

        // 5. PushSubscription 일괄 조회 + 발송
        List<PushSubscription> subscriptions =
                pushSubscriptionRepository.findAllByUserIdIn(enabledUserIds);

        PushPayload payload = new PushPayload(
                NotificationType.VOTE_RESULT_PUBLISHED,
                "투표 결과가 공개됐어요",
                vote.getTitle(),
                vote.getId(),
                "/votes/" + vote.getId() + "/result"
        );

        List<PushSubscription> goneSubs = new ArrayList<>();
        for (PushSubscription sub : subscriptions) {
            PushSenderPort.SendResult result = pushSender.send(sub, payload);
            if (result == PushSenderPort.SendResult.GONE) {
                goneSubs.add(sub);
            }
        }

        // 6. 만료된 구독 정리
        if (!goneSubs.isEmpty()) {
            pushSubscriptionRepository.deleteAll(goneSubs);
            log.info("Cleaned up {} stale push subscriptions", goneSubs.size());
        }
    }
}

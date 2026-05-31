package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import com.ject.vs.notification.port.in.NotificationCommandUseCase.NotificationCreateCommand;
import com.ject.vs.notification.port.out.VoteQueryPort;
import com.ject.vs.vote.domain.Vote;
import com.ject.vs.vote.domain.VoteParticipationRepository;
import com.ject.vs.vote.event.VoteEndedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEndedNotificationHandler {

    private final VoteQueryPort voteQueryPort;
    private final VoteParticipationRepository voteParticipationRepository;
    private final NotificationCommandUseCase notificationCommandUseCase;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void on(VoteEndedEvent event) {
        // 1. Vote 정보 조회
        Vote vote = voteQueryPort.getById(event.voteId());

        // 2. 참여 회원(userId != null) 목록 조회
        List<Long> participantUserIds = voteParticipationRepository
                .findAllUserIdsByVoteId(event.voteId());
        if (participantUserIds.isEmpty()) return;

        // 3. 이미 알림을 받은 사용자 제외 (중복 알림 방지)
        Set<Long> alreadyNotifiedUserIds = new HashSet<>(
                notificationRepository.findUserIdsByVoteIdAndType(event.voteId(), NotificationType.VOTE_ENDED)
        );
        List<Long> targetUserIds = participantUserIds.stream()
                .filter(uid -> !alreadyNotifiedUserIds.contains(uid))
                .toList();
        if (targetUserIds.isEmpty()) {
            log.debug("All participants already notified for voteId={}", event.voteId());
            return;
        }

        // 4. Notification batch insert
        List<NotificationCreateCommand> commands = targetUserIds.stream()
                .map(uid -> new NotificationCreateCommand(
                        uid, NotificationType.VOTE_ENDED,
                        vote.getId(), vote.getTitle(),
                        "투표 결과가 공개됐어요", vote.getThumbnailUrl()))
                .toList();
        List<Notification> created = notificationCommandUseCase.createBatch(commands);

        // 5. 푸시 발송을 위한 이벤트 발행
        List<Long> notificationIds = created.stream()
                .map(Notification::getId)
                .toList();
        eventPublisher.publishEvent(new NotificationCreatedEvent(notificationIds));

        log.info("Created {} notifications for voteId={}", created.size(), event.voteId());
    }
}

package com.ject.vs.notification.event;

import com.ject.vs.notification.domain.Notification;
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

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEndedNotificationHandler {

    private final VoteQueryPort voteQueryPort;
    private final VoteParticipationRepository voteParticipationRepository;
    private final NotificationCommandUseCase notificationCommandUseCase;
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

        // 3. Notification batch insert
        List<NotificationCreateCommand> commands = participantUserIds.stream()
                .map(uid -> new NotificationCreateCommand(
                        uid, NotificationType.VOTE_ENDED,
                        vote.getId(), vote.getTitle(),
                        "투표 결과가 공개됐어요", vote.getThumbnailUrl()))
                .toList();
        List<Notification> created = notificationCommandUseCase.createBatch(commands);

        // 4. 푸시 발송을 위한 이벤트 발행
        List<Long> notificationIds = created.stream()
                .map(Notification::getId)
                .toList();
        eventPublisher.publishEvent(new NotificationCreatedEvent(notificationIds));

        log.info("Created {} notifications for voteId={}", created.size(), event.voteId());
    }
}

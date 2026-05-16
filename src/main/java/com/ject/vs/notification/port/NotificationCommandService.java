package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.exception.NotificationAccessDeniedException;
import com.ject.vs.notification.exception.NotificationNotFoundException;
import com.ject.vs.notification.port.in.NotificationCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService implements NotificationCommandUseCase {

    private final NotificationRepository notificationRepository;
    private final Clock clock;

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(NotificationNotFoundException::new);
        if (!n.isOwnedBy(userId)) throw new NotificationAccessDeniedException();
        n.markRead(clock);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId, Instant.now(clock));
    }

    @Override
    public List<Notification> createBatch(List<NotificationCreateCommand> commands) {
        if (commands.isEmpty()) return List.of();
        List<Notification> notifications = commands.stream()
                .map(c -> {
                    if (c.type() == NotificationType.VOTE_RESULT_PUBLISHED) {
                        return Notification.ofVoteResultPublished(
                                c.userId(), c.voteId(), c.title(), c.thumbnailUrl(), clock);
                    }
                    throw new IllegalArgumentException("Unsupported type: " + c.type());
                }).toList();
        return notificationRepository.saveAll(notifications);
    }
}

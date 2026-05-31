package com.ject.vs.notification.port;

import com.ject.vs.config.AdminProperties;
import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationRepository;
import com.ject.vs.notification.event.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPushService {

    private final AdminProperties adminProperties;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public Long sendTestPush(Long adminUserId, Long targetUserId, String title, String body,
                             Long voteId, String thumbnailUrl) {
        validateAdmin(adminUserId);

        Notification notification = Notification.ofCustom(
                targetUserId, voteId, title, body, thumbnailUrl, clock);

        Notification saved = notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationCreatedEvent(List.of(saved.getId())));

        log.info("Admin {} sent test push to user {}: {}", adminUserId, targetUserId, title);

        return saved.getId();
    }

    private void validateAdmin(Long adminUserId) {
        if (adminProperties.userIds() == null || !adminProperties.userIds().contains(adminUserId)) {
            throw new IllegalArgumentException("관리자 권한이 없습니다.");
        }
    }
}

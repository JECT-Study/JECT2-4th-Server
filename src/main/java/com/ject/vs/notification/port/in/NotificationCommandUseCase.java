package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationType;

import java.util.List;

public interface NotificationCommandUseCase {
    void markAsRead(Long notificationId, Long userId);
    int markAllAsRead(Long userId);

    // 이벤트 핸들러용 — 생성된 Notification 목록 반환 (FCM 발송 시 notificationId 필요)
    List<Notification> createBatch(List<NotificationCreateCommand> commands);

    record NotificationCreateCommand(
            Long userId, NotificationType type, Long voteId,
            String title, String body, String thumbnailUrl) {
    }
}

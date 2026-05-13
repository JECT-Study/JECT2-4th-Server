package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.NotificationType;

import java.util.List;

public interface NotificationCommandUseCase {
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);

    // 이벤트 핸들러용 — 외부에서 직접 호출하지 않음
    void createBatch(List<NotificationCreateCommand> commands);

    record NotificationCreateCommand(
            Long userId, NotificationType type, Long voteId,
            String title, String body, String thumbnailUrl) {
    }
}

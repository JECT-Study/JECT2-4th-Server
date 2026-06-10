package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationType;

import java.util.List;

public interface NotificationCommandUseCase {
    /**
     * 알림을 읽음 처리하고, 행동 로그(notification_opened)에 필요한 정보를 반환한다.
     *
     * @return 읽음 처리한 알림의 메타데이터(읽음 처리 직전 상태 기준의 isRead 포함)
     */
    MarkAsReadResult markAsRead(Long notificationId, Long userId);
    int markAllAsRead(Long userId);

    record MarkAsReadResult(NotificationType type, Long voteId, boolean wasRead) {
    }

    // 이벤트 핸들러용 — 생성된 Notification 목록 반환 (FCM 발송 시 notificationId 필요)
    List<Notification> createBatch(List<NotificationCreateCommand> commands);

    record NotificationCreateCommand(
            Long userId, NotificationType type, Long voteId,
            String title, String body, String thumbnailUrl) {
    }
}

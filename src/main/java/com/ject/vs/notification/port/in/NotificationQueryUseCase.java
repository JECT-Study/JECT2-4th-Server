package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.Notification;
import com.ject.vs.notification.domain.NotificationType;

import java.time.Instant;
import java.util.List;

public interface NotificationQueryUseCase {
    NotificationPageResult getList(Long userId, Long cursor, int size);
    long getUnreadCount(Long userId);

    record NotificationPageResult(
            List<NotificationView> notifications, Long nextCursor, boolean hasNext) {
    }

    record NotificationView(
            Long notificationId, NotificationType type, Long voteId,
            String title, String body, String thumbnailUrl,
            boolean isRead, Instant createdAt) {

        public static NotificationView from(Notification n) {
            return new NotificationView(
                    n.getId(), n.getType(), n.getVoteId(),
                    n.getTitle(), n.getBody(), n.getThumbnailUrl(),
                    n.isRead(), n.getCreatedAt());
        }
    }
}

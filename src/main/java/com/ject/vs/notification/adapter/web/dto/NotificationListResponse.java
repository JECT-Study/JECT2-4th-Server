package com.ject.vs.notification.adapter.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ject.vs.notification.domain.NotificationType;
import com.ject.vs.notification.port.in.NotificationQueryUseCase.NotificationPageResult;
import com.ject.vs.notification.port.in.NotificationQueryUseCase.NotificationView;

import java.time.Instant;
import java.util.List;

public record NotificationListResponse(
        List<NotificationItem> notifications, Long nextCursor, boolean hasNext) {

    public static NotificationListResponse from(NotificationPageResult r) {
        return new NotificationListResponse(
                r.notifications().stream().map(NotificationItem::from).toList(),
                r.nextCursor(), r.hasNext());
    }

    public record NotificationItem(
            Long notificationId, NotificationType type, Long voteId,
            String title, String body, String thumbnailUrl, boolean isRead,
            @JsonSerialize(using = com.ject.vs.notification.adapter.web.KstInstantSerializer.class)
            Instant createdAt) {

        public static NotificationItem from(NotificationView v) {
            return new NotificationItem(
                    v.notificationId(), v.type(), v.voteId(),
                    v.title(), v.body(), v.thumbnailUrl(), v.isRead(), v.createdAt());
        }
    }
}

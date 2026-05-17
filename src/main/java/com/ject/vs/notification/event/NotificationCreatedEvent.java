package com.ject.vs.notification.event;

import java.util.List;

public record NotificationCreatedEvent(List<Long> notificationIds) {
}

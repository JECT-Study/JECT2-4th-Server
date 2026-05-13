package com.ject.vs.notification.adapter.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(@NotNull Boolean pushEnabled) {
}

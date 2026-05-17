package com.ject.vs.notification.adapter.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ject.vs.notification.adapter.web.KstInstantSerializer;
import com.ject.vs.notification.port.in.NotificationSettingUseCase.NotificationSettingView;

import java.time.Instant;

public record NotificationSettingResponse(
        boolean pushEnabled,
        @JsonSerialize(using = KstInstantSerializer.class) Instant pushEnabledAt,
        @JsonSerialize(using = KstInstantSerializer.class) Instant pushDisabledAt) {

    public static NotificationSettingResponse from(NotificationSettingView v) {
        return new NotificationSettingResponse(
                v.pushEnabled(), v.pushEnabledAt(), v.pushDisabledAt());
    }
}

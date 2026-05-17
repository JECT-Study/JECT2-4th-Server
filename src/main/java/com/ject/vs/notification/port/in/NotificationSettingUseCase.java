package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.NotificationSetting;

import java.time.Instant;

public interface NotificationSettingUseCase {
    NotificationSettingView get(Long userId);
    NotificationSettingView updatePushEnabled(Long userId, boolean pushEnabled);

    record NotificationSettingView(
            boolean pushEnabled, Instant pushEnabledAt, Instant pushDisabledAt) {

        public static NotificationSettingView from(NotificationSetting s) {
            return new NotificationSettingView(
                    s.isPushEnabled(), s.getPushEnabledAt(), s.getPushDisabledAt());
        }
    }
}

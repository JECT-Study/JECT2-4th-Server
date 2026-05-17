package com.ject.vs.notification.port;

import com.ject.vs.notification.domain.NotificationSetting;
import com.ject.vs.notification.domain.NotificationSettingRepository;
import com.ject.vs.notification.port.in.NotificationSettingUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationSettingService implements NotificationSettingUseCase {

    private final NotificationSettingRepository settingRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public NotificationSettingView get(Long userId) {
        NotificationSetting s = settingRepository.findById(userId)
                .orElseGet(() -> settingRepository.save(NotificationSetting.createDefault(userId)));
        return NotificationSettingView.from(s);
    }

    @Override
    public NotificationSettingView updatePushEnabled(Long userId, boolean pushEnabled) {
        NotificationSetting s = settingRepository.findById(userId)
                .orElseGet(() -> NotificationSetting.createDefault(userId));
        if (pushEnabled) s.enablePush(clock); else s.disablePush(clock);
        settingRepository.save(s);
        return NotificationSettingView.from(s);
    }
}

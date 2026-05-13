package com.ject.vs.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    // PK = userId 이므로 findById(userId) 로 조회
}

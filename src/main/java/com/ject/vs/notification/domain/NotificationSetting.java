package com.ject.vs.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "notification_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NotificationSetting {

    @Id
    private Long userId;

    private boolean pushEnabled;
    private Instant pushEnabledAt;
    private Instant pushDisabledAt;
    private int promptDismissCount;
    private Instant lastPromptShownAt;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public static NotificationSetting createDefault(Long userId) {
        NotificationSetting s = new NotificationSetting();
        s.userId = userId;
        s.pushEnabled = false;
        s.promptDismissCount = 0;
        return s;
    }

    public void enablePush(Clock clock) {
        this.pushEnabled = true;
        this.pushEnabledAt = Instant.now(clock);
    }

    public void disablePush(Clock clock) {
        this.pushEnabled = false;
        this.pushDisabledAt = Instant.now(clock);
    }

    public void recordPromptDismissed(Clock clock) {
        this.promptDismissCount += 1;
        this.lastPromptShownAt = Instant.now(clock);
    }
}

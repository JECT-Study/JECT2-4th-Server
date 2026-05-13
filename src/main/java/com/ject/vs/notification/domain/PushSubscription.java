package com.ject.vs.notification.domain;

import com.ject.vs.common.domain.BaseEntity;
import com.ject.vs.notification.exception.PushSubscriptionInvalidException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "push_subscription")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushSubscription extends BaseEntity {

    private Long userId;
    private String endpoint;
    private String p256dhKey;
    private String authKey;
    private String userAgent;

    private Instant createdAt;
    private Instant lastUsedAt;

    public static PushSubscription of(Long userId, String endpoint,
                                      String p256dhKey, String authKey,
                                      String userAgent, Clock clock) {
        if (endpoint == null || endpoint.isBlank()
                || p256dhKey == null || p256dhKey.isBlank()
                || authKey == null || authKey.isBlank()) {
            throw new PushSubscriptionInvalidException();
        }
        PushSubscription p = new PushSubscription();
        p.userId = userId;
        p.endpoint = endpoint;
        p.p256dhKey = p256dhKey;
        p.authKey = authKey;
        p.userAgent = userAgent;
        p.createdAt = Instant.now(clock);
        return p;
    }

    public void touch(Clock clock) {
        this.lastUsedAt = Instant.now(clock);
    }
}

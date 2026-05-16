package com.ject.vs.notification.domain;

import com.ject.vs.common.domain.BaseEntity;
import com.ject.vs.notification.exception.PushTokenInvalidException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "push_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PushToken extends BaseEntity {

    private Long userId;
    private String token;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    private Instant createdAt;
    private Instant updatedAt;

    public static PushToken of(Long userId, String token, Platform platform, Clock clock) {
        if (token == null || token.isBlank()) {
            throw new PushTokenInvalidException();
        }
        PushToken t = new PushToken();
        t.userId = userId;
        t.token = token;
        t.platform = platform;
        t.createdAt = Instant.now(clock);
        t.updatedAt = t.createdAt;
        return t;
    }

    public void touch(Clock clock) {
        this.updatedAt = Instant.now(clock);
    }
}

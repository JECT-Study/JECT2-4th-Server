package com.ject.vs.notification.port.in;

import com.ject.vs.notification.domain.Platform;

public interface PushTokenUseCase {
    void register(Long userId, String token, Platform platform);
    void unregisterAll(Long userId);
}
